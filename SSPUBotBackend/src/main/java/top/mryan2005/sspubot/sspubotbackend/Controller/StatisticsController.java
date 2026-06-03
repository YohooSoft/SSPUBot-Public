package top.mryan2005.sspubot.sspubotbackend.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.mryan2005.sspubot.sspubotbackend.Service.StatisticsService;

import java.util.Map;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    @Autowired
    StatisticsService statisticsService;

    @GetMapping("/source-count")
    public Map<String, Long> getFileCountBySource() {
        return statisticsService.getFileCountBySource();
    }

    @GetMapping("/daily-count")
    public Map<String, Long> getDailyFileCount() {
        return statisticsService.getDailyFileCount();
    }

    @GetMapping("/word-cloud")
    public Map<String, Integer> getWordCloudData() {
        return statisticsService.getWordCloudData();
    }
}
