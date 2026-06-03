package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(name = "chat_logs")
@Data
public class ChatLog {

    @EmbeddedId
    private ChatLogId id;

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", foreignKey = @ForeignKey(name = "fk_memory_user"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @MapsId("botId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bot_id", foreignKey = @ForeignKey(name = "fk_memory_bot"))
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Bot bot;

    @Column(columnDefinition = "VARCHAR(MAX)")
    private String content;
}