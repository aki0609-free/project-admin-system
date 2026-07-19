package com.project.backend.app.openai.dto;

import java.util.List;

public record AiAnalysisResponse(
    String summary,
    List<String> comments,
    List<String> risks,
    List<String> recommendations,
    String model,
    String promptVersion
) {}
