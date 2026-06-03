package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Bot;
import top.mryan2005.sspubot.sspubotbackend.Repository.BotRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class BotService {

    @Autowired
    private BotRepository botRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Get all bots
     */
    public List<Bot> getAllBots() {
        return botRepository.findAll();
    }

    /**
     * Get all active bots
     */
    public List<Bot> getActiveBots() {
        return botRepository.findByIsActive(true);
    }

    /**
     * Get bot by ID
     */
    public Optional<Bot> getBotById(Long id) {
        return botRepository.findById(id);
    }

    /**
     * Get bot by name
     */
    public Optional<Bot> getBotByName(String name) {
        return botRepository.findByName(name);
    }

    /**
     * Create a new bot
     */
    public Bot createBot(Bot bot) {
        if (botRepository.existsByName(bot.getName())) {
            throw new IllegalArgumentException("Bot with name '" + bot.getName() + "' already exists");
        }

        String now = LocalDateTime.now().format(DATE_FORMATTER);
        bot.setCreatedAt(now);
        bot.setUpdatedAt(now);
        
        if (bot.getIsActive() == null) {
            bot.setIsActive(true);
        }

        log.info("Creating new bot: {}", bot.getName());
        return botRepository.save(bot);
    }

    /**
     * Update an existing bot
     */
    public Bot updateBot(Long id, Bot updatedBot) {
        Bot existingBot = botRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot with ID " + id + " not found"));

        // Update fields
        if (updatedBot.getName() != null && !updatedBot.getName().equals(existingBot.getName())) {
            if (botRepository.existsByName(updatedBot.getName())) {
                throw new IllegalArgumentException("Bot with name '" + updatedBot.getName() + "' already exists");
            }
            existingBot.setName(updatedBot.getName());
        }

        if (updatedBot.getDescription() != null) {
            existingBot.setDescription(updatedBot.getDescription());
        }

        if (updatedBot.getAvatarUrl() != null) {
            existingBot.setAvatarUrl(updatedBot.getAvatarUrl());
        }

        if (updatedBot.getSystemPrompt() != null) {
            existingBot.setSystemPrompt(updatedBot.getSystemPrompt());
        }

        if (updatedBot.getSelectedModel() != null) {
            existingBot.setSelectedModel(updatedBot.getSelectedModel());
        }

        if (updatedBot.getApiKey() != null) {
            existingBot.setApiKey(updatedBot.getApiKey());
        }

        if (updatedBot.getBaseUrl() != null) {
            existingBot.setBaseUrl(updatedBot.getBaseUrl());
        }

        if (updatedBot.getIsActive() != null) {
            existingBot.setIsActive(updatedBot.getIsActive());
        }

        existingBot.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));

        log.info("Updating bot: {} (ID: {})", existingBot.getName(), id);
        return botRepository.save(existingBot);
    }

    /**
     * Delete a bot
     */
    public void deleteBot(Long id) {
        if (!botRepository.existsById(id)) {
            throw new IllegalArgumentException("Bot with ID " + id + " not found");
        }
        log.info("Deleting bot with ID: {}", id);
        botRepository.deleteById(id);
    }

    /**
     * Toggle bot active status
     */
    public Bot toggleBotStatus(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot with ID " + id + " not found"));
        
        bot.setIsActive(!bot.getIsActive());
        bot.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        
        log.info("Toggling bot {} status to: {}", bot.getName(), bot.getIsActive());
        return botRepository.save(bot);
    }

    /**
     * Find bot by ID (returns Bot or null)
     */
    public Bot findById(Long id) {
        return botRepository.findById(id).orElse(null);
    }

    /**
     * Save or update bot
     */
    public Bot save(Bot bot) {
        if (bot.getId() == null) {
            // New bot
            return createBot(bot);
        } else {
            // Update existing bot
            return botRepository.save(bot);
        }
    }

    /**
     * Delete bot by ID
     */
    public void deleteById(Long id) {
        deleteBot(id);
    }

    /**
     * Find bots by owner (placeholder - assuming bot has owner field in future)
     */
    public List<Bot> findByOwner(String owner) {
        // For now, return all active bots
        // TODO: Add owner field to Bot entity and filter by owner
        log.info("Finding bots for owner: {} (currently returning all active bots)", owner);
        return getActiveBots();
    }

    /**
     * Find all bots (for admin)
     */
    public List<Bot> findAll() {
        return botRepository.findAll();
    }
    
    /**
     * Get the default bot
     */
    public Optional<Bot> getDefaultBot() {
        return botRepository.findByIsDefault(true);
    }
    
    /**
     * Get all bots with isDefault status
     */
    public List<Bot> findAllByIsDefault() {
        return botRepository.findAllByIsDefault(true);
    }
    
    /**
     * Set a bot as the default bot
     * Automatically unsets any other bot that was previously set as default
     * Also updates any pending changes on the target bot
     */
    public Bot setDefaultBot(Long id, Bot updatedFields) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot with ID " + id + " not found"));
        
        // Apply any pending field updates if provided
        if (updatedFields != null) {
            if (updatedFields.getName() != null) {
                bot.setName(updatedFields.getName());
            }
            if (updatedFields.getDescription() != null) {
                bot.setDescription(updatedFields.getDescription());
            }
            if (updatedFields.getAvatarUrl() != null) {
                bot.setAvatarUrl(updatedFields.getAvatarUrl());
            }
            if (updatedFields.getSystemPrompt() != null) {
                bot.setSystemPrompt(updatedFields.getSystemPrompt());
            }
            if (updatedFields.getSelectedModel() != null) {
                bot.setSelectedModel(updatedFields.getSelectedModel());
            }
            if (updatedFields.getApiKey() != null) {
                bot.setApiKey(updatedFields.getApiKey());
            }
            if (updatedFields.getBaseUrl() != null) {
                bot.setBaseUrl(updatedFields.getBaseUrl());
            }
            if (updatedFields.getIsActive() != null) {
                bot.setIsActive(updatedFields.getIsActive());
            }
        }
        
        // First, unset all other bots as default using batch operation
        List<Bot> currentDefaultBots = botRepository.findAllByIsDefault(true);
        List<Bot> botsToUpdate = new ArrayList<>();
        
        for (Bot currentDefault : currentDefaultBots) {
            if (!currentDefault.getId().equals(id)) {
                currentDefault.setIsDefault(false);
                currentDefault.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
                botsToUpdate.add(currentDefault);
                log.info("Unset bot {} (ID: {}) as default", currentDefault.getName(), currentDefault.getId());
            }
        }
        
        // Save all updates in batch
        if (!botsToUpdate.isEmpty()) {
            botRepository.saveAll(botsToUpdate);
        }
        
        // Set the requested bot as default
        bot.setIsDefault(true);
        bot.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        
        log.info("Set bot {} (ID: {}) as default", bot.getName(), id);
        return botRepository.save(bot);
    }
    
    /**
     * Set a bot as the default bot (overload without field updates)
     */
    public Bot setDefaultBot(Long id) {
        return setDefaultBot(id, null);
    }
    
    /**
     * Unset a bot as the default bot
     */
    public Bot unsetDefaultBot(Long id) {
        Bot bot = botRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bot with ID " + id + " not found"));
        
        bot.setIsDefault(false);
        bot.setUpdatedAt(LocalDateTime.now().format(DATE_FORMATTER));
        
        log.info("Unset bot {} (ID: {}) as default", bot.getName(), id);
        return botRepository.save(bot);
    }
}
