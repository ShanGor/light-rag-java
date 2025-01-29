package cn.gzten.rag.controller;

import cn.gzten.rag.service.TestService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Resource
    TestService testService;
    @PostConstruct
    public void init() {
        System.out.println("LightRag is running");
    }

    @GetMapping("/test-cache")
    public String testCache() {
        return testService.testCacheDetails();
    }


}
