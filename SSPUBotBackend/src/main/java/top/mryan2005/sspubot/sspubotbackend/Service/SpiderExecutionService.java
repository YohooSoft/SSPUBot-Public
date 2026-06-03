package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpMethod;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class SpiderExecutionService {
    
    private final RestTemplate restTemplate = new RestTemplate();
    private static final String SPIDER_API_BASE_URL = "http://localhost:5000/api";

    /**
     * Start a spider by calling the Python API
     */
    public Map<String, Object> startSpider(String spiderName) {
        try {
            String url = SPIDER_API_BASE_URL + "/spiders/" + spiderName + "/start";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully started spider: {}", spiderName);
                return response.getBody();
            } else {
                log.error("Failed to start spider: {}, status: {}", spiderName, response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to start spider");
                return error;
            }
        } catch (Exception e) {
            log.error("Error starting spider {}: {}", spiderName, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * Stop a running spider
     */
    public Map<String, Object> stopSpider(String spiderName) {
        try {
            String url = SPIDER_API_BASE_URL + "/spiders/" + spiderName + "/stop";
            ResponseEntity<Map> response = restTemplate.postForEntity(url, null, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully stopped spider: {}", spiderName);
                return response.getBody();
            } else {
                log.error("Failed to stop spider: {}, status: {}", spiderName, response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to stop spider");
                return error;
            }
        } catch (Exception e) {
            log.error("Error stopping spider {}: {}", spiderName, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return error;
        }
    }

    /**
     * Get spider status and progress
     */
    public Map<String, Object> getSpiderStatus(String spiderName) {
        try {
            String url = SPIDER_API_BASE_URL + "/spiders/" + spiderName + "/status";
            log.info("Requesting spider status from: {}", url);
            
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                log.info("Successfully retrieved status for spider: {}", spiderName);
                return response.getBody();
            } else {
                log.error("Failed to get spider status: {}, status: {}", spiderName, response.getStatusCode());
                Map<String, Object> error = new HashMap<>();
                error.put("error", "Failed to get spider status - HTTP " + response.getStatusCode());
                return error;
            }
        } catch (org.springframework.web.client.ResourceAccessException e) {
            log.error("Cannot connect to Spider API at {}: {}", SPIDER_API_BASE_URL, e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "无法连接到Spider API服务，请确保Python服务正在运行");
            error.put("status", "unknown");
            return error;
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            log.error("HTTP error getting spider status {}: {} - {}", spiderName, e.getStatusCode(), e.getMessage());
            Map<String, Object> error = new HashMap<>();
            error.put("error", "Spider API返回错误: " + e.getStatusCode());
            error.put("status", "unknown");
            return error;
        } catch (Exception e) {
            log.error("Error getting spider status {}: {}", spiderName, e.getMessage(), e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            error.put("status", "unknown");
            return error;
        }
    }

    /**
     * Check if Python spider API is available
     */
    public boolean isSpiderApiAvailable() {
        try {
            String url = "http://localhost:5000/health";
            ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, null, Map.class);
            return response.getStatusCode().is2xxSuccessful();
        } catch (Exception e) {
            log.warn("Spider API is not available: {}", e.getMessage());
            return false;
        }
    }
}
