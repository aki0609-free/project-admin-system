package com.project.backend.app.openai.dto;

import java.util.List;

public record AiAnalysisStructuredResult(
        String summary,
        List<String> comments,
        List<String> risks,
        List<String> recommendations
) {}
