package top.mryan2005.sspubot.sspubotbackend.Controller;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Dto.UserDto;
import top.mryan2005.sspubot.sspubotbackend.Pojo.User;
import top.mryan2005.sspubot.sspubotbackend.Pojo.UserSettings;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForCreate;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForLogin;
import top.mryan2005.sspubot.sspubotbackend.Response.ResponseMessage;
import top.mryan2005.sspubot.sspubotbackend.Service.UserService;
import top.mryan2005.sspubot.sspubotbackend.Service.UserSettingsService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/user")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    UserRepository userRepository;

    @Autowired
    UserSettingsService userSettingsService;

    @PostMapping("/register")
    public ResponseMessage<User> registerUser(@Valid @RequestBody UserDtoForCreate userDtoForCreate) {
        return new ResponseMessage<>(HttpStatus.CREATED.value(), "User created successfully", userService.addUser(userDtoForCreate));
    }

    @PostMapping("/login")
    public ResponseMessage<UserDto> loginUser(@Valid @RequestBody UserDtoForLogin userDtoForLogin) {
        return new ResponseMessage<>(HttpStatus.OK.value(), "User logged in successfully", userService.findThisUser(userDtoForLogin));
    }

    /**
     * Get current user's profile
     */
    @GetMapping("/profile")
    public ResponseEntity<?> getUserProfile(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();
            // Remove sensitive information
            user.setPassword(null);
            user.setSalt(null);

            log.info("User {} fetched their profile", username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error fetching user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取用户资料失败: " + e.getMessage()));
        }
    }

    /**
     * Update current user's profile
     */
    @PutMapping("/profile")
    public ResponseEntity<?> updateUserProfile(@RequestBody Map<String, Object> updates, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();

            // Update profile fields
            if (updates.containsKey("displayName")) {
                user.setDisplayName((String) updates.get("displayName"));
            }
            
            // Parse dates with proper error handling
            try {
                if (updates.containsKey("birthDate") && updates.get("birthDate") != null) {
                    String dateStr = (String) updates.get("birthDate");
                    // Parse ISO 8601 string with timezone and convert to LocalDateTime
                    Instant instant = Instant.parse(dateStr);
                    user.setBirthDate(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                }
            } catch (Exception e) {
                log.error("Invalid birthDate format: {}", updates.get("birthDate"), e);
                return ResponseEntity.badRequest().body(Map.of("error", "出生日期格式无效"));
            }
            
            if (updates.containsKey("isSchoolStudent")) {
                user.setIsSchoolStudent((Boolean) updates.get("isSchoolStudent"));
            }
            
            try {
                if (updates.containsKey("enrollmentDate") && updates.get("enrollmentDate") != null) {
                    String dateStr = (String) updates.get("enrollmentDate");
                    // Parse ISO 8601 string with timezone and convert to LocalDateTime
                    Instant instant = Instant.parse(dateStr);
                    user.setEnrollmentDate(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                }
            } catch (Exception e) {
                log.error("Invalid enrollmentDate format: {}", updates.get("enrollmentDate"), e);
                return ResponseEntity.badRequest().body(Map.of("error", "入学日期格式无效"));
            }
            
            try {
                if (updates.containsKey("graduationDate") && updates.get("graduationDate") != null) {
                    String dateStr = (String) updates.get("graduationDate");
                    // Parse ISO 8601 string with timezone and convert to LocalDateTime
                    Instant instant = Instant.parse(dateStr);
                    user.setGraduationDate(LocalDateTime.ofInstant(instant, ZoneId.systemDefault()));
                }
            } catch (Exception e) {
                log.error("Invalid graduationDate format: {}", updates.get("graduationDate"), e);
                return ResponseEntity.badRequest().body(Map.of("error", "毕业日期格式无效"));
            }
            
            if (updates.containsKey("educationLevel")) {
                user.setEducationLevel((String) updates.get("educationLevel"));
            }
            if (updates.containsKey("graduatedSchool")) {
                user.setGraduatedSchool((String) updates.get("graduatedSchool"));
            }
            if (updates.containsKey("hobbies")) {
                user.setHobbies((String) updates.get("hobbies"));
            }
            if (updates.containsKey("fromLocation")) {
                user.setFromLocation((String) updates.get("fromLocation"));
            }
            if (updates.containsKey("wantToGo")) {
                user.setWantToGo((String) updates.get("wantToGo"));
            }

            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            // Remove sensitive information before returning
            user.setPassword(null);
            user.setSalt(null);

            log.info("User {} updated their profile", username);
            return ResponseEntity.ok(user);
        } catch (Exception e) {
            log.error("Error updating user profile", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新用户资料失败: " + e.getMessage()));
        }
    }

    /**
     * Get current user's settings
     */
    @GetMapping("/settings")
    public ResponseEntity<?> getUserSettings(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在"));
            }

            Long userId = userOpt.get().getId();
            UserSettings settings = userSettingsService.getOrCreateSettings(userId);

            log.info("User {} fetched their settings", username);
            return ResponseEntity.ok(settings);
        } catch (Exception e) {
            log.error("Error fetching user settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "获取用户设置失败: " + e.getMessage()));
        }
    }

    /**
     * Update current user's settings
     */
    @PutMapping("/settings")
    public ResponseEntity<?> updateUserSettings(@RequestBody UserSettings settings, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在"));
            }

            Long userId = userOpt.get().getId();
            
            // Retry logic for optimistic locking failures
            int maxRetries = 3;
            int attempt = 0;
            Exception lastException = null;
            
            while (attempt < maxRetries) {
                try {
                    UserSettings updatedSettings = userSettingsService.updateSettings(userId, settings);
                    log.info("User {} updated their settings", username);
                    return ResponseEntity.ok(updatedSettings);
                } catch (org.springframework.orm.ObjectOptimisticLockingFailureException e) {
                    lastException = e;
                    attempt++;
                    log.warn("Optimistic locking failure for user {} settings update, attempt {}/{}", 
                            username, attempt, maxRetries);
                    
                    if (attempt < maxRetries) {
                        // Brief pause before retry
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            // All retries failed
            log.error("Error updating user settings after {} attempts", maxRetries, lastException);
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "更新用户设置失败，请重试"));
                    
        } catch (Exception e) {
            log.error("Error updating user settings", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "更新用户设置失败: " + e.getMessage()));
        }
    }

    /**
     * Change password
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestBody Map<String, String> passwordData, Authentication authentication) {
        try {
            String username = authentication.getName();
            String oldPassword = passwordData.get("oldPassword");
            String newPassword = passwordData.get("newPassword");

            if (oldPassword == null || oldPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "当前密码不能为空"));
            }

            if (newPassword == null || newPassword.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "新密码不能为空"));
            }

            if (newPassword.length() < 6) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "新密码长度至少为6位"));
            }

            Optional<User> userOpt = userRepository.findByUsername(username);

            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "用户不存在"));
            }

            User user = userOpt.get();

            // Verify old password
            if (!userService.verifyPassword(oldPassword, user.getPassword())) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "当前密码错误"));
            }

            // Update to new password
            user.setPassword(userService.encodePassword(newPassword));
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("User {} changed their password", username);
            return ResponseEntity.ok(Map.of("message", "密码修改成功"));

        } catch (Exception e) {
            log.error("Error changing password", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "密码修改失败: " + e.getMessage()));
        }
    }
}
