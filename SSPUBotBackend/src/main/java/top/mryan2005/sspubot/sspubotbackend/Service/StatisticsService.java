package top.mryan2005.sspubot.sspubotbackend.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Post;
import top.mryan2005.sspubot.sspubotbackend.Repository.PostRepository;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class StatisticsService {

    @Autowired
    PostRepository postRepository;

    private static final int DATE_STRING_LENGTH = 10; // YYYY-MM-DD format

    /**
     * Get file count grouped by source
     */
    public Map<String, Long> getFileCountBySource() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .filter(post -> post.getPostSource() != null && !post.getPostSource().isBlank())
                .collect(Collectors.groupingBy(
                        Post::getPostSource,
                        Collectors.counting()
                ));
    }

    /**
     * Get daily file count (grouped by date)
     */
    public Map<String, Long> getDailyFileCount() {
        List<Post> posts = postRepository.findAll();
        return posts.stream()
                .filter(post -> post.getPostReleaseTime() != null && !post.getPostReleaseTime().isBlank())
                .collect(Collectors.groupingBy(
                        post -> {
                            // Extract date part from postReleaseTime (assuming format like "YYYY-MM-DD HH:MM:SS" or "YYYY-MM-DD")
                            String dateTime = post.getPostReleaseTime();
                            if (dateTime.contains(" ")) {
                                return dateTime.split(" ")[0]; // Get date part
                            }
                            return dateTime.substring(0, Math.min(DATE_STRING_LENGTH, dateTime.length())); // Get first 10 chars (YYYY-MM-DD)
                        },
                        TreeMap::new,
                        Collectors.counting()
                ));
    }

    /**
     * Get word cloud data from postWords field
     * Excludes symbols and counts word frequency
     */
    public Map<String, Integer> getWordCloudData() {
        List<Post> posts = postRepository.findAll();
        Map<String, Integer> wordFrequency = new HashMap<>();

        for (Post post : posts) {
            if (post.getPostWords() != null && !post.getPostWords().isBlank()) {
                String postWords = post.getPostWords();
                
                // Split by common delimiters and process words
                String[] words = postWords.split("[\\s,;，；、。！？!?.]+");
                
                for (String word : words) {
                    // Remove symbols and numbers, keep only Chinese characters and letters
                    String cleanWord = word.replaceAll("[^\\p{L}]+", "").trim();
                    
                    // Skip empty words, single characters, and very short words
                    if (!cleanWord.isEmpty() && cleanWord.length() >= 2) {
                        wordFrequency.merge(cleanWord, 1, Integer::sum);
                    }
                }
            }
        }

        // Sort by frequency and return top words
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(100) // Limit to top 100 words for performance
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (e1, e2) -> e1,
                        LinkedHashMap::new
                ));
    }
}
