package top.mryan2005.sspubot.sspubotbackend.Repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import top.mryan2005.sspubot.sspubotbackend.Pojo.Spider;

import java.util.List;
import java.util.Optional;

@Repository
public interface SpiderRepository extends JpaRepository<Spider, Long> {
    Optional<Spider> findByName(String name);
    List<Spider> findByIsActive(Boolean isActive);
}
