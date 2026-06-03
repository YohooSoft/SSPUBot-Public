package top.mryan2005.sspubot.sspubotbackend.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import top.mryan2005.sspubot.sspubotbackend.Pojo.*;
import top.mryan2005.sspubot.sspubotbackend.Service.*;
import top.mryan2005.sspubot.sspubotbackend.Service.PostSearchService;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class BotController {

    @Autowired
    private BotService botService;
    
    @Autowired
    private ChatLogService chatLogService;
    
    @Autowired
    private MemoryService memoryService;
    
    @Autowired
    private OpenRouterService openRouterService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private PostSearchService postSearchService;

    @Autowired
    private UserSettingsService userSettingsService;

    @Autowired
    private SynonymService synonymService;

    /**
     * Get all bots for the current user
     */
    @GetMapping("/bots")
    public ResponseEntity<?> getUserBots(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("Fetching bots for user: {}", username);
            
            List<Bot> bots = botService.findByOwner(username);
            return ResponseEntity.ok(bots);
        } catch (Exception e) {
            log.error("Error fetching user bots", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取机器人列表失败: " + e.getMessage());
        }
    }

    /**
     * Get specific bot by ID
     */
    @GetMapping("/bots/{id}")
    public ResponseEntity<?> getBotById(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            Bot bot = botService.findById(id);
            
            if (bot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("机器人不存在");
            }
            
            // Check if user owns this bot (assuming bot has owner field)
            // For now, allow access to all bots
            log.info("User {} fetched bot: {}", username, id);
            return ResponseEntity.ok(bot);
        } catch (Exception e) {
            log.error("Error fetching bot by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取机器人失败: " + e.getMessage());
        }
    }

    /**
     * Create a new bot
     */
    @PostMapping("/bots")
    public ResponseEntity<?> createBot(@RequestBody Bot bot, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} creating new bot: {}", username, bot.getName());
            
            // Validation
            if (bot.getName() == null || bot.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("机器人名称不能为空");
            }
            if (bot.getSystemPrompt() == null || bot.getSystemPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("系统提示词不能为空");
            }
            
            // Set timestamps
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            bot.setCreatedAt(now);
            bot.setUpdatedAt(now);
            
            // Set default values
            if (bot.getIsActive() == null) {
                bot.setIsActive(true);
            }
            
            // Handle isDefault field - ensure only one bot can be default
            if (bot.getIsDefault() != null && bot.getIsDefault()) {
                // If setting as default during creation, we need to handle it specially
                // First create without default status to get an ID
                bot.setIsDefault(false);
                Bot createdBot = botService.save(bot);
                log.info("Bot created successfully with ID: {}", createdBot.getId());
                // Then set as default (which will unset others and set this one)
                Bot defaultBot = botService.setDefaultBot(createdBot.getId());
                log.info("Bot set as default: {}", createdBot.getId());
                return ResponseEntity.status(HttpStatus.CREATED).body(defaultBot);
            }
            
            Bot createdBot = botService.save(bot);
            log.info("Bot created successfully with ID: {}", createdBot.getId());
            
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBot);
        } catch (Exception e) {
            log.error("Error creating bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建机器人失败: " + e.getMessage());
        }
    }

    /**
     * Update an existing bot
     */
    @PutMapping("/bots/{id}")
    public ResponseEntity<?> updateBot(@PathVariable Long id, @RequestBody Bot bot, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} updating bot: {}", username, id);
            
            Bot existingBot = botService.findById(id);
            if (existingBot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("机器人不存在");
            }
            
            // Update fields
            if (bot.getName() != null) {
                existingBot.setName(bot.getName());
            }
            if (bot.getDescription() != null) {
                existingBot.setDescription(bot.getDescription());
            }
            if (bot.getAvatarUrl() != null) {
                existingBot.setAvatarUrl(bot.getAvatarUrl());
            }
            if (bot.getSystemPrompt() != null) {
                existingBot.setSystemPrompt(bot.getSystemPrompt());
            }
            if (bot.getSelectedModel() != null) {
                existingBot.setSelectedModel(bot.getSelectedModel());
            }
            if (bot.getApiKey() != null) {
                existingBot.setApiKey(bot.getApiKey());
            }
            if (bot.getBaseUrl() != null) {
                existingBot.setBaseUrl(bot.getBaseUrl());
            }
            if (bot.getIsActive() != null) {
                existingBot.setIsActive(bot.getIsActive());
            }
            
            // Handle isDefault field - ensure only one bot can be default
            if (bot.getIsDefault() != null && bot.getIsDefault()) {
                // If setting this bot as default, use service method with pending updates
                // This avoids double-save by handling all updates in one operation
                Bot updatedBot = botService.setDefaultBot(id, existingBot);
                log.info("Bot updated and set as default successfully: {}", id);
                return ResponseEntity.ok(updatedBot);
            } else if (bot.getIsDefault() != null && !bot.getIsDefault()) {
                // Explicitly unsetting default
                existingBot.setIsDefault(false);
            }
            
            existingBot.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Bot updatedBot = botService.save(existingBot);
            log.info("Bot updated successfully: {}", id);
            
            return ResponseEntity.ok(updatedBot);
        } catch (Exception e) {
            log.error("Error updating bot: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新机器人失败: " + e.getMessage());
        }
    }

    /**
     * Delete a bot
     */
    @DeleteMapping("/bots/{id}")
    public ResponseEntity<?> deleteBot(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} deleting bot: {}", username, id);
            
            Bot bot = botService.findById(id);
            if (bot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("机器人不存在");
            }
            
            botService.deleteById(id);
            log.info("Bot deleted successfully: {}", id);
            
            return ResponseEntity.ok("机器人删除成功");
        } catch (Exception e) {
            log.error("Error deleting bot: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除机器人失败: " + e.getMessage());
        }
    }

    /**
     * OpenRoute API: Chat with a bot
     * Integrates with OpenRouter API and manages chat history and memory
     */
    @PostMapping("/openroute/bot/chat")
    public ResponseEntity<?> chatWithBot(@RequestBody Map<String, Object> request, Authentication authentication) {
        try {
            Long botId = Long.valueOf(request.get("botId").toString());
            String message = request.get("message").toString();
            
            // Get user from authentication
            String username = authentication != null ? authentication.getName() : null;
            Long userId = null;
            
            if (username != null) {
                Optional<User> userOpt = userRepository.findByUsername(username);
                if (userOpt.isPresent()) {
                    userId = userOpt.get().getId();
                }
            }
            
            log.info("OpenRoute chat request for bot: {}, user: {}, message: {}", botId, username, message);
            
            // Validate input
            if (botId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "botId is required"));
            }
            if (message == null || message.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "message is required"));
            }
            
            // Get bot
            Bot bot = botService.findById(botId);
            if (bot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bot not found"));
            }
            
            // Check if bot is active
            if (!bot.getIsActive()) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Bot is not active"));
            }
            
            // Build system prompt with memory
            String systemPrompt = bot.getSystemPrompt();
            
            // Add markdown formatting instruction
            systemPrompt = systemPrompt + "\n\nIMPORTANT: Format your responses using Markdown syntax for better readability. Use headers, lists, code blocks, bold, italic, and other markdown features where appropriate.";
            
            // Add synonym knowledge base to system prompt
            try {
                String synonymContext = synonymService.buildSynonymContextForAI();
                if (!synonymContext.isEmpty()) {
                    systemPrompt = systemPrompt + "\n\n" + synonymContext;
                    log.info("Added synonym knowledge base to system prompt");
                }
            } catch (Exception e) {
                log.warn("Failed to build synonym context for AI, continuing without it", e);
            }
            
            // Expand message with synonyms for better AI understanding
            String enrichedMessage = message;
            try {
                String synonymExpansion = synonymService.expandTextForAI(message);
                if (!synonymExpansion.equals(message)) {
                    log.info("Added synonym context to user message");
                    enrichedMessage = synonymExpansion;
                }
            } catch (Exception e) {
                log.warn("Failed to expand message with synonyms, continuing without expansion", e);
            }
            
            if (userId != null) {
                // Add user profile information based on settings
                Optional<User> userOpt = userRepository.findById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    UserSettings settings = userSettingsService.getOrCreateSettings(userId);
                    String userContext = userSettingsService.buildUserContextForAI(user, settings);
                    if (!userContext.isEmpty()) {
                        systemPrompt = systemPrompt + "\n\n" + userContext;
                    }
                }
                
                String memoryContent = memoryService.getMemoryContent(userId, botId);
                if (!memoryContent.isEmpty()) {
                    systemPrompt = systemPrompt + "\n\n[Memory about this user]\n" + memoryContent;
                }
            }
            
            // Get chat history and build messages for OpenRouter
            List<Map<String, String>> messages = new ArrayList<>();
            
            if (userId != null) {
                List<ChatLog> chatHistory = chatLogService.getChatHistory(userId, botId);
                for (ChatLog log : chatHistory) {
                    // Parse stored chat log content which contains role and message
                    // Format: "role:message" or just message (assume user)
                    String content = log.getContent();
                    String[] parts = content.split(":", 2);
                    
                    Map<String, String> msg = new HashMap<>();
                    if (parts.length == 2 && (parts[0].equals("user") || parts[0].equals("assistant"))) {
                        msg.put("role", parts[0]);
                        msg.put("content", parts[1]);
                    } else {
                        msg.put("role", "user");
                        msg.put("content", content);
                    }
                    messages.add(msg);
                }
            }
            
            // Retrieve and integrate relevant posts from knowledge base using new workflow
            // Get model selection (with fallback to default model)
            String model = bot.getSelectedModel();
            if (model == null || model.trim().isEmpty()) {
                model = "google/gemma-3-27b-it:free";  // Use default model
            }
            
            // Get API key for RAG process
            String apiKey = bot.getApiKey();
            if (apiKey == null || apiKey.trim().isEmpty()) {
                log.warn("Bot API key is not configured, using system default for RAG");
                // RAG will use the system default API key from OpenRouterService
            }
            
            log.info("Starting new RAG workflow for bot {} with model {}", botId, model);
            
            try {
                // Use new workflow that includes intent analysis, Top-K filtering, and chat history analysis
                String relevantContent = postSearchService.retrieveAndIntegrateWithHistory(
                        apiKey, message, messages, model);
                log.info("New RAG workflow completed, content length: {}", 
                        relevantContent != null ? relevantContent.length() : 0);
                
                if (!relevantContent.isEmpty()) {
                    // Limit the total system prompt length to avoid API issues
                    int maxSystemPromptLength = 8000; // Conservative limit for API compatibility
                    int currentLength = systemPrompt.length();
                    int availableSpace = maxSystemPromptLength - currentLength;
                    
                    if (availableSpace > 500) { // Only add if we have reasonable space
                        if (relevantContent.length() > availableSpace) {
                            relevantContent = relevantContent.substring(0, availableSpace - 100) + "...(truncated due to length limit)";
                            log.info("RAG content truncated from {} to {} characters", 
                                    relevantContent.length(), availableSpace - 100);
                        }
                        systemPrompt = systemPrompt + "\n\n" + relevantContent;
                        log.info("RAG content added to system prompt, new length: {}", systemPrompt.length());
                    } else {
                        log.warn("System prompt too long ({}), skipping RAG content", currentLength);
                    }
                } else {
                    log.info("RAG returned empty content (no search needed or no relevant info found)");
                }
            } catch (Exception e) {
                log.error("Error in new RAG workflow, continuing without RAG", e);
                // Continue without RAG content if there's an error
            }
            
            // Add current user message (use enriched message with synonym context)
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", enrichedMessage);
            messages.add(userMessage);
            
            // Call OpenRouter API with fallback model
            String aiResponse = openRouterService.sendChatCompletion(bot.getApiKey(), model, systemPrompt, messages);
            
            // Save chat history if user is authenticated (save original message, not enriched)
            if (userId != null) {
                chatLogService.saveChatMessage(userId, botId, "user:" + message);
                chatLogService.saveChatMessage(userId, botId, "assistant:" + aiResponse);
            }
            
            // Build response
            Map<String, Object> response = new HashMap<>();
            response.put("botId", botId);
            response.put("botName", bot.getName());
            response.put("message", message);
            response.put("response", aiResponse);
            response.put("responseFormat", "markdown");  // Indicate response supports markdown rendering
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            log.info("OpenRoute chat response sent for bot: {}", botId);
            return ResponseEntity.ok(response);
            
        } catch (NumberFormatException e) {
            log.error("Invalid botId format", e);
            return ResponseEntity.badRequest().body(Map.of("error", "Invalid botId format"));
        } catch (Exception e) {
            log.error("Error in OpenRoute chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Chat failed: " + e.getMessage()));
        }
    }
    
    /**
     * Get chat history for a user and bot
     */
    @GetMapping("/chat/history")
    public ResponseEntity<?> getChatHistory(@RequestParam Long botId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            List<ChatLog> chatLogs = chatLogService.getChatHistory(userId, botId);
            
            // Convert to response format
            List<Map<String, Object>> messages = chatLogs.stream().map(log -> {
                Map<String, Object> msg = new HashMap<>();
                String content = log.getContent();
                String[] parts = content.split(":", 2);
                
                if (parts.length == 2 && (parts[0].equals("user") || parts[0].equals("assistant"))) {
                    msg.put("role", parts[0]);
                    msg.put("content", parts[1]);
                } else {
                    msg.put("role", "user");
                    msg.put("content", content);
                }
                msg.put("datetime", log.getId().getDatetime());
                return msg;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(messages);
            
        } catch (Exception e) {
            log.error("Error fetching chat history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch chat history: " + e.getMessage()));
        }
    }
    
    /**
     * Clear all chat history for a user and bot
     */
    @DeleteMapping("/chat/history")
    public ResponseEntity<?> clearChatHistory(@RequestParam Long botId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            chatLogService.clearChatHistory(userId, botId);
            
            return ResponseEntity.ok(Map.of("message", "Chat history cleared"));
            
        } catch (Exception e) {
            log.error("Error clearing chat history", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to clear chat history: " + e.getMessage()));
        }
    }
    
    /**
     * Delete a specific chat message
     */
    @DeleteMapping("/chat/message")
    public ResponseEntity<?> deleteChatMessage(@RequestParam Long botId, 
                                                @RequestParam String datetime, 
                                                Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            chatLogService.deleteChatMessage(userId, botId, datetime);
            
            return ResponseEntity.ok(Map.of("message", "Message deleted"));
            
        } catch (Exception e) {
            log.error("Error deleting message", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete message: " + e.getMessage()));
        }
    }
    
    /**
     * Get memory for a specific bot
     */
    @GetMapping("/memory")
    public ResponseEntity<?> getMemory(@RequestParam Long botId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            Optional<Memory> memory = memoryService.getMemory(userId, botId);
            
            if (memory.isPresent()) {
                Map<String, Object> result = new HashMap<>();
                result.put("botId", botId);
                result.put("content", memory.get().getContent());
                result.put("createdAt", memory.get().getCreatedAt());
                result.put("updatedAt", memory.get().getUpdatedAt());
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.ok(Map.of("botId", botId, "content", ""));
            }
            
        } catch (Exception e) {
            log.error("Error fetching memory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch memory: " + e.getMessage()));
        }
    }
    
    /**
     * Get all memories for the current user
     */
    @GetMapping("/memory/all")
    public ResponseEntity<?> getAllMemories(Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            List<Memory> memories = memoryService.getAllMemoriesForUser(userId);
            
            List<Map<String, Object>> result = memories.stream().map(memory -> {
                Map<String, Object> item = new HashMap<>();
                item.put("botId", memory.getId().getBotId());
                item.put("botName", memory.getBot().getName());
                item.put("content", memory.getContent());
                item.put("createdAt", memory.getCreatedAt());
                item.put("updatedAt", memory.getUpdatedAt());
                return item;
            }).collect(Collectors.toList());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error fetching all memories", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch memories: " + e.getMessage()));
        }
    }
    
    /**
     * Update memory for a specific bot
     */
    @PostMapping("/memory")
    public ResponseEntity<?> updateMemory(@RequestBody Map<String, Object> request, 
                                          Authentication authentication) {
        try {
            Long botId = Long.valueOf(request.get("botId").toString());
            String content = request.get("content").toString();
            
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            Memory memory = memoryService.saveOrUpdateMemory(userId, botId, content);
            
            Map<String, Object> result = new HashMap<>();
            result.put("botId", botId);
            result.put("content", memory.getContent());
            result.put("updatedAt", memory.getUpdatedAt());
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error updating memory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update memory: " + e.getMessage()));
        }
    }
    
    /**
     * Delete memory for a specific bot
     */
    @DeleteMapping("/memory")
    public ResponseEntity<?> deleteMemory(@RequestParam Long botId, Authentication authentication) {
        try {
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            memoryService.deleteMemory(userId, botId);
            
            return ResponseEntity.ok(Map.of("message", "Memory deleted"));
            
        } catch (Exception e) {
            log.error("Error deleting memory", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete memory: " + e.getMessage()));
        }
    }
    
    /**
     * Generate memory summary from chat history
     */
    @PostMapping("/memory/generate")
    public ResponseEntity<?> generateMemoryFromChat(@RequestBody Map<String, Object> request,
                                                     Authentication authentication) {
        try {
            Long botId = Long.valueOf(request.get("botId").toString());
            
            String username = authentication.getName();
            Optional<User> userOpt = userRepository.findByUsername(username);
            
            if (!userOpt.isPresent()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not found"));
            }
            
            Long userId = userOpt.get().getId();
            
            // Get bot
            Bot bot = botService.findById(botId);
            if (bot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Bot not found"));
            }
            
            // Get chat history
            List<ChatLog> chatHistory = chatLogService.getChatHistory(userId, botId);
            
            if (chatHistory.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No chat history available to generate memory"));
            }
            
            // Build conversation context for AI
            StringBuilder conversationContext = new StringBuilder();
            conversationContext.append("Recent conversation:\n\n");
            
            int messageCount = Math.min(chatHistory.size(), 20); // Last 20 messages
            for (int i = Math.max(0, chatHistory.size() - messageCount); i < chatHistory.size(); i++) {
                ChatLog log = chatHistory.get(i);
                String content = log.getContent();
                String[] parts = content.split(":", 2);
                
                if (parts.length == 2) {
                    conversationContext.append(parts[0].toUpperCase()).append(": ").append(parts[1]).append("\n");
                } else {
                    conversationContext.append("USER: ").append(content).append("\n");
                }
            }
            
            // Create prompt for memory generation
            String systemPrompt = "你是一个乐于助人的助手，负责创建简洁的记忆摘要。根据对话提取关于用户的关键事实（偏好、兴趣、他们分享的个人信息、目标等）。仅返回事实性信息，以简洁格式呈现（3-5 条要点）。具体且基于事实。";
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", conversationContext.toString() + 
                    "\n\n请基于上述对话生成关于用户的记忆摘要。");
            messages.add(userMessage);
            
            // Call OpenRouter to generate summary
            String model = bot.getSelectedModel();
            if (model == null || model.trim().isEmpty()) {
                model = "qwen/qwen-2.5-vl-7b-instruct:free";
            }
            
            String memorySummary = openRouterService.sendChatCompletion(bot.getApiKey(), model, systemPrompt, messages);
            
            // Check if it's an error message
            if (memorySummary.startsWith("Error:")) {
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body(Map.of("error", memorySummary));
            }
            
            // Return the generated memory
            Map<String, Object> result = new HashMap<>();
            result.put("botId", botId);
            result.put("generatedMemory", memorySummary);
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error generating memory from chat", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to generate memory: " + e.getMessage()));
        }
    }
    
    /**
     * Get the default bot
     */
    @GetMapping("/bots/default")
    public ResponseEntity<?> getDefaultBot(Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} fetching default bot", username);
            
            Optional<Bot> defaultBot = botService.getDefaultBot();
            if (defaultBot.isPresent()) {
                return ResponseEntity.ok(defaultBot.get());
            } else {
                return ResponseEntity.ok(Map.of("message", "No default bot set"));
            }
            
        } catch (Exception e) {
            log.error("Error fetching default bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch default bot: " + e.getMessage()));
        }
    }
    
    /**
     * Set a bot as the default bot
     */
    @PostMapping("/bots/{id}/set-default")
    public ResponseEntity<?> setDefaultBot(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} setting bot {} as default", username, id);
            
            Bot bot = botService.setDefaultBot(id);
            return ResponseEntity.ok(bot);
            
        } catch (IllegalArgumentException e) {
            log.error("Bot not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting default bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set default bot: " + e.getMessage()));
        }
    }
    
    /**
     * Unset a bot as the default bot
     */
    @PostMapping("/bots/{id}/unset-default")
    public ResponseEntity<?> unsetDefaultBot(@PathVariable Long id, Authentication authentication) {
        try {
            String username = authentication.getName();
            log.info("User {} unsetting bot {} as default", username, id);
            
            Bot bot = botService.unsetDefaultBot(id);
            return ResponseEntity.ok(bot);
            
        } catch (IllegalArgumentException e) {
            log.error("Bot not found: {}", id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error unsetting default bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to unset default bot: " + e.getMessage()));
        }
    }
}
