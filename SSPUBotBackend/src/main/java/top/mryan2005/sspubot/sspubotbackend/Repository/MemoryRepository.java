package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Memory;
import top.mryan2005.sspubot.sspubotbackend.Pojo.MemoryId;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemoryRepository extends JpaRepository<Memory, MemoryId> {
    
    /**
     * Find memory for a specific user and bot
     */
    @Query("SELECT m FROM Memory m WHERE m.id.userId = :userId AND m.id.botId = :botId")
    Optional<Memory> findByUserIdAndBotId(@Param("userId") Long userId, @Param("botId") Long botId);
    
    /**
     * Find all memories for a specific user
     */
    @Query("SELECT m FROM Memory m WHERE m.id.userId = :userId")
    List<Memory> findByUserId(@Param("userId") Long userId);
}
