package com.example.demo.rag;

import java.util.List;
public record RagDocument(
        String title,
        List<String> keywords,
        String content
) {
}
