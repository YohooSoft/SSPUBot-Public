package top.mryan2005.sspubot.sspubotbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;
import top.mryan2005.sspubot.sspubotbackend.Repository.PostRepository;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.PostList.PostLink;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.PostList.PostList;

import java.util.Comparator;
import java.util.List;

@Service
public class PostService {

    @Autowired
    PostRepository postRepository;

    public List<Post> listPosts() {
        return postRepository.findAll().stream()
                .sorted(Comparator.comparing(Post::getId))
                .toList();
    }

    public List<PostList> getIndexList() {
        List<PostList> postLists = new java.util.ArrayList<>();
        List<String> sources = postRepository.findAll()
                .stream()
                .map(Post::getPostSource)
                .distinct()
                .toList();
        long idCounter = 1;
        for(String source : sources) {
            PostList postList = new PostList();
            postList.id = idCounter;
            postList.title = source ;
            List<Post> posts = postRepository.findTop3ByPostSource(source);
            postList.items = new java.util.ArrayList<>();
            for (Post post : posts) {
                PostLink link = new PostLink();
                link.title = post.getPostName();
                link.url = post.getPostUrl();
                postList.items.add(link);
            }
            postLists.add(postList);
            idCounter++;
        }
        return postLists;
    }
}
