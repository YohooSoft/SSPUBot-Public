package top.mryan2005.sspubot.sspubotbackend.Pojo.Dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * Data Transfer Object for Bot
 * Used for API requests and responses to separate presentation layer from persistence layer
 */
@Data
public class BotDto {
    
    private Long id;

    @NotBlank(message = "Bot name is required")
    private String name;

    private String description;

    private String avatarUrl;

    @NotBlank(message = "System prompt is required")
    private String systemPrompt;

    private String selectedModel;

    private String apiKey;

    private String baseUrl;

    @NotNull(message = "Active status is required")
    private Boolean isActive;

    private String createdAt;

    private String updatedAt;

    /**
     * Temperature parameter for AI model (0.0 - 2.0)
     * Lower values make output more focused and deterministic
     * Higher values make output more random and creative
     */
    @DecimalMin(value = "0.0", message = "Temperature must be between 0.0 and 2.0")
    @DecimalMax(value = "2.0", message = "Temperature must be between 0.0 and 2.0")
    private Double temperature;

    /**
     * Top-K parameter for AI model (1 - 100)
     * Limits the model to consider only the top K most likely tokens
     */
    @Min(value = 1, message = "Top-K must be between 1 and 100")
    @Max(value = 100, message = "Top-K must be between 1 and 100")
    private Integer topK;

    /**
     * Whether this bot is the default bot shown in /chat
     */
    private Boolean isDefault;

    /**
     * Convert Bot entity to BotDto
     */
    public static BotDto fromEntity(top.mryan2005.sspubot.sspubotbackend.Pojo.Bot bot) {
        if (bot == null) {
            return null;
        }
        
        BotDto dto = new BotDto();
        dto.setId(bot.getId());
        dto.setName(bot.getName());
        dto.setDescription(bot.getDescription());
        dto.setAvatarUrl(bot.getAvatarUrl());
        dto.setSystemPrompt(bot.getSystemPrompt());
        dto.setSelectedModel(bot.getSelectedModel());
        dto.setApiKey(bot.getApiKey());
        dto.setBaseUrl(bot.getBaseUrl());
        dto.setIsActive(bot.getIsActive());
        dto.setCreatedAt(bot.getCreatedAt());
        dto.setUpdatedAt(bot.getUpdatedAt());
        dto.setTemperature(bot.getTemperature());
        dto.setTopK(bot.getTopK());
        dto.setIsDefault(bot.getIsDefault());
        
        return dto;
    }

    /**
     * Convert BotDto to Bot entity
     */
    public top.mryan2005.sspubot.sspubotbackend.Pojo.Bot toEntity() {
        top.mryan2005.sspubot.sspubotbackend.Pojo.Bot bot = new top.mryan2005.sspubot.sspubotbackend.Pojo.Bot();
        bot.setId(this.id);
        bot.setName(this.name);
        bot.setDescription(this.description);
        bot.setAvatarUrl(this.avatarUrl);
        bot.setSystemPrompt(this.systemPrompt);
        bot.setSelectedModel(this.selectedModel);
        bot.setApiKey(this.apiKey);
        bot.setBaseUrl(this.baseUrl);
        bot.setIsActive(this.isActive);
        bot.setCreatedAt(this.createdAt);
        bot.setUpdatedAt(this.updatedAt);
        bot.setTemperature(this.temperature);
        bot.setTopK(this.topK);
        bot.setIsDefault(this.isDefault != null ? this.isDefault : false);
        
        return bot;
    }

    /**
     * Update entity from DTO (for PATCH/PUT operations)
     */
    public void updateEntity(top.mryan2005.sspubot.sspubotbackend.Pojo.Bot bot) {
        if (bot == null) {
            return;
        }
        
        if (this.name != null) bot.setName(this.name);
        if (this.description != null) bot.setDescription(this.description);
        if (this.avatarUrl != null) bot.setAvatarUrl(this.avatarUrl);
        if (this.systemPrompt != null) bot.setSystemPrompt(this.systemPrompt);
        if (this.selectedModel != null) bot.setSelectedModel(this.selectedModel);
        if (this.apiKey != null) bot.setApiKey(this.apiKey);
        if (this.baseUrl != null) bot.setBaseUrl(this.baseUrl);
        if (this.isActive != null) bot.setIsActive(this.isActive);
        if (this.temperature != null) bot.setTemperature(this.temperature);
        if (this.topK != null) bot.setTopK(this.topK);
        if (this.isDefault != null) bot.setIsDefault(this.isDefault);
    }
}
