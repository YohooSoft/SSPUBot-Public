package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "synonyms")
public class Synonym {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 主词 (Primary word)
     */
    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String word;

    /**
     * 同义词列表 (Comma-separated synonyms)
     */
    @Column(nullable = false, columnDefinition = "VARCHAR(MAX)")
    private String synonyms;

    /**
     * 分类 (Category for organizing synonyms)
     */
    @Column(columnDefinition = "VARCHAR(MAX)")
    private String category;

    /**
     * 描述 (Description)
     */
    @Column(columnDefinition = "VARCHAR(MAX)")
    private String description;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
