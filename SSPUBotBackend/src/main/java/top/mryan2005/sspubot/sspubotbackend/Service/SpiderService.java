package top.mryan2005.sspubot.sspubotbackend.Service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Spider;
import top.mryan2005.sspubot.sspubotbackend.Repository.SpiderRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class SpiderService {
    
    @Autowired
    private SpiderRepository spiderRepository;

    public List<Spider> findAll() {
        return spiderRepository.findAll();
    }

    public Optional<Spider> findById(Long id) {
        return spiderRepository.findById(id);
    }

    public Optional<Spider> findByName(String name) {
        return spiderRepository.findByName(name);
    }

    public List<Spider> findActiveSpiders() {
        return spiderRepository.findByIsActive(true);
    }

    public Spider save(Spider spider) {
        if (spider.getCreatedAt() == null) {
            spider.setCreatedAt(LocalDateTime.now());
        }
        spider.setUpdatedAt(LocalDateTime.now());
        return spiderRepository.save(spider);
    }

    public void deleteById(Long id) {
        spiderRepository.deleteById(id);
    }

    public void updateSpiderStatus(Long id, String status, Integer progress) {
        Optional<Spider> spiderOpt = spiderRepository.findById(id);
        if (spiderOpt.isPresent()) {
            Spider spider = spiderOpt.get();
            spider.setStatus(status);
            if (progress != null) {
                spider.setProgress(progress);
            }
            spider.setLastRunTime(LocalDateTime.now());
            spider.setUpdatedAt(LocalDateTime.now());
            spiderRepository.save(spider);
        }
    }

    public void updateSpiderError(Long id, String error) {
        Optional<Spider> spiderOpt = spiderRepository.findById(id);
        if (spiderOpt.isPresent()) {
            Spider spider = spiderOpt.get();
            spider.setStatus("error");
            spider.setLastError(error);
            spider.setUpdatedAt(LocalDateTime.now());
            spiderRepository.save(spider);
        }
    }
}
