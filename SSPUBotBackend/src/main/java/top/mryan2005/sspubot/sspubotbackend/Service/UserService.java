package top.mryan2005.sspubot.sspubotbackend.Service;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.mryan2005.simplifiedjava.Hash;
import top.mryan2005.simplifiedjava.MD5;
import top.mryan2005.sspubot.sspubotbackend.Exception.*;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Dto.UserDto;
import top.mryan2005.sspubot.sspubotbackend.Pojo.User;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForCreate;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.UserDtoForLogin;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class UserService {
    @Autowired
    UserRepository userRepository;
    
    @Autowired
    PasswordEncoder passwordEncoder;

    /**
     * 增加用户
     * 使用 BCrypt 加密密码以匹配 Spring Security 的认证机制
     */
    public User addUser(UserDtoForCreate userDtoForCreate) {
        // 检查用户是否存在
        if (userRepository.findByUsername(userDtoForCreate.getUsername()).isPresent()) {
            throw new ThisUsernameIsExisted("User already exists");
        }
        if (userRepository.findByEmail(userDtoForCreate.getEmail()).isPresent()) {
            throw new ThisEmailIsExisted("Email already exists");
        }

        // 创建新用户实体
        User newUser = new User();
        BeanUtils.copyProperties(userDtoForCreate, newUser);
        
        // 使用 BCrypt 加密密码（与 Spring Security 配置一致）
        String encodedPassword = passwordEncoder.encode(userDtoForCreate.getPassword());
        newUser.setPassword(encodedPassword);
        
        newUser.setDisplayName(userDtoForCreate.getUsername());
        
        // Salt 字段保留用于兼容性，但 BCrypt 内部已包含 salt
        // 生成一个随机 salt 字符串以满足数据库非空约束
        newUser.setSalt(UUID.randomUUID().toString());
        
        // 使用 MD5 生成头像 URL
        MD5 md5 = new MD5();
        newUser.setAvatarUrl("https://cravatar.cn/avatar/"+ md5.generateMd5Hash(userDtoForCreate.getEmail()).hash);
        
        newUser.setCreatedAt(LocalDateTime.now());
        newUser.setUpdatedAt(LocalDateTime.now());
        
        // 设置默认状态为激活 (Status = 1)
        newUser.setStatus(1);

        // 保存用户到数据库
        User savedUser = userRepository.save(newUser);

        return savedUser;
    }

    public UserDto findThisUser(UserDtoForLogin userDtoForLogin) {
        User user;
        if (userDtoForLogin.getUsername() != null && !userDtoForLogin.getUsername().isEmpty()) {
            user = userRepository.findByUsername(userDtoForLogin.getUsername())
                    .orElseThrow(() -> new ThisUserNotFound("User not found"));
        } else if (userDtoForLogin.getEmail() != null && !userDtoForLogin.getEmail().isEmpty()) {
            user = userRepository.findByEmail(userDtoForLogin.getEmail())
                    .orElseThrow(() -> new ThisUserNotFound("User not found"));
        } else {
            throw new InvalidLoginInfo("User not found");
        }
        
        // 密码验证由 Spring Security 的 AuthenticationManager 处理
        // 这里只返回用户信息，密码匹配交给 BCrypt
        // 注意：传递原始密码给 AuthenticationManager，它会使用 BCrypt 进行验证
        
        // 返回用户信息，密码需要返回数据库中的加密密码
        UserDto userDto = new UserDto();
        BeanUtils.copyProperties(user, userDto);
        return userDto;
    }

    /**
     * Verify if the raw password matches the encoded password
     */
    public boolean verifyPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * Encode a raw password using BCrypt
     */
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }
}
