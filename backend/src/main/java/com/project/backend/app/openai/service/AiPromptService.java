package com.project.backend.app.openai.service;


import com.project.backend.app.openai.dto.AiAnalysisRequest;
import org.springframework.stereotype.Service;

@Service
public class AiPromptService {

    public String buildSystemPrompt(AiAnalysisRequest request) {
        String locale = request.locale() == null || request.locale().isBlank()
                ? "ja-JP"
                : request.locale();

        return """
                あなたは業務データ分析アシスタントです。
                与えられた summary データをもとに、事実ベースで簡潔に分析してください。
                数値の再計算を主目的にせず、傾向、リスク、示唆、改善提案を出してください。
                入力にない事実は断定しないでください。
                曖昧な場合は「考えられます」と表現してください。
                出力は指定された JSON スキーマに厳密に従ってください。
                                
                analysisType: %s
                locale: %s
                """.formatted(request.analysisType(), locale);
    }

    public String buildUserPrompt(AiAnalysisRequest request) {
        return """
                タイトル: %s
                注目観点: %s
                summary: %s
                """.formatted(
                request.title(),
                request.focusPoints(),
                request.summary()
        );
    }
}