package top.mryan2005.sspubot.sspubotbackend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.PostList.PostList;
import top.mryan2005.sspubot.sspubotbackend.RequestModel.PostSearchResponse;
import top.mryan2005.sspubot.sspubotbackend.Service.PostService;

import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {

    @Autowired
    PostService postService;

    @GetMapping("/list")
    public List<Post> listPosts() {
        return postService.listPosts();
    }

    @GetMapping("/index")
    public List<PostList> getPostList() {
        return postService.getIndexList();
    }

    @GetMapping("/list1")
    public List<Post> getPostsBySource(@RequestParam String source) {
        // This is a placeholder implementation. Replace with actual logic to fetch posts by gridId.
        return postService.listPosts().stream()
                .filter(post -> post.getPostSource().equals(source))
                .toList();
    }

    @GetMapping("/search/title")
    public PostSearchResponse searchPostsByTitle(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        List<Post> allFiltered = postService.listPosts().stream()
                .filter(post -> {
                    // Filter by title
                    if (title != null && !title.isBlank() 
                            && !post.getPostName().toLowerCase().contains(title.toLowerCase())) {
                        return false;
                    }
                    
                    // Filter by source
                    if (source != null && !source.isBlank() 
                            && !post.getPostSource().equals(source)) {
                        return false;
                    }
                    
                    // Filter by date range
                    if (startDate != null && !startDate.isBlank() 
                            && post.getPostReleaseTime() != null 
                            && post.getPostReleaseTime().compareTo(startDate) < 0) {
                        return false;
                    }
                    
                    if (endDate != null && !endDate.isBlank() 
                            && post.getPostReleaseTime() != null 
                            && post.getPostReleaseTime().compareTo(endDate) > 0) {
                        return false;
                    }
                    
                    return true;
                })
                .toList();

        long total = allFiltered.size();

        int from = page * size;
        List<Post> pagedList;

        if (from >= total) {
            pagedList = List.of();
        } else {
            int to = Math.min(from + size, (int) total);
            pagedList = allFiltered.subList(from, to);
        }

        return new PostSearchResponse(pagedList, total);
    }

    @GetMapping("/search/source")
    public PostSearchResponse searchPostsBySource(
            @RequestParam(required = false) String source,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        List<Post> allFiltered = postService.listPosts().stream()
                .filter(post -> source == null || source.isBlank()
                        || post.getPostSource().toLowerCase().contains(source.toLowerCase()))
                .toList();
        long total = allFiltered.size();
        int from = page * size;
        List<Post> pagedList;
        if (from >= total) {
            pagedList = List.of();
        } else {
            int to = Math.min(from + size, (int) total);
            pagedList = allFiltered.subList(from, to);
        }
        return new PostSearchResponse(pagedList, total);
    }

    @GetMapping("/search/keyword")
    public PostSearchResponse searchPostsByKeyword(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String source,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        // 1. 处理关键词：去除首尾空格，转小写（实现忽略大小写搜索）
        String q = (keyword == null) ? "" : keyword.trim().toLowerCase();

        // 2. Stream 流式过滤
        List<Post> allFiltered = postService.listPosts().stream()
                .filter(post -> {
                    // 如果关键词为空，返回所有（或者返回空，看需求）
                    if (!q.isEmpty()) {
                        // 辅助方法：安全检查是否包含
                        // 搜索范围：标题、来源、简化内容、Markdown内容、关键词
                        boolean keywordMatch = containsIgnoreCase(post.getPostName(), q) ||
                                containsIgnoreCase(post.getPostSource(), q) ||
                                containsIgnoreCase(post.getPostSimplifiedContent(), q) ||
                                containsIgnoreCase(post.getPostContentUsingMarkdown(), q) ||
                                containsIgnoreCase(post.getPostWords(), q);
                        if (!keywordMatch) return false;
                    }
                    
                    // Filter by source
                    if (source != null && !source.isBlank() 
                            && !post.getPostSource().equals(source)) {
                        return false;
                    }
                    
                    // Filter by date range
                    if (startDate != null && !startDate.isBlank() 
                            && post.getPostReleaseTime() != null 
                            && post.getPostReleaseTime().compareTo(startDate) < 0) {
                        return false;
                    }
                    
                    if (endDate != null && !endDate.isBlank() 
                            && post.getPostReleaseTime() != null 
                            && post.getPostReleaseTime().compareTo(endDate) > 0) {
                        return false;
                    }
                    
                    return true;
                })
                .toList();

        // 3. 内存分页计算 (Total & SubList)
        long total = allFiltered.size();
        int from = page * size;
        List<Post> pagedList;

        if (from >= total) {
            pagedList = List.of();
        } else {
            int to = Math.min(from + size, (int) total);
            pagedList = allFiltered.subList(from, to);
        }

        return new PostSearchResponse(pagedList, total);
    }

    // 辅助函数：防止 Null 指针异常，统一转小写比较
    private boolean containsIgnoreCase(String source, String target) {
        return source != null && source.toLowerCase().contains(target);
    }

    /**
     * Get all distinct post sources
     */
    @GetMapping("/sources")
    public List<String> getDistinctPostSources() {
        return postService.listPosts().stream()
                .map(Post::getPostSource)
                .filter(source -> source != null && !source.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

}
