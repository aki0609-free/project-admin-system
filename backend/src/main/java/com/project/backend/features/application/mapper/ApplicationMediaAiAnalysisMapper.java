package com.project.backend.features.application.mapper;

import com.project.backend.app.openai.dto.AiAnalysisRequest;
import com.project.backend.features.application.dto.ApplicationMediaSummaryRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ApplicationMediaAiAnalysisMapper {

    public AiAnalysisRequest toAiAnalysisRequest(
            String subjectId,
            ApplicationMediaSummaryRequest summaryRequest
    ) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalCost", summaryRequest.totalCost());
        summary.put("totalHires", summaryRequest.totalHires());
        summary.put("averageUnitPrice", summaryRequest.averageUnitPrice());
        summary.put("mediaSummary", summaryRequest.mediaSummary());
        summary.put("monthlySummary", summaryRequest.monthlySummary());

        return new AiAnalysisRequest(
                "application-media-performance",
                subjectId,
                "応募媒体分析",
                "v1",
                summary,
                List.of(
                        "効率の良い媒体",
                        "コストの高い媒体",
                        "採用数が多い媒体",
                        "改善提案"
                ),
                "ja-JP"
        );
    }
}