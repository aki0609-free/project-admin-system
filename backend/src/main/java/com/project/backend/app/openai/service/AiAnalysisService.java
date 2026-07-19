package com.project.backend.app.openai.service;

import com.project.backend.app.openai.config.AiAnalysisProperties;
import com.project.backend.app.openai.dto.AiAnalysisRequest;
import com.project.backend.app.openai.dto.AiAnalysisResponse;
import com.project.backend.app.openai.dto.AiAnalysisStructuredResult;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@ConditionalOnProperty(prefix = "app.ai", name = "enabled", havingValue = "true", matchIfMissing = true)
public class AiAnalysisService {

    private final ChatClient.Builder chatClientBuilder;
    private final AiPromptService aiPromptService;
    private final AiAnalysisProperties properties;
    private final AiAnalysisHistoryService historyService;

    @SuppressWarnings("null")
    public AiAnalysisResponse analyze(AiAnalysisRequest request) {
        BeanOutputConverter<AiAnalysisStructuredResult> outputConverter =
                new BeanOutputConverter<>(AiAnalysisStructuredResult.class);

        ChatClient chatClient = chatClientBuilder.build();

        AiAnalysisStructuredResult result = chatClient.prompt()
                .system(aiPromptService.buildSystemPrompt(request))
                .user(u -> u.text("""
                        次のデータを分析してください。
                        %s
                                                
                        出力形式:
                        %s
                        """.formatted(
                        aiPromptService.buildUserPrompt(request),
                        outputConverter.getFormat()
                )))
                .call()
                .entity(outputConverter);

        AiAnalysisResponse response = new AiAnalysisResponse(
                result.summary(),
                result.comments(),
                result.risks(),
                result.recommendations(),
                "spring-ai-openai",
                request.promptVersion() != null ? request.promptVersion() : properties.promptVersion()
        );

        historyService.save(request, response);

        return response;
    }
}
