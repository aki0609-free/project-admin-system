package com.project.backend.features.application.controller;

import com.project.backend.app.openai.dto.AiAnalysisResponse;
import com.project.backend.app.openai.service.AiAnalysisService;
import com.project.backend.features.application.dto.ApplicationMediaSummaryRequest;
import com.project.backend.features.application.mapper.ApplicationMediaAiAnalysisMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/application-media/analysis")
@RequiredArgsConstructor
public class ApplicationMediaAiAnalysisController {

    private final ObjectProvider<AiAnalysisService> aiAnalysisServiceProvider;
    private final ApplicationMediaAiAnalysisMapper mapper;

    @PostMapping
    public AiAnalysisResponse analyze(@RequestBody ApplicationMediaSummaryRequest request) {
        AiAnalysisService aiAnalysisService = aiAnalysisServiceProvider.getIfAvailable();
        if (aiAnalysisService == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis is disabled"
            );
        }
        return aiAnalysisService.analyze(
                mapper.toAiAnalysisRequest("application-media:manual", request)
        );
    }
}
