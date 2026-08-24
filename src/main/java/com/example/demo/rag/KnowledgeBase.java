package com.example.demo.rag;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 知识库：启动时加载 resources/knowledge/ 下所有 txt 文件
 * 存储格式：文件名(不含后缀) → 文件内容
 */
@Component
public class KnowledgeBase {

    private final Map<String, String> documents = new LinkedHashMap<>();

    @PostConstruct
    public void init() {
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources("classpath:knowledge/*.txt");

            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) continue;

                // 去掉 .txt 后缀作为文档名
                String docName = filename.endsWith(".txt")
                        ? filename.substring(0, filename.length() - 4)
                        : filename;

                String content = readResource(resource);
                documents.put(docName, content);
                System.out.println("[KnowledgeBase] 已加载知识库文档：" + docName
                        + "（" + content.length() + " 字符）");
            }

            System.out.println("[KnowledgeBase] 共加载 " + documents.size() + " 个知识库文档");
        } catch (Exception e) {
            System.err.println("[KnowledgeBase] 加载知识库失败：" + e.getMessage());
        }
    }

    /**
     * 获取所有文档
     */
    public Map<String, String> getAllDocuments() {
        return documents;
    }

    /**
     * 读取资源文件内容
     */
    private String readResource(Resource resource) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString().trim();
    }
}
