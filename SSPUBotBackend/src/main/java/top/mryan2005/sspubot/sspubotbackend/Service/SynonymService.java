package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Synonym;
import top.mryan2005.sspubot.sspubotbackend.Repository.SynonymRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SynonymService {

    @Autowired
    private SynonymRepository synonymRepository;

    /**
     * Get all synonyms (cached)
     */
    @Cacheable(value = "synonyms", key = "'all'")
    public List<Synonym> findAll() {
        log.debug("Loading all synonyms from database");
        return synonymRepository.findAll();
    }

    /**
     * Get synonym by ID (cached)
     */
    @Cacheable(value = "synonyms", key = "#id")
    public Optional<Synonym> findById(Long id) {
        return synonymRepository.findById(id);
    }

    /**
     * Get synonym by word (cached)
     */
    @Cacheable(value = "synonyms", key = "'word_' + #word")
    public Optional<Synonym> findByWord(String word) {
        return synonymRepository.findByWord(word);
    }

    /**
     * Get synonyms by category (cached)
     */
    @Cacheable(value = "synonyms", key = "'category_' + #category")
    public List<Synonym> findByCategory(String category) {
        return synonymRepository.findByCategory(category);
    }

    /**
     * Save or update synonym (clears cache)
     */
    @Transactional
    @CacheEvict(value = "synonyms", allEntries = true)
    public Synonym save(Synonym synonym) {
        if (synonym.getId() == null) {
            synonym.setCreatedAt(LocalDateTime.now());
        }
        synonym.setUpdatedAt(LocalDateTime.now());
        return synonymRepository.save(synonym);
    }

    /**
     * Delete synonym by ID (clears cache)
     */
    @Transactional
    @CacheEvict(value = "synonyms", allEntries = true)
    public void deleteById(Long id) {
        synonymRepository.deleteById(id);
    }

    /**
     * Check if word exists
     */
    public boolean existsByWord(String word) {
        return synonymRepository.existsByWord(word);
    }

    /**
     * Expand query with synonyms
     * Given a query string, replace words with their synonyms
     * Uses caching for better performance
     */
    @Cacheable(value = "synonymExpansion", key = "#query")
    public String expandQueryWithSynonyms(String query) {
        if (query == null || query.trim().isEmpty()) {
            return query;
        }

        List<Synonym> allSynonyms = findAll(); // This uses cached data
        String expandedQuery = query;

        for (Synonym synonym : allSynonyms) {
            String word = synonym.getWord();
            String synonymsStr = synonym.getSynonyms();
            
            if (query.contains(word)) {
                // If the query contains this word, add its synonyms
                List<String> synonymList = Arrays.stream(synonymsStr.split(","))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .collect(Collectors.toList());
                
                if (!synonymList.isEmpty()) {
                    String synonymsExpanded = String.join(" OR ", synonymList);
                    expandedQuery = expandedQuery + " OR " + synonymsExpanded;
                }
            }
        }

        return expandedQuery;
    }

    /**
     * Get all related terms for a word (including the word itself and its synonyms)
     * Uses caching for better performance
     */
    @Cacheable(value = "relatedTerms", key = "#word")
    public List<String> getRelatedTerms(String word) {
        Optional<Synonym> synonymOpt = findByWord(word); // This uses cached data
        
        if (synonymOpt.isPresent()) {
            String synonymsStr = synonymOpt.get().getSynonyms();
            List<String> terms = Arrays.stream(synonymsStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());
            terms.add(0, word); // Add the original word at the beginning
            return terms;
        }
        
        return Arrays.asList(word);
    }

    /**
     * Expand text with synonyms for AI context
     * This method enriches the text with synonym alternatives for better AI understanding
     */
    public String expandTextForAI(String text) {
        if (text == null || text.trim().isEmpty()) {
            return text;
        }

        List<Synonym> allSynonyms = findAll(); // Uses cached data
        StringBuilder enrichedText = new StringBuilder(text);

        for (Synonym synonym : allSynonyms) {
            String word = synonym.getWord();
            if (text.contains(word)) {
                enrichedText.append("\n[Note: '").append(word).append("' can also refer to: ")
                        .append(synonym.getSynonyms()).append("]");
            }
        }

        return enrichedText.toString();
    }

    /**
     * Build synonym context for AI system prompt
     * This provides the AI with knowledge about synonym mappings to better understand queries
     */
    public String buildSynonymContextForAI() {
        List<Synonym> allSynonyms = findAll(); // Uses cached data
        
        if (allSynonyms.isEmpty()) {
            return "";
        }
        
        StringBuilder context = new StringBuilder();
        context.append("[Synonym Knowledge Base]\n");
        context.append("The following are synonym mappings to help understand user queries:\n");
        
        for (Synonym synonym : allSynonyms) {
            context.append("- ").append(synonym.getWord())
                   .append(": ").append(synonym.getSynonyms());
            
            if (synonym.getCategory() != null && !synonym.getCategory().isEmpty()) {
                context.append(" (").append(synonym.getCategory()).append(")");
            }
            
            if (synonym.getDescription() != null && !synonym.getDescription().isEmpty()) {
                context.append(" - ").append(synonym.getDescription());
            }
            
            context.append("\n");
        }
        
        return context.toString();
    }
}
