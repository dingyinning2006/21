package com.example.demo.agent.contract.m5.config;

import com.example.demo.agent.contract.m5.service.InMemoryTaskTemplateRepository;
import com.example.demo.agent.contract.m5.service.TaskRearranger;
import com.example.demo.agent.contract.m5.service.TaskSelector;
import com.example.demo.agent.contract.m5.service.TaskTemplateRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * M5 现实任务模块 Spring Boot 自动配置。
 *
 * <p>集成到主项目后，自动注册 TaskTemplateRepository、TaskSelector、TaskRearranger。
 * 如需替换为数据库实现，只需在主项目中定义自己的 TaskTemplateRepository Bean 即可覆盖。
 */
@Configuration
public class M5AutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(TaskTemplateRepository.class)
    public TaskTemplateRepository taskTemplateRepository() {
        return new InMemoryTaskTemplateRepository();
    }

    @Bean
    public TaskSelector taskSelector(TaskTemplateRepository repository) {
        return new TaskSelector(repository);
    }

    @Bean
    public TaskRearranger taskRearranger(TaskSelector taskSelector) {
        return new TaskRearranger(taskSelector);
    }
}
