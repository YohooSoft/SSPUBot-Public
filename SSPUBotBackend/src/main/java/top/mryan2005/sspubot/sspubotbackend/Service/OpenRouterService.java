package top.mryan2005.sspubot.sspubotbackend.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class OpenRouterService {

    @Value("${spring.ai.openai.api-key}")
    private String openRouterApiKey;

    @Value("${spring.ai.openai.base-url:https://openrouter.ai/api/v1}")
    private String openRouterBaseUrl;

    @Value("${spring.ai.default-model}")
    private String defaultModel;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 2000;
    
    // Chat history analysis limits to avoid API request size issues
    private static final int MAX_HISTORY_MESSAGES_FOR_ANALYSIS = 10;
    private static final int MAX_MESSAGE_LENGTH_IN_ANALYSIS = 200;
    
    // Compiled regex pattern for performance
    private static final java.util.regex.Pattern INDEX_PATTERN = java.util.regex.Pattern.compile("(-?\\d+)");
    
    // Models that don't support system prompts (developer instructions)
    private static final Set<String> MODELS_WITHOUT_SYSTEM_PROMPT_SUPPORT = Set.of(
        "google/gemma-3-27b-it:free",
        "google/gemma-2-9b-it:free",
        "google/gemma-2-27b-it:free"
    );
    
    // Stop words for basic keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
        "什么", "是", "的", "了", "吗", "呢", "怎么", "如何", "为什么", 
        "哪里", "哪个", "谁", "when", "what", "where", "who", "why", "how", 
        "is", "are", "the", "a", "an"
    );

    public OpenRouterService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Analyze user question intent to determine if post searching is needed
     * Returns true if searching is needed, false for greetings or simple conversations
     * 
     * @param Openkey API key for OpenRouter
     * @param userQuestion The user's question
     * @param model The model to use
     * @return true if post search is needed, false otherwise
     */
    public boolean shouldSearchPosts(String Openkey, String userQuestion, String model) {
        try {
            String systemPrompt = "你是一个意图分析助手。分析用户的问题，判断是否需要搜索知识库。" +
                    "如果是打招呼、问候、闲聊等礼貌性对话，返回'NO'。" +
                    "如果是询问具体信息、知识、帮助等需要查询的问题，返回'YES'。" +
                    "只返回'YES'或'NO'，不要有其他解释。";
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "分析这个问题是否需要搜索知识库：" + userQuestion);
            messages.add(userMessage);
            
            String response = sendChatCompletion(Openkey, model, systemPrompt, messages);
            
            if (response.startsWith("Error:")) {
                log.warn("Intent analysis failed, defaulting to search: {}", response);
                return true; // Default to searching if analysis fails
            }
            
            boolean needsSearch = response.trim().toUpperCase().contains("YES");
            log.info("Intent analysis - Question needs search: {}", needsSearch);
            return needsSearch;
            
        } catch (Exception e) {
            log.error("Error analyzing intent", e);
            return true; // Default to searching on error
        }
    }
    
    /**
     * Extract keywords from user question for post searching
     * 
     * @param Openkey API key for OpenRouter
     * @param userQuestion The user's question
     * @param model The model to use
     * @return List of keywords
     */
    public List<String> extractKeywords(String Openkey, String userQuestion, String model) {
        try {
            String systemPrompt = "你是一个关键词提取助手。从用户的问题中提取最重要的关键词。" +
                    "只返回关键词，用逗号分隔，不要有任何解释。" +
                    "关注名词、主题和关键概念。提取3-7个关键词。";
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "从这个问题中提取关键词：" + userQuestion);
            messages.add(userMessage);
            
            String response = sendChatCompletion(Openkey, model, systemPrompt, messages);
            
            if (response.startsWith("Error:")) {
                log.warn("Keyword extraction failed: {}", response);
                return extractKeywordsBasic(userQuestion);
            }
            
            // Parse keywords from response
            List<String> keywords = new ArrayList<>();
            String[] parts = response.split("[,，、]");
            for (String keyword : parts) {
                String cleaned = keyword.trim();
                if (!cleaned.isEmpty()) {
                    keywords.add(cleaned);
                }
            }
            
            log.info("Extracted keywords: {}", keywords);
            return keywords;
            
        } catch (Exception e) {
            log.error("Error extracting keywords", e);
            return extractKeywordsBasic(userQuestion);
        }
    }
    
    /**
     * Basic keyword extraction as fallback
     */
    private List<String> extractKeywordsBasic(String userQuestion) {
        List<String> keywords = new ArrayList<>();
        
        String[] words = userQuestion.split("\\s+");
        for (String word : words) {
            word = word.trim();
            if (word.length() > 1 && !STOP_WORDS.contains(word.toLowerCase()) && keywords.size() < 7) {
                keywords.add(word);
            }
        }
        
        log.info("Extracted keywords (basic): {}", keywords);
        return keywords;
    }
    
    /**
     * Analyze chat history to find relevant previous messages
     * Returns the index of where relevant conversation starts, or -1 if no relevant history
     * 
     * @param Openkey API key for OpenRouter
     * @param currentQuestion Current user question
     * @param chatHistory List of previous messages (will be limited to recent messages to avoid API limits)
     * @param model The model to use
     * @return Index of first relevant message, or -1 if none are relevant
     */
    public int findRelevantChatHistoryIndex(String Openkey, String currentQuestion, 
            List<Map<String, String>> chatHistory, String model) {
        try {
            if (chatHistory == null || chatHistory.isEmpty()) {
                return -1;
            }
            
            // Limit chat history to last N messages to avoid API request size limits
            int startIndex = Math.max(0, chatHistory.size() - MAX_HISTORY_MESSAGES_FOR_ANALYSIS);
            List<Map<String, String>> recentHistory = chatHistory.subList(startIndex, chatHistory.size());
            
            // Build chat history context with relative indices
            StringBuilder historyContext = new StringBuilder();
            historyContext.append("最近的聊天历史：\n");
            for (int i = 0; i < recentHistory.size(); i++) {
                Map<String, String> msg = recentHistory.get(i);
                historyContext.append(i).append(". ");
                historyContext.append(msg.get("role")).append(": ");
                
                // Limit individual message length to avoid excessive context
                String content = msg.get("content");
                if (content != null && content.length() > MAX_MESSAGE_LENGTH_IN_ANALYSIS) {
                    content = content.substring(0, MAX_MESSAGE_LENGTH_IN_ANALYSIS) + "...";
                }
                historyContext.append(content).append("\n");
            }
            
            String systemPrompt = "你是一个对话上下文分析助手。分析当前问题与之前聊天记录的关系。" +
                    "如果当前问题是延续之前某个话题的，返回相关对话开始的位置索引（数字）。" +
                    "如果当前问题是全新话题，与之前对话无关，返回'-1'。" +
                    "只返回一个数字，不要有其他解释。";
            
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", historyContext.toString() + 
                    "\n当前问题：" + currentQuestion + 
                    "\n\n返回相关对话开始的索引，或-1如果是新话题：");
            messages.add(userMessage);
            
            String response = sendChatCompletion(Openkey, model, systemPrompt, messages);
            
            if (response.startsWith("Error:")) {
                log.warn("Chat history analysis failed, using recent history: {}", response);
                return startIndex; // Use start of recent history window if analysis fails
            }
            
            // Parse index from response
            try {
                String trimmed = response.trim();
                // Try to find a number (can be negative)
                java.util.regex.Matcher matcher = INDEX_PATTERN.matcher(trimmed);
                if (matcher.find()) {
                    int relativeIndex = Integer.parseInt(matcher.group(1));
                    if (relativeIndex == -1) {
                        log.info("Current question is a new topic");
                        return -1;
                    }
                    // Convert relative index to absolute index with bounds validation
                    int absoluteIndex = Math.max(0, Math.min(startIndex + relativeIndex, chatHistory.size() - 1));
                    log.info("Relevant chat history starts at absolute index: {} (relative: {})", absoluteIndex, relativeIndex);
                    return absoluteIndex;
                } else {
                    log.warn("Could not parse index from response: {}", response);
                    return startIndex; // Default to using recent history window
                }
            } catch (NumberFormatException e) {
                log.warn("Could not parse index from response: {}", response);
                return startIndex; // Default to using recent history window
            }
            
        } catch (Exception e) {
            log.error("Error analyzing chat history relevance", e);
            return 0; // Default to using all history on error
        }
    }

    /**
     * Send a chat completion request to OpenRouter API
     * 
     * @param model The model to use (e.g., "google/gemma-3-27b-it:free")
     * @param systemPrompt The system prompt for the bot
     * @param messages List of chat messages (role and content)
     * @return The AI response
     */
    public String sendChatCompletion(String Openkey, String model, String systemPrompt, List<Map<String, String>> messages) {
        int attempt = 0;
        Exception lastException = null;
        
        while (attempt < MAX_RETRIES) {
            attempt++;
            try {
                log.info("OpenRouter API attempt {}/{}", attempt, MAX_RETRIES);
                
                // Build messages array including system prompt
                List<Map<String, String>> allMessages = new ArrayList<>();
                
                // Check if model supports system prompts
                boolean modelSupportsSystemPrompt = !MODELS_WITHOUT_SYSTEM_PROMPT_SUPPORT.contains(defaultModel);
                
                if (systemPrompt != null && !systemPrompt.trim().isEmpty()) {
                    if (modelSupportsSystemPrompt) {
                        // Add system message as separate message
                        Map<String, String> systemMessage = new HashMap<>();
                        systemMessage.put("role", "system");
                        systemMessage.put("content", systemPrompt);
                        allMessages.add(systemMessage);
                    } else {
                        // For models without system prompt support, prepend system prompt to first user message
                        log.debug("Model {} doesn't support system prompts, embedding in user message", defaultModel);
                        if (!messages.isEmpty()) {
                            Map<String, String> firstMessage = messages.get(0);
                            if ("user".equals(firstMessage.get("role"))) {
                                Map<String, String> modifiedMessage = new HashMap<>(firstMessage);
                                modifiedMessage.put("content", systemPrompt + "\n\n" + firstMessage.get("content"));
                                allMessages.add(modifiedMessage);
                                // Add remaining messages
                                for (int i = 1; i < messages.size(); i++) {
                                    allMessages.add(messages.get(i));
                                }
                            } else {
                                // If first message is not user, add system prompt as user message
                                Map<String, String> systemAsUser = new HashMap<>();
                                systemAsUser.put("role", "user");
                                systemAsUser.put("content", systemPrompt);
                                allMessages.add(systemAsUser);
                                allMessages.addAll(messages);
                            }
                        } else {
                            // No messages, just add system prompt as user message
                            Map<String, String> systemAsUser = new HashMap<>();
                            systemAsUser.put("role", "user");
                            systemAsUser.put("content", systemPrompt);
                            allMessages.add(systemAsUser);
                        }
                        // Skip adding original messages since we already handled them
                        messages = new ArrayList<>();
                    }
                }
                
                // Add conversation messages (if not already added above for no-system-prompt models)
                allMessages.addAll(messages);
                
                // Build request body
                Map<String, Object> requestBody = new HashMap<>();
                requestBody.put("model", defaultModel);
                requestBody.put("messages", allMessages);
                
                String jsonBody = objectMapper.writeValueAsString(requestBody);
                
                log.info("Sending request to OpenRouter API: model={}, messages={}, systemPromptLength={}", 
                        requestBody.get("model"), allMessages.size(), 
                        systemPrompt != null ? systemPrompt.length() : 0);
                log.debug("Request body length: {} chars", jsonBody.length());
                
                // Make HTTP request
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(openRouterBaseUrl + "/chat/completions"))
                        .header("Content-Type", "application/json")
                        .header("Authorization", "Bearer " + Openkey)
                        .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                        .timeout(Duration.ofSeconds(90))
                        .build();
                
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                log.info("OpenRouter API response status: {}", response.statusCode());
                if (response.statusCode() != 200) {
                    log.error("OpenRouter API error response: {}", response.body());
                }
                log.debug("OpenRouter API response body: {}", response.body());
                
                if (response.statusCode() == 200) {
                    // Parse response
                    @SuppressWarnings("unchecked")
                    Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
                    
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
                    
                    if (choices != null && !choices.isEmpty()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                        String content = (String) message.get("content");
                        return content != null ? content : "No response content";
                    }
                    
                    return "No response from AI";
                } else if (response.statusCode() == 502 || response.statusCode() == 503 || response.statusCode() == 504) {
                    // Gateway errors - retry
                    log.warn("OpenRouter API gateway error ({}), attempt {}/{}", response.statusCode(), attempt, MAX_RETRIES);
                    
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS * attempt); // Exponential backoff
                        continue;
                    }
                    
                    return "Error: AI service is temporarily unavailable (Status: " + response.statusCode() + "). Please try again later.";
                } else if (response.statusCode() == 429) {
                    // Rate limit - retry with longer delay
                    log.warn("OpenRouter API rate limit hit, attempt {}/{}", attempt, MAX_RETRIES);
                    
                    if (attempt < MAX_RETRIES) {
                        Thread.sleep(RETRY_DELAY_MS * attempt * 2);
                        continue;
                    }
                    
                    return "Error: Rate limit exceeded. Please wait a moment and try again.";
                } else if (response.statusCode() == 400) {
                    // Bad request - check if it's a model not found error
                    String responseBody = response.body();
                    log.error("OpenRouter API bad request: {}", responseBody);
                    
                    if (responseBody != null && responseBody.contains("not found")) {
                        return "Error: The selected AI model is not available. Please contact the administrator to update the bot configuration.";
                    }
                    return "Error: Invalid request format. Please check your message and try again.";
                } else if (response.statusCode() == 401 || response.statusCode() == 403) {
                    // Auth error - don't retry
                    log.error("OpenRouter API authentication error: {}", response.statusCode());
                    return "Error: API authentication failed. Please check the API key configuration.";
                } else {
                    // Other errors
                    log.error("OpenRouter API error: {} - {}", response.statusCode(), response.body());
                    return "Error: Failed to get response from AI service (Status: " + response.statusCode() + ")";
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.error("Request interrupted", e);
                return "Error: Request was interrupted";
            } catch (Exception e) {
                lastException = e;
                log.error("Error calling OpenRouter API (attempt {}/{})", attempt, MAX_RETRIES, e);
                
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        return "Error: Request was interrupted";
                    }
                }
            }
        }
        
        // All retries failed
        String errorMsg = lastException != null ? lastException.getMessage() : "Unknown error";
        log.error("All {} retry attempts failed. Last error: {}", MAX_RETRIES, errorMsg);
        return "Error: Unable to connect to AI service after " + MAX_RETRIES + " attempts. Please try again later.";
    }
}
