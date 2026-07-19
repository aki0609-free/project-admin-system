package com.project.backend.app.openai.controller;

import com.project.backend.app.openai.dto.AiAnalysisRequest;
import com.project.backend.app.openai.dto.AiAnalysisResponse;
import com.project.backend.app.openai.service.AiAnalysisService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiAnalysisControllerTest {

    @Mock
    private ObjectProvider<AiAnalysisService> aiAnalysisServiceProvider;

    @Mock
    private AiAnalysisService aiAnalysisService;

    private AiAnalysisController controller;

    @BeforeEach
    void setUp() {
        controller = new AiAnalysisController(aiAnalysisServiceProvider);
    }

    @Test
    void returnsServiceUnavailableWhenAiIsDisabled() {
        when(aiAnalysisServiceProvider.getIfAvailable()).thenReturn(null);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> controller.analyze(request())
        );

        assertEquals(HttpStatus.SERVICE_UNAVAILABLE, exception.getStatusCode());
        assertEquals("AI analysis is disabled", exception.getReason());
    }

    @Test
    void delegatesToServiceWhenAiIsEnabled() {
        AiAnalysisRequest request = request();
        AiAnalysisResponse expected = new AiAnalysisResponse(
                "summary",
                List.of("comment"),
                List.of("risk"),
                List.of("recommendation"),
                "spring-ai-openai",
                "v1"
        );
        when(aiAnalysisServiceProvider.getIfAvailable()).thenReturn(aiAnalysisService);
        when(aiAnalysisService.analyze(request)).thenReturn(expected);

        AiAnalysisResponse actual = controller.analyze(request);

        assertSame(expected, actual);
    }

    private AiAnalysisRequest request() {
        return new AiAnalysisRequest(
                "summary",
                "subject-1",
                "title",
                "v1",
                Map.of("count", 1),
                List.of("risk"),
                "ja-JP"
        );
    }
}
