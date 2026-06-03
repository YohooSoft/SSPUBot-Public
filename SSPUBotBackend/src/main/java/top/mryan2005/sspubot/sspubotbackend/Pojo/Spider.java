package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "spiders")
public class Spider {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Lob
    @Column
    private String description;

    @Column(nullable = false)
    private String spiderClass;

    @Lob
    @Column
    private String startUrls;

    @Lob
    @Column
    private String allowedDomains;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column
    private String status = "idle"; // idle, running, stopped, error

    @Column
    private Integer progress = 0;

    @Lob
    @Column
    private String lastError;

    @Column
    private LocalDateTime lastRunTime;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
