package top.mryan2005.sspubot.sspubotbackend.Pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "bots")
public class Bot {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String avatarUrl;

    @Column(columnDefinition = "VARCHAR(MAX)", nullable = false)
    private String systemPrompt;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String selectedModel;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String apiKey;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String baseUrl;

    @Column(nullable = false)
    private Boolean isActive;

    @Column(nullable = false)
    private String createdAt;

    @Column(nullable = false)
    private String updatedAt;

    @Column
    private Double temperature;

    @Column
    private Integer topK;

    @Column(nullable = false)
    private Boolean isDefault = false;
}
