package com.example.demo.rag;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordRagServiceTest {

    @Test
    void shouldRetrieveSleepKnowledge() {
        KeywordRagService keywordRagService =
                new KeywordRagService(true);

        String context = keywordRagService.buildContext(
                "成年人每天应该睡多久"
        );

        System.out.println(context);

        assertTrue(context.contains("7 到 9 小时"));
    }

    @Test
    void shouldReturnEmptyWhenNoKeywordMatches() {
        KeywordRagService keywordRagService =
                new KeywordRagService(true);

        String context = keywordRagService.buildContext(
                "我想了解 Spring Boot"
        );

        System.out.println(context);

        assertTrue(context.isEmpty());
    }

    @Test
    void shouldReturnEmptyWhenRagDisabled() {
        KeywordRagService keywordRagService =
                new KeywordRagService(false);

        String context = keywordRagService.buildContext(
                "成年人每天应该睡多久"
        );

        System.out.println(context);

        assertTrue(context.isEmpty());
    }
}