package com.example.demo.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

/** 为微信 Bot 提供可恢复的 M4 文件存储。 */
@Configuration
public class SupportStorageConfig {

    @Bean
    public SupportStateStore supportStateStore(
            @Value("${support.storage-file:data/support-state.json}") String storageFile
    ) {
        return new FileSupportStateStore(Path.of(storageFile));
    }
}
