package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.mryan2005.sspubot.sspubotbackend.Pojo.*;
import top.mryan2005.sspubot.sspubotbackend.Repository.MemoryRepository;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.Repository.BotRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class MemoryService {

    @Autowired
    private MemoryRepository memoryRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BotRepository botRepository;

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get memory for a specific user and bot
     */
    public Optional<Memory> getMemory(Long userId, Long botId) {
        log.info("Fetching memory for user {} and bot {}", userId, botId);
        return memoryRepository.findByUserIdAndBotId(userId, botId);
    }

    /**
     * Get all memories for a user
     */
    public List<Memory> getAllMemoriesForUser(Long userId) {
        log.info("Fetching all memories for user {}", userId);
        return memoryRepository.findByUserId(userId);
    }

    /**
     * Create or update memory for a user and bot
     */
    @Transactional
    public Memory saveOrUpdateMemory(Long userId, Long botId, String content) {
        log.info("Saving/updating memory for user {} and bot {}", userId, botId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        Bot bot = botRepository.findById(botId)
                .orElseThrow(() -> new IllegalArgumentException("Bot not found: " + botId));
        
        Optional<Memory> existingMemory = memoryRepository.findByUserIdAndBotId(userId, botId);
        
        Memory memory;
        String now = LocalDateTime.now().format(DATE_TIME_FORMATTER);
        
        if (existingMemory.isPresent()) {
            memory = existingMemory.get();
            memory.setContent(content);
            memory.setUpdatedAt(now);
        } else {
            memory = new Memory();
            MemoryId memoryId = new MemoryId(userId, botId);
            memory.setId(memoryId);
            memory.setUser(user);
            memory.setBot(bot);
            memory.setContent(content);
            memory.setCreatedAt(now);
            memory.setUpdatedAt(now);
        }
        
        return memoryRepository.save(memory);
    }

    /**
     * Delete memory for a user and bot
     */
    @Transactional
    public void deleteMemory(Long userId, Long botId) {
        log.info("Deleting memory for user {} and bot {}", userId, botId);
        MemoryId memoryId = new MemoryId(userId, botId);
        memoryRepository.deleteById(memoryId);
    }

    /**
     * Get memory content as string for inclusion in system prompt
     */
    public String getMemoryContent(Long userId, Long botId) {
        Optional<Memory> memory = getMemory(userId, botId);
        return memory.map(Memory::getContent).orElse("");
    }
}
