package com.example.demo.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class KeywordRagService {

    private final boolean enabled;

    private final List<RagDocument> documents = List.of(
            new RagDocument(
                    "成年人睡眠时长",
                    List.of("睡眠", "睡多久", "睡眠时长", "成年人"),
                    "成年人通常建议每天睡眠 7 到 9 小时，具体需求会因年龄和个体差异而不同。"
            ),
            new RagDocument(
                    "改善睡眠习惯",
                    List.of("失眠", "作息", "入睡", "睡前"),
                    "建议保持固定的入睡和起床时间，睡前减少使用手机，保持安静、舒适的睡眠环境。"
            ),
            new RagDocument(
                    "夜间频繁醒来",
                    List.of("醒来", "夜醒", "睡不着"),
                    "夜间频繁醒来可能影响睡眠连续性。如果长期出现，建议记录睡眠情况并咨询专业医生。"
            )
    );

    public KeywordRagService(
            @Value("${rag.enabled:true}") boolean enabled
    ) {
        this.enabled = enabled;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public List<RagDocument> retrieve(String query) {
        if (!enabled || query == null || query.isBlank()) {
            return List.of();
        }

        return documents.stream()
                .map(document -> new ScoredDocument(
                        document,
                        calculateScore(document, query)
                ))
                .filter(item -> item.score() > 0)
                .sorted(Comparator.comparingInt(
                        ScoredDocument::score
                ).reversed())
                .limit(2)
                .map(ScoredDocument::document)
                .toList();
    }

    public String buildContext(String query) {
        List<RagDocument> results = retrieve(query);

        if (results.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();

        for (RagDocument document : results) {
            context.append("【")
                    .append(document.title())
                    .append("】\n")
                    .append(document.content())
                    .append("\n\n");
        }

        return context.toString().trim();
    }

    private int calculateScore(RagDocument document, String query) {
        int score = 0;

        for (String keyword : document.keywords()) {
            if (query.contains(keyword)) {
                score++;
            }
        }

        return score;
    }

    private record ScoredDocument(
            RagDocument document,
            int score
    ) {
    }
}