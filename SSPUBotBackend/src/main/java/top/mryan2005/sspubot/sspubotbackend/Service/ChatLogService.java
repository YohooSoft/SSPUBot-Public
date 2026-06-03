package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.mryan2005.sspubot.sspubotbackend.Pojo.*;
import top.mryan2005.sspubot.sspubotbackend.Repository.ChatLogRepository;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.Repository.BotRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class ChatLogService {

    @Autowired
    private ChatLogRepository chatLogRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BotRepository botRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /**
     * Get all chat logs for a user and bot
     */
    public List<ChatLog> getChatHistory(Long userId, Long botId) {
        log.info("Fetching chat history for user {} and bot {}", userId, botId);
        return chatLogRepository.findByUserIdAndBotId(userId, botId);
    }

    /**
     * Save a chat message
     */
    @Transactional
    public ChatLog saveChatMessage(Long userId, Long botId, String content) {
        log.info("Saving chat message for user {} and bot {}", userId, botId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));
        
        String datetime = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        
        ChatLog chatLog = new ChatLog();
        ChatLogId chatLogId = new ChatLogId(userId, botId, datetime);
        chatLog.setId(chatLogId);
        chatLog.setUser(user);
        chatLog.setBot(bot);
        chatLog.setContent(content);
        
        return chatLogRepository.save(chatLog);
    }

    /**
     * Delete all chat history for a user and bot
     */
    @Transactional
    public void clearChatHistory(Long userId, Long botId) {
        log.info("Clearing chat history for user {} and bot {}", userId, botId);
        chatLogRepository.deleteByUserIdAndBotId(userId, botId);
    }

    /**
     * Delete a specific chat message
     */
    @Transactional
    public void deleteChatMessage(Long userId, Long botId, String datetime) {
        log.info("Deleting chat message for user {}, bot {}, datetime {}", userId, botId, datetime);
        chatLogRepository.deleteByUserIdAndBotIdAndDatetime(userId, botId, datetime);
    }
}
