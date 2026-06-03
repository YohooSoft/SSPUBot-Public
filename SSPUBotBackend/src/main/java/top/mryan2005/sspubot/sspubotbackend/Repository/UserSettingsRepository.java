package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.UserSettings;

import java.util.Optional;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    
    /**
     * Find user settings by user ID
     */
    Optional<UserSettings> findByUserId(Long userId);
}
