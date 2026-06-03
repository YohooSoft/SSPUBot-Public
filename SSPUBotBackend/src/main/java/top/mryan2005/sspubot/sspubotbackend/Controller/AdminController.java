package top.mryan2005.sspubot.sspubotbackend.Controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Bot;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Dto.BotDto;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Spider;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Synonym;
import top.mryan2005.sspubot.sspubotbackend.Pojo.User;
import top.mryan2005.sspubot.sspubotbackend.Repository.UserRepository;
import top.mryan2005.sspubot.sspubotbackend.Service.BotService;
import top.mryan2005.sspubot.sspubotbackend.Service.SpiderService;
import top.mryan2005.sspubot.sspubotbackend.Service.SpiderExecutionService;
import top.mryan2005.sspubot.sspubotbackend.Service.SynonymService;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:8080"})
public class AdminController {

    // User status constants
    private static final int STATUS_INACTIVE = 0;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_BANNED = 2;
    private static final int STATUS_MUTED = 3;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BotService botService;

    @Autowired
    private SpiderService spiderService;

    @Autowired
    private SpiderExecutionService spiderExecutionService;

    @Autowired
    private SynonymService synonymService;

    /**
     * Check if current user is admin
     */
    private boolean isAdmin(Authentication authentication) {
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ADMIN") || auth.getAuthority().equals("ROLE_ADMIN"));
    }

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            List<User> users = userRepository.findAll();
            // Remove sensitive information
            users.forEach(user -> {
                user.setPassword(null);
                user.setSalt(null);
            });
            
            log.info("Admin {} fetched all users", authentication.getName());
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            log.error("Error fetching users", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取用户列表失败: " + e.getMessage());
        }
    }

    /**
     * Ban a user (admin only)
     */
    @PutMapping("/users/{userId}/ban")
    public ResponseEntity<?> banUser(@PathVariable Long userId, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            // Get current user
            String currentUsername = authentication.getName();
            Optional<User> currentUserOpt = userRepository.findByUsername(currentUsername);
            
            if (currentUserOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body("当前用户不存在");
            }

            // Check if trying to ban self
            if (currentUserOpt.get().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("不能封禁自己");
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("用户不存在");
            }

            User user = userOpt.get();
            user.setStatus(STATUS_BANNED); // 2 = banned
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Admin {} banned user {}", currentUsername, user.getUsername());
            return ResponseEntity.ok("用户已被封禁");
        } catch (Exception e) {
            log.error("Error banning user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("封禁用户失败: " + e.getMessage());
        }
    }

    /**
     * Unban a user (admin only)
     */
    @PutMapping("/users/{userId}/unban")
    public ResponseEntity<?> unbanUser(@PathVariable Long userId, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<User> userOpt = userRepository.findById(userId);
            if (userOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("用户不存在");
            }

            User user = userOpt.get();
            user.setStatus(STATUS_ACTIVE); // 1 = active
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);

            log.info("Admin {} unbanned user {}", authentication.getName(), user.getUsername());
            return ResponseEntity.ok("用户已解除封禁");
        } catch (Exception e) {
            log.error("Error unbanning user", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("解除封禁失败: " + e.getMessage());
        }
    }

    /**
     * Get all bots (admin only)
     */
    @GetMapping("/bots")
    public ResponseEntity<?> getAllBots(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            List<Bot> bots = botService.findAll();
            // Convert entities to DTOs
            List<BotDto> botDtos = bots.stream()
                    .map(BotDto::fromEntity)
                    .collect(Collectors.toList());
            
            log.info("Admin {} fetched all bots", authentication.getName());
            return ResponseEntity.ok(botDtos);
        } catch (Exception e) {
            log.error("Error fetching bots", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取机器人列表失败: " + e.getMessage());
        }
    }

    /**
     * Create a bot (admin only)
     * Receives BotDto and transfers data to Bot entity
     */
    @PostMapping("/bots")
    public ResponseEntity<?> createBot(@RequestBody BotDto botDto, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            if (botDto.getName() == null || botDto.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("机器人名称不能为空");
            }
            if (botDto.getSystemPrompt() == null || botDto.getSystemPrompt().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("系统提示词不能为空");
            }

            // Transfer data from DTO to entity
            Bot bot = botDto.toEntity();
            
            String now = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            bot.setCreatedAt(now);
            bot.setUpdatedAt(now);

            if (bot.getIsActive() == null) {
                bot.setIsActive(true);
            }

            Bot createdBot = botService.save(bot);
            
            // Convert entity back to DTO for response
            BotDto responseDto = BotDto.fromEntity(createdBot);
            
            log.info("Admin {} created bot {}", authentication.getName(), createdBot.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(responseDto);
        } catch (Exception e) {
            log.error("Error creating bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建机器人失败: " + e.getMessage());
        }
    }

    /**
     * Update a bot (admin only)
     * Receives BotDto and transfers data to Bot entity
     */
    @PutMapping("/bots/{id}")
    public ResponseEntity<?> updateBot(@PathVariable Long id, @RequestBody BotDto botDto, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Bot existingBot = botService.findById(id);
            if (existingBot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("机器人不存在");
            }

            // Transfer data from DTO to existing entity
            botDto.updateEntity(existingBot);

            existingBot.setUpdatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            Bot updatedBot = botService.save(existingBot);
            
            // Convert entity back to DTO for response
            BotDto responseDto = BotDto.fromEntity(updatedBot);
            
            log.info("Admin {} updated bot {}", authentication.getName(), id);
            return ResponseEntity.ok(responseDto);
        } catch (Exception e) {
            log.error("Error updating bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新机器人失败: " + e.getMessage());
        }
    }

    /**
     * Delete a bot (admin only)
     */
    @DeleteMapping("/bots/{id}")
    public ResponseEntity<?> deleteBot(@PathVariable Long id, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Bot bot = botService.findById(id);
            if (bot == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("机器人不存在");
            }

            botService.deleteById(id);
            log.info("Admin {} deleted bot {}", authentication.getName(), id);
            return ResponseEntity.ok("机器人删除成功");
        } catch (Exception e) {
            log.error("Error deleting bot", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除机器人失败: " + e.getMessage());
        }
    }

    /**
     * Get all spiders (admin only)
     * Fetches spider list from Python spider_api.py
     */
    @GetMapping("/spiders")
    public ResponseEntity<?> getAllSpiders(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            // Check if Python spider API is available
            boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
            
            if (apiAvailable) {
                // Fetch spiders from Python API
                try {
                    String url = "http://localhost:5000/api/spiders";
                    ResponseEntity<List> response = new org.springframework.web.client.RestTemplate()
                            .exchange(url, org.springframework.http.HttpMethod.GET, null, List.class);
                    
                    if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                        log.info("Admin {} fetched all spiders from Python API", authentication.getName());
                        return ResponseEntity.ok(response.getBody());
                    }
                } catch (Exception apiError) {
                    log.warn("Failed to fetch from Python API, falling back to database: {}", apiError.getMessage());
                }
            }
            
            // Fallback to database if API is not available
            List<Spider> spiders = spiderService.findAll();
            log.info("Admin {} fetched all spiders from database (API unavailable)", authentication.getName());
            
            Map<String, Object> fallbackResponse = new HashMap<>();
            fallbackResponse.put("spiders", spiders);
            fallbackResponse.put("source", "database");
            fallbackResponse.put("warning", "Python Spider API is not available. Showing database records only.");
            
            return ResponseEntity.ok(fallbackResponse);
        } catch (Exception e) {
            log.error("Error fetching spiders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取爬虫列表失败: " + e.getMessage());
        }
    }

    /**
     * Create a spider (admin only)
     */
    @PostMapping("/spiders")
    public ResponseEntity<?> createSpider(@RequestBody Spider spider, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            if (spider.getName() == null || spider.getName().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("爬虫名称不能为空");
            }
            if (spider.getSpiderClass() == null || spider.getSpiderClass().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("爬虫类名不能为空");
            }

            spider.setCreatedAt(LocalDateTime.now());
            spider.setUpdatedAt(LocalDateTime.now());
            
            if (spider.getIsActive() == null) {
                spider.setIsActive(true);
            }
            if (spider.getStatus() == null) {
                spider.setStatus("idle");
            }
            if (spider.getProgress() == null) {
                spider.setProgress(0);
            }

            Spider createdSpider = spiderService.save(spider);
            log.info("Admin {} created spider {}", authentication.getName(), createdSpider.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSpider);
        } catch (Exception e) {
            log.error("Error creating spider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建爬虫失败: " + e.getMessage());
        }
    }

    /**
     * Update a spider (admin only)
     */
    @PutMapping("/spiders/{id}")
    public ResponseEntity<?> updateSpider(@PathVariable Long id, @RequestBody Spider spider, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<Spider> existingSpiderOpt = spiderService.findById(id);
            if (existingSpiderOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("爬虫不存在");
            }

            Spider existingSpider = existingSpiderOpt.get();
            
            if (spider.getName() != null) {
                existingSpider.setName(spider.getName());
            }
            if (spider.getDescription() != null) {
                existingSpider.setDescription(spider.getDescription());
            }
            if (spider.getSpiderClass() != null) {
                existingSpider.setSpiderClass(spider.getSpiderClass());
            }
            if (spider.getStartUrls() != null) {
                existingSpider.setStartUrls(spider.getStartUrls());
            }
            if (spider.getAllowedDomains() != null) {
                existingSpider.setAllowedDomains(spider.getAllowedDomains());
            }
            if (spider.getIsActive() != null) {
                existingSpider.setIsActive(spider.getIsActive());
            }

            existingSpider.setUpdatedAt(LocalDateTime.now());

            Spider updatedSpider = spiderService.save(existingSpider);
            log.info("Admin {} updated spider {}", authentication.getName(), id);
            return ResponseEntity.ok(updatedSpider);
        } catch (Exception e) {
            log.error("Error updating spider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新爬虫失败: " + e.getMessage());
        }
    }

    /**
     * Delete a spider (admin only)
     */
    @DeleteMapping("/spiders/{id}")
    public ResponseEntity<?> deleteSpider(@PathVariable Long id, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<Spider> spider = spiderService.findById(id);
            if (spider.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("爬虫不存在");
            }

            spiderService.deleteById(id);
            log.info("Admin {} deleted spider {}", authentication.getName(), id);
            return ResponseEntity.ok("爬虫删除成功");
        } catch (Exception e) {
            log.error("Error deleting spider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除爬虫失败: " + e.getMessage());
        }
    }

    /**
     * Start a spider (admin only)
     * Accepts either numeric ID or spider name as identifier
     */
    @PostMapping("/spiders/{identifier}/start")
    public ResponseEntity<?> startSpider(@PathVariable String identifier, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            // Try to parse as Long ID first, otherwise treat as spider name
            Optional<Spider> spiderOpt;
            try {
                Long id = Long.parseLong(identifier);
                spiderOpt = spiderService.findById(id);
            } catch (NumberFormatException e) {
                // identifier is a spider name, not a numeric ID
                spiderOpt = spiderService.findByName(identifier);
            }
            
            if (spiderOpt.isEmpty()) {
                // If not found in database, try to start via Python API directly
                boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
                
                if (apiAvailable) {
                    Map<String, Object> result = spiderExecutionService.startSpider(identifier);
                    
                    if (result.containsKey("error")) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("启动爬虫失败: " + result.get("error"));
                    }
                    
                    log.info("Admin {} started spider {} directly via Python API (not in database)", authentication.getName(), identifier);
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("爬虫不存在且 Python API 不可用");
                }
            }

            Spider spider = spiderOpt.get();
            
            // Check if spider is already running
            if ("running".equals(spider.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("爬虫正在运行中");
            }

            // Try to start the spider via Python API
            boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
            
            if (apiAvailable) {
                // Call Python API to start spider
                Map<String, Object> result = spiderExecutionService.startSpider(spider.getName());
                
                if (result.containsKey("error")) {
                    spiderService.updateSpiderError(spider.getId(), result.get("error").toString());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("启动爬虫失败: " + result.get("error"));
                }
                
                // Update spider status to running
                spiderService.updateSpiderStatus(spider.getId(), "running", 0);
                log.info("Admin {} started spider {} via Python API", authentication.getName(), spider.getName());
                
                return ResponseEntity.ok(result);
            } else {
                // Python API not available, just update status
                spiderService.updateSpiderStatus(spider.getId(), "running", 0);
                log.warn("Python API not available. Spider {} status updated to running but not actually started", spider.getName());
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "爬虫已启动（注意：Python API 未运行，实际未执行）");
                response.put("spiderId", spider.getId());
                response.put("status", "running");
                response.put("warning", "Spider API service is not available");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error starting spider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("启动爬虫失败: " + e.getMessage());
        }
    }

    /**
     * Get spider progress (admin only)
     * Accepts either numeric ID or spider name as identifier
     */
    @GetMapping("/spiders/{identifier}/progress")
    public ResponseEntity<?> getSpiderProgress(@PathVariable String identifier, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            // Try to get status directly from Python API using the identifier as spider name
            boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
            
            log.info("Spider API available: {}, requesting status for: {}", apiAvailable, identifier);
            
            if (!apiAvailable) {
                log.error("Spider API is not available at http://localhost:5000");
                return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .body("Spider API服务不可用，请确保Python服务正在运行");
            }
            
            Map<String, Object> apiStatus = spiderExecutionService.getSpiderStatus(identifier);
            
            log.info("Received API status: {}", apiStatus);
            
            // Check if apiStatus is null or contains a non-null error
            if (apiStatus == null || (apiStatus.containsKey("error") && apiStatus.get("error") != null)) {
                String errorMsg = "Unknown error";
                if (apiStatus != null && apiStatus.get("error") != null) {
                    errorMsg = apiStatus.get("error").toString();
                }
                log.error("Failed to get spider status: {}", errorMsg);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("爬虫不存在或获取状态失败: " + errorMsg);
            }
            
            // Return the status directly from API
            Map<String, Object> result = new HashMap<>();
            result.put("name", apiStatus.getOrDefault("name", identifier));
            result.put("status", apiStatus.getOrDefault("status", "unknown"));
            result.put("runtimeSeconds", apiStatus.getOrDefault("runtimeSeconds", 0));
            result.put("lastRunTime", apiStatus.get("last_run"));
            result.put("lastError", apiStatus.get("error"));
            result.put("startedAt", apiStatus.get("started_at"));
            result.put("apiAvailable", true);
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error getting spider progress for {}: {}", identifier, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取爬虫运行信息失败: " + e.getMessage());
        }
    }

    /**
     * Stop a spider (admin only)
     * Accepts either numeric ID or spider name as identifier
     */
    @PostMapping("/spiders/{identifier}/stop")
    public ResponseEntity<?> stopSpider(@PathVariable String identifier, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            // Try to parse as Long ID first, otherwise treat as spider name
            Optional<Spider> spiderOpt;
            try {
                Long id = Long.parseLong(identifier);
                spiderOpt = spiderService.findById(id);
            } catch (NumberFormatException e) {
                // identifier is a spider name, not a numeric ID
                spiderOpt = spiderService.findByName(identifier);
            }
            
            if (spiderOpt.isEmpty()) {
                // If not found in database, try to stop via Python API directly
                boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
                
                if (apiAvailable) {
                    Map<String, Object> result = spiderExecutionService.stopSpider(identifier);
                    
                    if (result.containsKey("error")) {
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body("停止爬虫失败: " + result.get("error"));
                    }
                    
                    log.info("Admin {} stopped spider {} directly via Python API (not in database)", authentication.getName(), identifier);
                    return ResponseEntity.ok(result);
                } else {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND)
                            .body("爬虫不存在且 Python API 不可用");
                }
            }

            Spider spider = spiderOpt.get();
            Long id = spider.getId();
            
            // Check if spider is running
            if (!"running".equals(spider.getStatus())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("爬虫未在运行中");
            }

            // Try to stop the spider via Python API
            boolean apiAvailable = spiderExecutionService.isSpiderApiAvailable();
            
            if (apiAvailable) {
                Map<String, Object> result = spiderExecutionService.stopSpider(spider.getName());
                
                if (result.containsKey("error")) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("停止爬虫失败: " + result.get("error"));
                }
                
                // Update spider status to stopped
                spiderService.updateSpiderStatus(id, "stopped", spider.getProgress());
                log.info("Admin {} stopped spider {} via Python API", authentication.getName(), spider.getName());
                
                return ResponseEntity.ok(result);
            } else {
                // Python API not available, just update status
                spiderService.updateSpiderStatus(id, "stopped", spider.getProgress());
                log.warn("Python API not available. Spider {} status updated to stopped", spider.getName());
                
                Map<String, Object> response = new HashMap<>();
                response.put("message", "爬虫已停止（注意：Python API 未运行）");
                response.put("spiderId", id);
                response.put("status", "stopped");
                response.put("warning", "Spider API service is not available");
                
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            log.error("Error stopping spider", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("停止爬虫失败: " + e.getMessage());
        }
    }

    // ============================================================
    // Synonym Management
    // ============================================================

    /**
     * Get all synonyms (admin only)
     */
    @GetMapping("/synonyms")
    public ResponseEntity<?> getAllSynonyms(Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            List<Synonym> synonyms = synonymService.findAll();
            log.info("Admin {} fetched all synonyms", authentication.getName());
            return ResponseEntity.ok(synonyms);
        } catch (Exception e) {
            log.error("Error fetching synonyms", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取同义词列表失败: " + e.getMessage());
        }
    }

    /**
     * Get synonym by ID (admin only)
     */
    @GetMapping("/synonyms/{id}")
    public ResponseEntity<?> getSynonymById(@PathVariable Long id, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<Synonym> synonymOpt = synonymService.findById(id);
            if (synonymOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("同义词不存在");
            }

            return ResponseEntity.ok(synonymOpt.get());
        } catch (Exception e) {
            log.error("Error fetching synonym by ID: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取同义词失败: " + e.getMessage());
        }
    }

    /**
     * Create a synonym (admin only)
     */
    @PostMapping("/synonyms")
    public ResponseEntity<?> createSynonym(@RequestBody Synonym synonym, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            if (synonym.getWord() == null || synonym.getWord().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("主词不能为空");
            }
            if (synonym.getSynonyms() == null || synonym.getSynonyms().trim().isEmpty()) {
                return ResponseEntity.badRequest().body("同义词列表不能为空");
            }

            if (synonymService.existsByWord(synonym.getWord())) {
                return ResponseEntity.badRequest().body("该主词已存在");
            }

            Synonym createdSynonym = synonymService.save(synonym);
            log.info("Admin {} created synonym: {}", authentication.getName(), synonym.getWord());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdSynonym);
        } catch (Exception e) {
            log.error("Error creating synonym", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("创建同义词失败: " + e.getMessage());
        }
    }

    /**
     * Update a synonym (admin only)
     */
    @PutMapping("/synonyms/{id}")
    public ResponseEntity<?> updateSynonym(@PathVariable Long id, @RequestBody Synonym synonym, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<Synonym> existingSynonymOpt = synonymService.findById(id);
            if (existingSynonymOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("同义词不存在");
            }

            Synonym existingSynonym = existingSynonymOpt.get();

            if (synonym.getWord() != null) {
                // Check if new word conflicts with existing ones (excluding current record)
                if (!synonym.getWord().equals(existingSynonym.getWord()) && 
                    synonymService.existsByWord(synonym.getWord())) {
                    return ResponseEntity.badRequest().body("该主词已被其他记录使用");
                }
                existingSynonym.setWord(synonym.getWord());
            }
            if (synonym.getSynonyms() != null) {
                existingSynonym.setSynonyms(synonym.getSynonyms());
            }
            if (synonym.getCategory() != null) {
                existingSynonym.setCategory(synonym.getCategory());
            }
            if (synonym.getDescription() != null) {
                existingSynonym.setDescription(synonym.getDescription());
            }

            Synonym updatedSynonym = synonymService.save(existingSynonym);
            log.info("Admin {} updated synonym: {}", authentication.getName(), id);
            return ResponseEntity.ok(updatedSynonym);
        } catch (Exception e) {
            log.error("Error updating synonym: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("更新同义词失败: " + e.getMessage());
        }
    }

    /**
     * Delete a synonym (admin only)
     */
    @DeleteMapping("/synonyms/{id}")
    public ResponseEntity<?> deleteSynonym(@PathVariable Long id, Authentication authentication) {
        try {
            if (!isAdmin(authentication)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("需要管理员权限");
            }

            Optional<Synonym> synonymOpt = synonymService.findById(id);
            if (synonymOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("同义词不存在");
            }

            synonymService.deleteById(id);
            log.info("Admin {} deleted synonym: {}", authentication.getName(), id);
            return ResponseEntity.ok("同义词删除成功");
        } catch (Exception e) {
            log.error("Error deleting synonym: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除同义词失败: " + e.getMessage());
        }
    }
}
