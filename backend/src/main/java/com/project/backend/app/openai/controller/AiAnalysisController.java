package com.project.backend.app.openai.controller;

import com.project.backend.app.openai.dto.AiAnalysisRequest;
import com.project.backend.app.openai.dto.AiAnalysisResponse;
import com.project.backend.app.openai.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ai/analysis")
@RequiredArgsConstructor
public class AiAnalysisController {

    private final ObjectProvider<AiAnalysisService> aiAnalysisServiceProvider;

    @PostMapping
    public AiAnalysisResponse analyze(@RequestBody AiAnalysisRequest request) {
        AiAnalysisService aiAnalysisService = aiAnalysisServiceProvider.getIfAvailable();
        if (aiAnalysisService == null) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "AI analysis is disabled"
            );
        }
        return aiAnalysisService.analyze(request);
    }
}
