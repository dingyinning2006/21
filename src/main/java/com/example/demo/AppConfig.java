package com.example.demo;

import com.example.demo.llm.LlmService;
import com.example.demo.util.ConfigUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.example.demo")
public class AppConfig {

    @Bean
    public LlmService llmService() {
        // 用你现有的 ConfigUtil 读配置，和原来保持一致
        String apiKey = ConfigUtil.get("llm.api-key");
        String baseUrl = ConfigUtil.get("llm.base-url");
        String model = ConfigUtil.get("llm.model");
        return new LlmService(apiKey, baseUrl, model);
    }
}
