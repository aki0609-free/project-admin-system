package com.project.backend.app.openai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.backend.app.openai.dto.AiAnalysisRequest;
import com.project.backend.app.openai.dto.AiAnalysisResponse;
import com.project.backend.app.openai.entity.AiAnalysisHistory;
import com.project.backend.app.openai.repository.AiAnalysisHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiAnalysisHistoryService {

    private final AiAnalysisHistoryRepository repository;
    private final ObjectMapper objectMapper;

    public void save(
            AiAnalysisRequest request,
            AiAnalysisResponse response
    ) {
        try {
            AiAnalysisHistory entity = new AiAnalysisHistory();
            entity.setAnalysisType(request.analysisType());
            entity.setSubjectId(request.subjectId());
            entity.setTitle(request.title());
            entity.setPromptVersion(request.promptVersion());
            entity.setModel(response.model());
            entity.setSummaryJson(objectMapper.writeValueAsString(request.summary()));
            entity.setResponseJson(objectMapper.writeValueAsString(response));

            repository.save(entity);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("AI analysis history serialization failed", e);
        }
    }
}