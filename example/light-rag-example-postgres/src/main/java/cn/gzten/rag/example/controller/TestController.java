package cn.gzten.rag.example.controller;

import cn.gzten.rag.example.service.TestService;
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
        testService.doCache();
        testService.monitorCache();
        var res = testService.testCacheDetails();
        testService.monitorCache();
        return res;
    }


}
