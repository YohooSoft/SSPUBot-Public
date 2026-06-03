package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Bot;

import java.util.List;
import java.util.Optional;

@Repository
public interface BotRepository extends JpaRepository<Bot, Long> {
    Optional<Bot> findByName(String name);
    
    List<Bot> findByIsActive(Boolean isActive);
    
    boolean existsByName(String name);
    
    // Return the first default bot ordered by most recently updated
    // This ensures deterministic behavior even if data inconsistency occurs
    Optional<Bot> findFirstByIsDefaultOrderByUpdatedAtDesc(Boolean isDefault);
    
    List<Bot> findAllByIsDefault(Boolean isDefault);
    
    // For backward compatibility
    default Optional<Bot> findByIsDefault(Boolean isDefault) {
        return findFirstByIsDefaultOrderByUpdatedAtDesc(isDefault);
    }
}
