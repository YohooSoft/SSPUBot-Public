package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;

import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    List<Post> findByPostSource(String postSource);

    List<Post> findTop3ByPostSource(String postSource);
    
    // More efficient database-level search for posts containing keywords
    // Using native query due to @Lob annotation on postWords column
    // Searches postWords, postName, and postContentUsingMarkdown columns for better keyword matching
    @Query(value = "SELECT * FROM posts WHERE LOWER(postWords) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(postName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(postContentUsingMarkdown) LIKE LOWER(CONCAT('%', :keyword, '%'))", nativeQuery = true)
    List<Post> findByPostWordsContaining(@Param("keyword") String keyword);
}
