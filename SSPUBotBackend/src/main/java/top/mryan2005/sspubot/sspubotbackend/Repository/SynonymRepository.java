package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Synonym;

import java.util.List;
import java.util.Optional;

@Repository
public interface SynonymRepository extends JpaRepository<Synonym, Long> {
    
    /**
     * Find synonym by word
     */
    Optional<Synonym> findByWord(String word);
    
    /**
     * Find all synonyms by category
     */
    List<Synonym> findByCategory(String category);
    
    /**
     * Check if word exists
     */
    boolean existsByWord(String word);
}
