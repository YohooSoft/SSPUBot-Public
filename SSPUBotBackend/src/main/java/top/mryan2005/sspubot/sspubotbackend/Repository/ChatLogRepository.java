package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import top.mryan2005.sspubot.sspubotbackend.Pojo.ChatLog;
import top.mryan2005.sspubot.sspubotbackend.Pojo.ChatLogId;

import java.util.List;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, ChatLogId> {
    
    /**
     * Find all chat logs for a specific user and bot
     */
    @Query("SELECT c FROM ChatLog c WHERE c.id.userId = :userId AND c.id.botId = :botId ORDER BY c.id.datetime ASC")
    List<ChatLog> findByUserIdAndBotId(@Param("userId") Long userId, @Param("botId") Long botId);
    
    /**
     * Delete all chat logs for a specific user and bot
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatLog c WHERE c.id.userId = :userId AND c.id.botId = :botId")
    void deleteByUserIdAndBotId(@Param("userId") Long userId, @Param("botId") Long botId);
    
    /**
     * Delete a specific chat log by user, bot, and datetime
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM ChatLog c WHERE c.id.userId = :userId AND c.id.botId = :botId AND c.id.datetime = :datetime")
    void deleteByUserIdAndBotIdAndDatetime(@Param("userId") Long userId, @Param("botId") Long botId, @Param("datetime") String datetime);
}
