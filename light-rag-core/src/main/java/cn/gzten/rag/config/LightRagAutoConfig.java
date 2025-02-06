package cn.gzten.rag.config;

import cn.gzten.rag.service.HttpService;
import cn.gzten.rag.service.impl.DefaultHttpServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LightRagAutoConfig {
    @Bean
    @ConditionalOnMissingBean(HttpService.class)
    public HttpService defaultHttpService(ObjectMapper objectMapper) {
        return new DefaultHttpServiceImpl(objectMapper);
    }
}
