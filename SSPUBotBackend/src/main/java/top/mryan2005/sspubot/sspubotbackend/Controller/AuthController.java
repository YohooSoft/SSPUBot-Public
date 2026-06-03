package top.mryan2005.sspubot.sspubotbackend.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Dto.UserDto;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForCreate;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForLogin;
import top.mryan2005.sspubot.sspubotbackend.Service.UserService;
import top.mryan2005.sspubot.sspubotbackend.Utils.JwtUtils;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"}, allowCredentials = "true")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtUtils jwtUtils;

    @Autowired
    UserService userService;

    @GetMapping("/status")
    public ResponseEntity<String> status() {
        return ResponseEntity.ok("Auth service is running. Use POST /auth/login to authenticate.");
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody UserDtoForLogin request) {
        try {
            log.info("Login attempt for user: {}", request.getUsername());
            
            // Find user (this validates user exists)
            UserDto thisUser = userService.findThisUser(request);
            log.info("User found: {}", thisUser.getUsername());
            
            // Authenticate using the raw password from request, not the encrypted one
            // AuthenticationManager will use BCrypt to compare with the stored password
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(thisUser.getUsername(), request.getPassword())
            );
            log.info("Authentication successful for user: {}", thisUser.getUsername());

            // Load user details and generate token
            final UserDetails userDetails = userDetailsService.loadUserByUsername(thisUser.getUsername());
            final String token = jwtUtils.generateToken(userDetails);
            
            log.info("JWT token generated for user: {}", thisUser.getUsername());
            
            // Return token as plain text
            return ResponseEntity.ok(token);
            
        } catch (org.springframework.security.authentication.DisabledException e) {
            log.error("User account is disabled: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("账户已被禁用，请联系管理员");
        } catch (org.springframework.security.authentication.LockedException e) {
            log.error("User account is locked: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("账户已被锁定，请联系管理员");
        } catch (BadCredentialsException e) {
            log.error("Bad credentials for user: {}", request.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户名或密码错误");
        } catch (Exception e) {
            log.error("Login error for user: {}", request.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("登录失败: " + e.getMessage());
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        // 由于使用的是无状态的 JWT 认证，登出操作通常在客户端完成
        return ResponseEntity.ok("Logout successful. Please delete the token on the client side.");
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(@RequestHeader("Authorization") String token) {
        try {
            String jwt = token.substring(7); // 去掉 "Bearer "
            String username = jwtUtils.extractUsername(jwt);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            String newToken = jwtUtils.generateToken(userDetails);
            return ResponseEntity.ok(newToken);
        } catch (Exception e) {
            log.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token refresh failed");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody UserDtoForCreate userDtoForCreate) {
        try {
            log.info("Registration attempt for user: {}", userDtoForCreate.getUsername());
            userService.addUser(userDtoForCreate);
            log.info("User registered successfully: {}", userDtoForCreate.getUsername());
            return ResponseEntity.ok("User registered successfully.");
        } catch (Exception e) {
            log.error("Registration error for user: {}", userDtoForCreate.getUsername(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("注册失败: " + e.getMessage());
        }
    }
}