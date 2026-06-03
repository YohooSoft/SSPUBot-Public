package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "posts")
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "postName")
    private String postName;

    @Column(name = "postReleaseTime")
    private String postReleaseTime;

    @Column(name = "postSource")
    private String postSource;

    // 修改点：添加 @Lob 或指定 columnDefinition
    @Lob
    @Column(name = "postContent", columnDefinition = "NVARCHAR(MAX)")
    private String postContent;

    @Column(name = "postUrl")
    private String postUrl;

    // 如果 postFiles 也是长 JSON 字符串，建议也改大
    @Column(name = "postFiles", columnDefinition = "NVARCHAR(MAX)")
    private String postFiles;

    @Lob
    @Column(name = "postContentUsingMarkdown", columnDefinition = "NVARCHAR(MAX)")
    private String postContentUsingMarkdown;

    @Lob
    @Column(name = "postSimplifiedContent", columnDefinition = "NVARCHAR(MAX)")
    private String postSimplifiedContent;

    @Lob
    @Column(name = "postWords", columnDefinition = "NVARCHAR(MAX)")
    private String postWords;
}