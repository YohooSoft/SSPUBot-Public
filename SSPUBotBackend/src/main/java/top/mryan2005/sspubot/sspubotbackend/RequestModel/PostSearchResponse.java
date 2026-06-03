package top.mryan2005.sspubot.sspubotbackend.RequestModel;

import lombok.Data;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;

import java.util.List;

@Data
public class PostSearchResponse {
    private List<Post> posts;
    private long total;

    // 构造函数、Getter、Setter
    public PostSearchResponse(List<Post> posts, long total) {
        this.posts = posts;
        this.total = total;
    }
}
