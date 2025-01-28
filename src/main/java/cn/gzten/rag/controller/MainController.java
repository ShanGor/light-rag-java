package cn.gzten.rag.controller;

import jakarta.annotation.PostConstruct;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MainController {
    @PostConstruct
    public void init() {
        System.out.println("LightRag is running");
    }
    @GetMapping("/health")
    public String health() {
        return "ok";
    }
}
