package top.mryan2005.sspubot.sspubotbackend.Pojo;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatLogId {
    private static final long serialVersionUID = 1L;
    private Long userId;
    private Long botId;
    private String datetime;
}
