package top.mryan2005.sspubot.sspubotbackend.Controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ping")
public class MainController {

    @GetMapping("/get")
    public String ping() {
        return "pong";
    }
}
