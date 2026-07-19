package com.project.backend.app.openai.dto;

import java.util.List;
import java.util.Map;

public record AiAnalysisRequest(
    String analysisType,
    String subjectId,
    String title,
    String promptVersion,
    Map<String, Object> summary,
    List<String> focusPoints,
    String locale
) {}
