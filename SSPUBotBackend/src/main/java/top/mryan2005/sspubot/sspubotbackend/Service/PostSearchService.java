package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;
import top.mryan2005.sspubot.sspubotbackend.Repository.PostRepository;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for searching and filtering posts based on user queries
 * Implements RAG (Retrieval-Augmented Generation) pattern with three-step AI query approach:
 * 1. Analyze user question to understand intent
 * 2. Generate search keywords based on analysis
 * 3. Integrate retrieved content using AI
 */
@Slf4j
@Service
public class PostSearchService {

    // Configuration constants
    private static final int MAX_KEYWORDS = 7;
    private static final int RELEVANCE_CHECK_MULTIPLIER = 2;
    private static final int RELEVANCE_CHECK_CONTENT_LIMIT = 500;
    private static final int INTEGRATION_CONTENT_LIMIT = 2000;
    private static final int DEFAULT_MAX_POSTS = 10;
    
    // Chat history integration limits to avoid excessive context
    private static final int MAX_RELEVANT_HISTORY_MESSAGES = 5;
    private static final int MAX_MESSAGE_LENGTH_IN_INTEGRATION = 300;
    
    // Compiled regex patterns for performance
    private static final java.util.regex.Pattern SCORE_PATTERN = java.util.regex.Pattern.compile("\\b([0-9]|10)\\b");
    private static final java.util.regex.Pattern INDEX_PATTERN = java.util.regex.Pattern.compile("(-?\\d+)");
    
    // Stop words for basic keyword extraction
    private static final Set<String> STOP_WORDS = Set.of(
        "什么", "是", "的", "了", "吗", "呢", "怎么", "如何", "为什么", 
        "哪里", "哪个", "谁", "when", "what", "where", "who", "why", "how", 
        "is", "are", "the", "a", "an"
    );

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private OpenRouterService openRouterService;

    @Autowired
    private SynonymService synonymService;

    /**
     * Extract keywords from user question using AI
     * 
     * @param userQuestion The user's question
     * @param model The AI model to use
     * @return List of keywords
     */
    public List<String> extractKeywords(String ApiKey, String userQuestion, String model) {
        try {
            String systemPrompt = "You are a keyword extraction assistant. Extract the most important keywords " +
                    "from the user's question. Return only the keywords separated by commas, without any explanation. " +
                    "Focus on nouns, topics, and key concepts. Extract 3-7 keywords.";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Extract keywords from this question: " + userQuestion);
            messages.add(userMessage);

            String response = openRouterService.sendChatCompletion(ApiKey, model, systemPrompt, messages);
            
            // Handle error responses
            if (response.startsWith("Error:")) {
                log.warn("Failed to extract keywords using AI, falling back to basic extraction: {}", response);
                return extractKeywordsBasic(userQuestion);
            }

            // Parse keywords from response
            List<String> keywords = Arrays.stream(response.split("[,，、]"))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());

            log.info("Extracted keywords: {}", keywords);
            return keywords;

        } catch (Exception e) {
            log.error("Error extracting keywords with AI", e);
            return extractKeywordsBasic(userQuestion);
        }
    }

    /**
     * Basic keyword extraction as fallback
     * 
     * @param userQuestion The user's question
     * @return List of keywords
     */
    private List<String> extractKeywordsBasic(String userQuestion) {
        // Simple extraction: split by spaces and filter out common words
        List<String> keywords = Arrays.stream(userQuestion.split("\\s+"))
                .map(String::trim)
                .filter(w -> w.length() > 1)
                .filter(w -> !STOP_WORDS.contains(w.toLowerCase()))
                .limit(MAX_KEYWORDS)
                .collect(Collectors.toList());

        log.info("Extracted keywords (basic): {}", keywords);
        return keywords;
    }

    /**
     * Search for posts that contain the keywords in their postWords, postName, and postContentUsingMarkdown fields
     * Uses database-level search for better efficiency
     * 
     * Posts are scored based on where keywords are found:
     * - postName match: +3 points (highest weight - titles are most relevant)
     * - postContentUsingMarkdown match: +2 points (medium weight - content is important)
     * - postWords match: +1 point (lower weight - keywords are general tags)
     * 
     * @param keywords List of keywords to search for
     * @return List of posts that match the keywords, sorted by relevance score (highest first)
     */
    public List<Post> searchPostsByKeywords(List<String> keywords) {
        if (keywords == null || keywords.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Expand keywords with synonyms
            List<String> expandedKeywords = new ArrayList<>(keywords);
            for (String keyword : keywords) {
                try {
                    List<String> relatedTerms = synonymService.getRelatedTerms(keyword);
                    // Add related terms (excluding the original keyword which is already in the list)
                    for (String term : relatedTerms) {
                        if (!expandedKeywords.contains(term) && !term.equals(keyword)) {
                            expandedKeywords.add(term);
                        }
                    }
                } catch (Exception e) {
                    log.warn("Failed to get synonyms for keyword: {}", keyword, e);
                }
            }
            
            log.info("Expanded {} keywords to {} keywords (including synonyms)", 
                    keywords.size(), expandedKeywords.size());
            
            // Use a Map to track posts and their match scores
            Map<Long, PostMatchScore> postScores = new HashMap<>();

            // Search database for each keyword (including synonyms)
            for (String keyword : expandedKeywords) {
                List<Post> posts = postRepository.findByPostWordsContaining(keyword);
                for (Post post : posts) {
                    // Use computeIfAbsent to avoid redundant object creation
                    PostMatchScore score = postScores.computeIfAbsent(post.getId(), id -> new PostMatchScore(post));
                    score.incrementScore(keyword);
                }
            }

            // Sort by score and return posts
            List<Post> sortedPosts = postScores.values().stream()
                    .sorted((a, b) -> Integer.compare(b.score, a.score))
                    .map(pms -> pms.post)
                    .collect(Collectors.toList());

            log.info("Found {} posts matching keywords from database search", sortedPosts.size());
            return sortedPosts;

        } catch (Exception e) {
            log.error("Error searching posts by keywords", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Helper class to track post match scores
     */
    private static class PostMatchScore {
        final Post post;
        int score;
        
        PostMatchScore(Post post) {
            this.post = post;
            this.score = 0;
        }
        
        void incrementScore(String keyword) {
            // Convert keyword to lowercase once for performance
            String keywordLower = keyword.toLowerCase();
            
            // Check postName (weight: 3)
            if (post.getPostName() != null && 
                post.getPostName().toLowerCase().contains(keywordLower)) {
                score += 3;
            }
            // Check postContentUsingMarkdown (weight: 2)
            if (post.getPostContentUsingMarkdown() != null && 
                post.getPostContentUsingMarkdown().toLowerCase().contains(keywordLower)) {
                score += 2;
            }
            // Check postWords (weight: 1)
            if (post.getPostWords() != null && 
                post.getPostWords().toLowerCase().contains(keywordLower)) {
                score += 1;
            }
        }
    }

    /**
     * Filter posts by relevance to the user's question using Top-K approach
     * Selects the top K most relevant posts based on simplified content
     * 
     * @param posts List of candidate posts
     * @param userQuestion The user's question
     * @param model The AI model to use
     * @param topK Number of posts to return (default 3)
     * @return List of top K relevant posts
     */
    public List<Post> filterTopKRelevantPosts(String ApiKey, List<Post> posts, String userQuestion, String model, int topK, String currentTime) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Use all candidate posts for scoring
            List<PostRelevanceScore> scoredPosts = new ArrayList<>();

            // Score each post for relevance using traditional ML
            for (Post post : posts) {
                // Use simplified content for relevance check
                String contentToCheck = post.getPostSimplifiedContent();
                if (contentToCheck == null || contentToCheck.trim().isEmpty()) {
                    contentToCheck = post.getPostContentUsingMarkdown();
                }
                if (contentToCheck == null || contentToCheck.trim().isEmpty()) {
                    contentToCheck = post.getPostName();
                }

                // Calculate relevance score using TF-IDF and cosine similarity
                double relevanceScore = calculateRelevanceScore(userQuestion, contentToCheck, post.getPostName(), post.getPostReleaseTime(), currentTime);
                scoredPosts.add(new PostRelevanceScore(post, relevanceScore));
            }

            // Sort by relevance score and return top K
            List<Post> topKPosts = scoredPosts.stream()
                    .sorted((a, b) -> Double.compare(b.score, a.score))
                    .limit(topK)
                    .filter(prs -> prs.score > 0) // Only include posts with positive relevance
                    .map(prs -> prs.post)
                    .collect(Collectors.toList());

            log.info("Selected {} posts from {} candidates using Top-K filtering (Traditional ML)", topKPosts.size(), posts.size());
            return topKPosts;

        } catch (Exception e) {
            log.error("Error filtering Top-K relevant posts", e);
            // Return first topK posts as fallback
            return posts.stream().limit(topK).collect(Collectors.toList());
        }
    }
    
    /**
     * Helper class for post relevance scoring
     */
    private static class PostRelevanceScore {
        final Post post;
        final double score;
        
        PostRelevanceScore(Post post, double score) {
            this.post = post;
            this.score = score;
        }
    }
    
    /**
     * Calculate relevance score using traditional ML methods (TF-IDF and cosine similarity)
     * with time decay factor
     * 
     * @param question The user's question
     * @param content The content to check
     * @param postTitle The title of the post
     * @param postReleaseTime Release time of the post
     * @param currentTime Current time
     * @return Relevance score (0.0 to 1.0)
     */
    private double calculateRelevanceScore(String question, String content, String postTitle, String postReleaseTime, String currentTime) {
        try {
            // Combine title and content for better matching (title has higher weight)
            String combinedContent = postTitle + " " + postTitle + " " + content; // Title counted twice for emphasis
            
            // Tokenize and normalize
            List<String> questionTokens = tokenize(question.toLowerCase());
            List<String> contentTokens = tokenize(combinedContent.toLowerCase());
            
            if (questionTokens.isEmpty() || contentTokens.isEmpty()) {
                return 0.0;
            }
            
            // Calculate TF-IDF based cosine similarity
            double cosineSimilarity = calculateCosineSimilarity(questionTokens, contentTokens);
            
            // Calculate time decay factor (newer posts get higher scores)
            double timeFactor = calculateTimeFactor(postReleaseTime, currentTime);
            
            // Combine content similarity with time factor
            // 70% content relevance, 30% time recency
            double finalScore = (cosineSimilarity * 0.7) + (timeFactor * 0.3);
            
            log.debug("Relevance score for '{}': content={}, time={}, final={}", 
                     postTitle, cosineSimilarity, timeFactor, finalScore);
            
            return finalScore;
            
        } catch (Exception e) {
            log.error("Error calculating relevance score", e);
            return 0.0;
        }
    }
    
    /**
     * Tokenize text into words, removing stop words and punctuation
     */
    private List<String> tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Split by whitespace and punctuation
        String[] words = text.split("[\\s\\p{Punct}]+");
        List<String> tokens = new ArrayList<>();
        
        for (String word : words) {
            word = word.trim();
            // Keep Chinese characters and alphanumeric words
            if (!word.isEmpty() && word.length() >= 2 && !STOP_WORDS.contains(word)) {
                tokens.add(word);
            }
        }
        
        return tokens;
    }
    
    /**
     * Calculate cosine similarity between two token lists using TF-IDF
     */
    private double calculateCosineSimilarity(List<String> tokens1, List<String> tokens2) {
        // Build term frequency maps
        Map<String, Integer> tf1 = buildTermFrequency(tokens1);
        Map<String, Integer> tf2 = buildTermFrequency(tokens2);
        
        // Get all unique terms
        Set<String> allTerms = new HashSet<>();
        allTerms.addAll(tf1.keySet());
        allTerms.addAll(tf2.keySet());
        
        if (allTerms.isEmpty()) {
            return 0.0;
        }
        
        // Calculate dot product and magnitudes
        double dotProduct = 0.0;
        double magnitude1 = 0.0;
        double magnitude2 = 0.0;
        
        for (String term : allTerms) {
            double freq1 = tf1.getOrDefault(term, 0);
            double freq2 = tf2.getOrDefault(term, 0);
            
            // Apply TF weighting (log normalization)
            double weight1 = freq1 > 0 ? 1 + Math.log(freq1) : 0;
            double weight2 = freq2 > 0 ? 1 + Math.log(freq2) : 0;
            
            dotProduct += weight1 * weight2;
            magnitude1 += weight1 * weight1;
            magnitude2 += weight2 * weight2;
        }
        
        if (magnitude1 == 0 || magnitude2 == 0) {
            return 0.0;
        }
        
        return dotProduct / (Math.sqrt(magnitude1) * Math.sqrt(magnitude2));
    }
    
    /**
     * Build term frequency map from token list
     */
    private Map<String, Integer> buildTermFrequency(List<String> tokens) {
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokens) {
            tf.merge(token, 1, Integer::sum);
        }
        return tf;
    }
    
    /**
     * Calculate time decay factor (0.0 to 1.0, where 1.0 is most recent)
     * Posts within 7 days get 1.0
     * Posts older than 180 days get 0.0
     * Linear decay between 7 and 180 days
     */
    private double calculateTimeFactor(String postReleaseTime, String currentTime) {
        try {
            if (postReleaseTime == null || postReleaseTime.isEmpty()) {
                return 0.5; // Default to middle if no time information
            }
            
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            LocalDateTime postTime = LocalDateTime.parse(postReleaseTime.substring(0, Math.min(19, postReleaseTime.length())), formatter);
            LocalDateTime now = LocalDateTime.parse(currentTime, formatter);
            
            long daysDiff = java.time.Duration.between(postTime, now).toDays();
            
            if (daysDiff < 0) {
                // Future date, treat as very recent
                return 1.0;
            } else if (daysDiff <= 7) {
                // Within a week, full score
                return 1.0;
            } else if (daysDiff >= 180) {
                // Older than 6 months, minimum score
                return 0.1;
            } else {
                // Linear decay between 7 and 180 days
                return 1.0 - ((daysDiff - 7.0) / 173.0) * 0.9;
            }
            
        } catch (Exception e) {
            log.warn("Error parsing time for time factor calculation: {}", e.getMessage());
            return 0.5; // Default to middle on error
        }
    }

    /**
     * Filter posts by relevance to the user's question using AI
     * 
     * @param posts List of candidate posts
     * @param userQuestion The user's question
     * @param model The AI model to use
     * @param maxPosts Maximum number of posts to return
     * @return List of relevant posts
     */
    public List<Post> filterRelevantPosts(String ApiKey, List<Post> posts, String userQuestion, String model, int maxPosts) {
        if (posts == null || posts.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            List<Post> relevantPosts = new ArrayList<>();

            // Check each post for relevance (check up to RELEVANCE_CHECK_MULTIPLIER * maxPosts candidates)
            for (int i = 0; i < Math.min(posts.size(), maxPosts * RELEVANCE_CHECK_MULTIPLIER); i++) {
                Post post = posts.get(i);
                
                // Use simplified content if available, otherwise use the title
                String contentToCheck = post.getPostSimplifiedContent();
                if (contentToCheck == null || contentToCheck.trim().isEmpty()) {
                    contentToCheck = post.getPostName();
                }

                // Limit content length to avoid token limits
                if (contentToCheck.length() > RELEVANCE_CHECK_CONTENT_LIMIT) {
                    contentToCheck = contentToCheck.substring(0, RELEVANCE_CHECK_CONTENT_LIMIT) + "...";
                }

                boolean isRelevant = checkRelevance(ApiKey, userQuestion, contentToCheck, model);
                
                if (isRelevant) {
                    relevantPosts.add(post);
                    if (relevantPosts.size() >= maxPosts) {
                        break;
                    }
                }
            }

            log.info("Filtered to {} relevant posts from {} candidates", relevantPosts.size(), posts.size());
            return relevantPosts;

        } catch (Exception e) {
            log.error("Error filtering relevant posts", e);
            // Return first few posts as fallback
            return posts.stream().limit(maxPosts).collect(Collectors.toList());
        }
    }

    /**
     * Check if content is relevant to the question using AI
     * 
     * @param question The user's question
     * @param content The content to check
     * @param model The AI model to use
     * @return true if relevant, false otherwise
     */
    private boolean checkRelevance(String ApiKey, String question, String content, String model) {
        try {
            String systemPrompt = "You are a relevance checker. Determine if the given content is relevant " +
                    "to answering the user's question. Respond with ONLY 'YES' or 'NO'.";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", String.format(
                    "Question: %s\n\nContent: %s\n\nIs this content relevant to the question? Answer YES or NO.",
                    question, content
            ));
            messages.add(userMessage);

            String response = openRouterService.sendChatCompletion(ApiKey, model, systemPrompt, messages);
            
            // Handle error responses - assume relevant to avoid losing data
            if (response.startsWith("Error:")) {
                log.warn("Failed to check relevance, assuming relevant: {}", response);
                return true;
            }

            boolean relevant = response.trim().toUpperCase().contains("YES");
            log.debug("Relevance check result: {}", relevant);
            return relevant;

        } catch (Exception e) {
            log.error("Error checking relevance", e);
            // Assume relevant on error to avoid losing potentially useful data
            return true;
        }
    }

    /**
     * Integrate post contents using only simplified content
     * 
     * @param posts List of relevant posts (should be Top-K filtered, typically 3)
     * @return Integrated content string
     */
    public String integratePostSimplifiedContents(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return "";
        }

        StringBuilder integrated = new StringBuilder();
        integrated.append("[知识库相关信息]\n\n");

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            integrated.append("=== 文档 ").append(i + 1).append(": ").append(post.getPostName()).append(" ===\n");
            
            // Use simplified content
            String content = post.getPostSimplifiedContent();
            if (content == null || content.trim().isEmpty()) {
                // Fallback to markdown content if simplified is not available
                content = post.getPostContentUsingMarkdown();
            }
            if (content == null || content.trim().isEmpty()) {
                content = post.getPostContent();
            }

            if (content != null && !content.trim().isEmpty()) {
                // Limit content length per post to avoid token limits
                if (content.length() > INTEGRATION_CONTENT_LIMIT) {
                    content = content.substring(0, INTEGRATION_CONTENT_LIMIT) + "...(truncated)";
                }
                integrated.append(content).append("\n\n");
            }
            
            // Add source URL if available
            if (post.getPostUrl() != null && !post.getPostUrl().trim().isEmpty()) {
                integrated.append("来源: ").append(post.getPostUrl()).append("\n\n");
            }
        }

        log.info("Integrated {} posts (simplified content) into knowledge context", posts.size());
        return integrated.toString();
    }

    /**
     * Integrate post contents into a comprehensive answer context
     * 
     * @param posts List of relevant posts
     * @return Integrated content string
     */
    public String integratePostContents(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return "";
        }

        StringBuilder integrated = new StringBuilder();
        integrated.append("[Relevant Information from Knowledge Base]\n\n");

        for (int i = 0; i < posts.size(); i++) {
            Post post = posts.get(i);
            integrated.append("=== Document ").append(i + 1).append(": ").append(post.getPostName()).append(" ===\n");
            
            // Use markdown content if available
            String content = post.getPostContentUsingMarkdown();
            if (content == null || content.trim().isEmpty()) {
                content = post.getPostSimplifiedContent();
            }
            if (content == null || content.trim().isEmpty()) {
                content = post.getPostContent();
            }

            if (content != null && !content.trim().isEmpty()) {
                // Limit content length per post to avoid token limits
                if (content.length() > INTEGRATION_CONTENT_LIMIT) {
                    content = content.substring(0, INTEGRATION_CONTENT_LIMIT) + "...(truncated)";
                }
                integrated.append(content).append("\n\n");
            }
            
            // Add source URL if available
            if (post.getPostUrl() != null && !post.getPostUrl().trim().isEmpty()) {
                integrated.append("Source: ").append(post.getPostUrl()).append("\n\n");
            }
        }

        log.info("Integrated {} posts into knowledge context", posts.size());
        return integrated.toString();
    }

    /**
     * Main method to retrieve and integrate relevant posts for a user question
     * Implements the new workflow:
     * 1. Analyze user intent (skip search for greetings)
     * 2. Extract keywords and search posts
     * 3. Use Top-K to select 3 most relevant posts
     * 4. Integrate simplified content
     * 5. Analyze chat history relevance
     * 6. Find answer in integrated content
     * 
     * @param userQuestion The user's question
     * @param chatHistory Previous chat messages (can be null)
     * @param model The AI model to use
     * @return Integrated content string to add to context, or empty if no search needed
     */
    public String retrieveAndIntegrateWithHistory(String ApiKey, String userQuestion, 
            List<Map<String, String>> chatHistory, String model) {
        try {
            log.info("Starting new RAG workflow for question: {}", userQuestion);

            // Step 1: Analyze user intent - should we search posts?
            boolean needsSearch = openRouterService.shouldSearchPosts(ApiKey, userQuestion, model);
            if (!needsSearch) {
                log.info("User question identified as greeting/casual conversation, skipping post search");
                return "";
            }

            // Step 2: Extract keywords from user question
            List<String> keywords = openRouterService.extractKeywords(ApiKey, userQuestion, model);
            if (keywords.isEmpty()) {
                log.info("No keywords extracted, skipping post search");
                return "";
            }

            // Step 3: Search posts by keywords in postContentUsingMarkdown and postName
            List<Post> candidatePosts = searchPostsByKeywords(keywords);
            if (candidatePosts.isEmpty()) {
                log.info("No posts found matching keywords");
                return "";
            }
            log.info("Found {} candidate posts", candidatePosts.size());
            
            // Get current time for relevance scoring
            String currentTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            log.info("Current time for relevance scoring: {}", currentTime);

            // Step 4: Use Top-K filtering to select 10 most relevant posts
            List<Post> topKPosts = filterTopKRelevantPosts(ApiKey, candidatePosts, userQuestion, model, DEFAULT_MAX_POSTS, currentTime);
            if (topKPosts.isEmpty()) {
                log.info("No relevant posts after Top-K filtering");
                return "";
            }
            log.info("Selected {} posts using Top-K filtering", topKPosts.size());

            // Step 5: Integrate simplified content from selected posts
            String postsContent = integratePostSimplifiedContents(topKPosts);

            // Step 6: Analyze chat history for relevance
            String relevantHistory = "";
            if (chatHistory != null && !chatHistory.isEmpty()) {
                int relevantStartIndex = openRouterService.findRelevantChatHistoryIndex(
                        ApiKey, userQuestion, chatHistory, model);
                
                if (relevantStartIndex >= 0 && relevantStartIndex < chatHistory.size()) {
                    // Build relevant chat history context
                    StringBuilder historyBuilder = new StringBuilder();
                    historyBuilder.append("[相关聊天历史]\n\n");
                    
                    // Start from relevantStartIndex but limit to MAX_RELEVANT_HISTORY_MESSAGES
                    // If there are more than MAX messages from the relevant point, only include the last MAX messages
                    int actualStartIndex = Math.max(relevantStartIndex, chatHistory.size() - MAX_RELEVANT_HISTORY_MESSAGES);
                    
                    for (int i = actualStartIndex; i < chatHistory.size(); i++) {
                        Map<String, String> msg = chatHistory.get(i);
                        String role = msg.get("role");
                        String content = msg.get("content");
                        
                        // Limit individual message length
                        if (content != null && content.length() > MAX_MESSAGE_LENGTH_IN_INTEGRATION) {
                            content = content.substring(0, MAX_MESSAGE_LENGTH_IN_INTEGRATION) + "...";
                        }
                        
                        historyBuilder.append(role.equals("user") ? "用户: " : "助手: ");
                        historyBuilder.append(content).append("\n");
                    }
                    historyBuilder.append("\n");
                    
                    relevantHistory = historyBuilder.toString();
                    log.info("Added relevant chat history from index {} (actual start: {}, {} messages)", 
                            relevantStartIndex, actualStartIndex, chatHistory.size() - actualStartIndex);
                } else {
                    log.info("Current question is a new topic, no relevant history");
                }
            }

            // Step 7: Combine history and posts content
            String finalContext = relevantHistory + postsContent;
            
            log.info("RAG workflow completed, total context length: {}", finalContext.length());
            return finalContext;

        } catch (Exception e) {
            log.error("Error in new RAG workflow", e);
            return "";
        }
    }

    /**
     * Main method to retrieve and integrate relevant posts for a user question
     * Uses a three-step AI query approach:
     * 1. Analyze user question
     * 2. Generate search keywords
     * 3. Integrate content
     * 
     * @param userQuestion The user's question
     * @param model The AI model to use
     * @return Integrated content string to add to system prompt
     */
    public String retrieveAndIntegrateRelevantPosts(String ApiKey, String userQuestion, String model) {
        try {
            log.info("Starting three-step RAG process for question: {}", userQuestion);

            // Step 1: Analyze user question to understand intent
            String analysis = analyzeUserQuestion(ApiKey, userQuestion, model);
            if (analysis.startsWith("Error:")) {
                log.warn("Failed to analyze question: {}", analysis);
                return "";
            }
            log.info("Question analysis completed");

            // Step 2: Generate search keywords to find relevant posts
            String sqlQuery = generateSQLQuery(ApiKey, userQuestion, analysis, model);
            if (sqlQuery.startsWith("Error:")) {
                log.warn("Failed to generate search keywords: {}", sqlQuery);
                return "";
            }
            log.info("Generated search keywords: {}", sqlQuery);

            // Step 3: Execute query and retrieve posts
            List<Post> relevantPosts = executeSearchQuery(sqlQuery);
            if (relevantPosts.isEmpty()) {
                log.info("No posts found matching the query");
                return "";
            }
            log.info("Found {} relevant posts", relevantPosts.size());

            // Step 4: Use AI to integrate the content
            String integratedContent = integrateContentWithAI(ApiKey, userQuestion, relevantPosts, model);
            
            return integratedContent;

        } catch (Exception e) {
            log.error("Error in retrieve and integrate relevant posts", e);
            return "";
        }
    }

    /**
     * Step 1: Analyze the user's question to understand intent and key topics
     * 
     * @param userQuestion The user's question
     * @param model The AI model to use
     * @return Analysis of the question
     */
    private String analyzeUserQuestion(String ApiKey, String userQuestion, String model) {
        try {
            String systemPrompt = "You are a question analysis assistant. Analyze the user's question " +
                    "and identify: 1) The main intent, 2) Key topics/entities mentioned, 3) What information " +
                    "the user is looking for. Provide a concise analysis in 2-3 sentences.";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", "Analyze this question: " + userQuestion);
            messages.add(userMessage);

            String response = openRouterService.sendChatCompletion(ApiKey, model, systemPrompt, messages);
            
            if (response.startsWith("Error:")) {
                log.warn("AI analysis failed: {}", response);
                return response;
            }

            log.debug("Question analysis: {}", response);
            return response;

        } catch (Exception e) {
            log.error("Error analyzing user question", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Step 2: Generate search keywords to find relevant posts based on question analysis
     * 
     * @param userQuestion The user's question
     * @param analysis The analysis from step 1
     * @param model The AI model to use
     * @return Search keywords (comma-separated) for querying the database
     */
    private String generateSQLQuery(String ApiKey, String userQuestion, String analysis, String model) {
        try {
            String systemPrompt = "You are a SQL generation assistant. Based on the user's question and analysis, " +
                    "generate search keywords (NOT a full SQL query) that can be used to search the postWords column. " +
                    "The postWords column contains comma-separated keywords related to each post. " +
                    "Return ONLY the keywords separated by commas, without any explanation or SQL syntax. " +
                    "Example output: 'admission, requirements, application'";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", String.format(
                    "Question: %s\n\nAnalysis: %s\n\nGenerate search keywords:",
                    userQuestion, analysis
            ));
            messages.add(userMessage);

            String response = openRouterService.sendChatCompletion(ApiKey, model, systemPrompt, messages);
            
            if (response.startsWith("Error:")) {
                log.warn("SQL generation failed: {}", response);
                return response;
            }

            // Clean up the response to get just the keywords
            String keywords = response.trim()
                    .replaceAll("(?i)^(keywords?:|search terms?:)\\s*", "")
                    .replaceAll("[\"'`]", "")
                    .trim();

            log.debug("Generated keywords: {}", keywords);
            return keywords;

        } catch (Exception e) {
            log.error("Error generating SQL query", e);
            return "Error: " + e.getMessage();
        }
    }

    /**
     * Execute search query using generated keywords
     * 
     * @param keywords Comma-separated keywords
     * @return List of relevant posts
     */
    private List<Post> executeSearchQuery(String keywords) {
        try {
            if (keywords == null || keywords.trim().isEmpty()) {
                return new ArrayList<>();
            }

            // Parse keywords
            List<String> keywordList = Arrays.stream(keywords.split("[,，]"))
                    .map(String::trim)
                    .filter(k -> !k.isEmpty())
                    .collect(Collectors.toList());

            if (keywordList.isEmpty()) {
                return new ArrayList<>();
            }

            log.info("Executing search with keywords: {}", keywordList);

            // Use database search - searches both postWords and postName columns
            Set<Post> matchingPosts = new HashSet<>();
            for (String keyword : keywordList) {
                List<Post> posts = postRepository.findByPostWordsContaining(keyword);
                matchingPosts.addAll(posts);
                log.debug("Keyword '{}' matched {} posts", keyword, posts.size());
            }

            // Limit results
            List<Post> results = new ArrayList<>(matchingPosts);
            if (results.size() > DEFAULT_MAX_POSTS * 2) {
                results = results.subList(0, DEFAULT_MAX_POSTS * 2);
            }

            log.info("Found {} posts from database", results.size());
            return results;

        } catch (Exception e) {
            log.error("Error executing search query", e);
            return new ArrayList<>();
        }
    }

    /**
     * Step 3: Use AI to integrate content from retrieved posts into a coherent answer
     * 
     * @param userQuestion The user's question
     * @param posts List of relevant posts
     * @param model The AI model to use
     * @return Integrated content
     */
    private String integrateContentWithAI(String ApiKey, String userQuestion, List<Post> posts, String model) {
        try {
            if (posts.isEmpty()) {
                return "";
            }

            // Prepare post contents for AI
            StringBuilder postsContent = new StringBuilder();
            postsContent.append("Available information from knowledge base:\n\n");

            int postNum = 1;
            for (Post post : posts) {
                if (postNum > DEFAULT_MAX_POSTS) {
                    break;
                }

                postsContent.append("--- Document ").append(postNum).append(": ")
                        .append(post.getPostName()).append(" ---\n");

                // Get content in order of preference
                String content = post.getPostContentUsingMarkdown();
                if (content == null || content.trim().isEmpty()) {
                    content = post.getPostSimplifiedContent();
                }
                if (content == null || content.trim().isEmpty()) {
                    content = post.getPostContent();
                }

                if (content != null && !content.trim().isEmpty()) {
                    // Truncate to avoid token limits
                    if (content.length() > INTEGRATION_CONTENT_LIMIT) {
                        content = content.substring(0, INTEGRATION_CONTENT_LIMIT) + "...(truncated)";
                    }
                    postsContent.append(content).append("\n\n");
                }

                // Add source
                if (post.getPostUrl() != null && !post.getPostUrl().trim().isEmpty()) {
                    postsContent.append("Source: ").append(post.getPostUrl()).append("\n\n");
                }

                postNum++;
            }

            // Use AI to integrate the information
            String systemPrompt = "You are a content integration assistant. You will be given several documents " +
                    "and a user question. Your task is to extract and organize the most relevant information from " +
                    "these documents that helps answer the question. Present the information in a clear, structured way. " +
                    "Do not make up information - only use what's provided in the documents.";

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> userMessage = new HashMap<>();
            userMessage.put("role", "user");
            userMessage.put("content", String.format(
                    "Question: %s\n\n%s\n\nPlease extract and organize the relevant information to help answer this question:",
                    userQuestion, postsContent.toString()
            ));
            messages.add(userMessage);

            String integratedResponse = openRouterService.sendChatCompletion(ApiKey, model, systemPrompt, messages);
            
            if (integratedResponse.startsWith("Error:")) {
                log.warn("AI integration failed, using direct content: {}", integratedResponse);
                // Fallback to direct content
                return "[Relevant Information from Knowledge Base]\n\n" + postsContent.toString();
            }

            log.info("Successfully integrated content using AI");
            return "[Relevant Information from Knowledge Base]\n\n" + integratedResponse;

        } catch (Exception e) {
            log.error("Error integrating content with AI", e);
            // Fallback to basic integration
            return integratePostContents(posts);
        }
    }
}
