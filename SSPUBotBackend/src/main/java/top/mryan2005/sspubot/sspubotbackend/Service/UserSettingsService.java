package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.mryan2005.sspubot.sspubotbackend.Pojo.User;
import top.mryan2005.sspubot.sspubotbackend.Pojo.UserSettings;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserSettingsRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@Slf4j
@Service
public class UserSettingsService {

    @Autowired
    private UserSettingsRepository userSettingsRepository;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get user settings by user ID
     */
    public Optional<UserSettings> findByUserId(Long userId) {
        return userSettingsRepository.findByUserId(userId);
    }

    /**
     * Get or create default user settings
     */
    public UserSettings getOrCreateSettings(Long userId) {
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByUserId(userId);
        
        if (settingsOpt.isPresent()) {
            return settingsOpt.get();
        }

        // Create default settings
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found with ID: " + userId);
        }

        UserSettings settings = new UserSettings();
        settings.setUserId(userId);
        settings.setUser(userOpt.get());
        settings.setCreatedAt(LocalDateTime.now());
        settings.setUpdatedAt(LocalDateTime.now());
        
        // All permissions are false by default
        return userSettingsRepository.save(settings);
    }

    /**
     * Update user settings
     */
    @Transactional
    public UserSettings updateSettings(Long userId, UserSettings newSettings) {
        // Always fetch the latest version from database to avoid optimistic locking issues
        Optional<UserSettings> settingsOpt = userSettingsRepository.findByUserId(userId);
        
        UserSettings existingSettings;
        if (settingsOpt.isPresent()) {
            existingSettings = settingsOpt.get();
        } else {
            // Create new settings if doesn't exist
            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found with ID: " + userId);
            }
            
            existingSettings = new UserSettings();
            existingSettings.setUserId(userId);
            existingSettings.setUser(userOpt.get());
            existingSettings.setCreatedAt(LocalDateTime.now());
        }

        // Update fields
        if (newSettings.getAllowBirthDate() != null) {
            existingSettings.setAllowBirthDate(newSettings.getAllowBirthDate());
        }
        if (newSettings.getAllowIsSchoolStudent() != null) {
            existingSettings.setAllowIsSchoolStudent(newSettings.getAllowIsSchoolStudent());
        }
        if (newSettings.getAllowEnrollmentDate() != null) {
            existingSettings.setAllowEnrollmentDate(newSettings.getAllowEnrollmentDate());
        }
        if (newSettings.getAllowGraduationDate() != null) {
            existingSettings.setAllowGraduationDate(newSettings.getAllowGraduationDate());
        }
        if (newSettings.getAllowEducationLevel() != null) {
            existingSettings.setAllowEducationLevel(newSettings.getAllowEducationLevel());
        }
        if (newSettings.getAllowGraduatedSchool() != null) {
            existingSettings.setAllowGraduatedSchool(newSettings.getAllowGraduatedSchool());
        }
        if (newSettings.getAllowHobbies() != null) {
            existingSettings.setAllowHobbies(newSettings.getAllowHobbies());
        }
        if (newSettings.getAllowFromLocation() != null) {
            existingSettings.setAllowFromLocation(newSettings.getAllowFromLocation());
        }
        if (newSettings.getAllowWantToGo() != null) {
            existingSettings.setAllowWantToGo(newSettings.getAllowWantToGo());
        }

        existingSettings.setUpdatedAt(LocalDateTime.now());
        return userSettingsRepository.save(existingSettings);
    }

    /**
     * Build user context string for AI based on settings
     */
    public String buildUserContextForAI(User user, UserSettings settings) {
        if (settings == null) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("[User Information]\n");

        if (Boolean.TRUE.equals(settings.getAllowBirthDate()) && user.getBirthDate() != null) {
            context.append("出生年月: ")
                   .append(user.getBirthDate().format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                   .append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowIsSchoolStudent()) && user.getIsSchoolStudent() != null) {
            context.append("是否本校学生: ")
                   .append(user.getIsSchoolStudent() ? "是" : "否")
                   .append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowEnrollmentDate()) && user.getEnrollmentDate() != null) {
            context.append("入学时间: ")
                   .append(user.getEnrollmentDate().format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                   .append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowGraduationDate()) && user.getGraduationDate() != null) {
            context.append("毕业时间: ")
                   .append(user.getGraduationDate().format(DateTimeFormatter.ofPattern("yyyy年MM月")))
                   .append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowEducationLevel()) && user.getEducationLevel() != null && !user.getEducationLevel().isEmpty()) {
            context.append("最高学历: ").append(user.getEducationLevel()).append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowGraduatedSchool()) && user.getGraduatedSchool() != null && !user.getGraduatedSchool().isEmpty()) {
            context.append("毕业学校: ").append(user.getGraduatedSchool()).append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowHobbies()) && user.getHobbies() != null && !user.getHobbies().isEmpty()) {
            context.append("爱好: ").append(user.getHobbies()).append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowFromLocation()) && user.getFromLocation() != null && !user.getFromLocation().isEmpty()) {
            context.append("来自: ").append(user.getFromLocation()).append("\n");
        }

        if (Boolean.TRUE.equals(settings.getAllowWantToGo()) && user.getWantToGo() != null && !user.getWantToGo().isEmpty()) {
            context.append("想去的地方: ").append(user.getWantToGo()).append("\n");
        }

        return context.toString();
    }
}
