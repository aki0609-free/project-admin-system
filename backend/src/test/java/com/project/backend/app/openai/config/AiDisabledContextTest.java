package com.project.backend.app.openai.config;

import com.project.backend.app.openai.controller.AiAnalysisController;
import com.project.backend.app.openai.service.AiAnalysisService;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.model.chat.client.autoconfigure.ChatClientAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AiDisabledContextTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    OpenAiChatAutoConfiguration.class,
                    ChatClientAutoConfiguration.class
            ))
            .withUserConfiguration(AiAnalysisController.class, AiAnalysisService.class)
            .withPropertyValues(
                    "app.ai.enabled=false",
                    "spring.ai.model.chat=none",
                    "spring.ai.chat.client.enabled=false"
            );

    @Test
    void startsWithoutOpenAiApiKeyWhenAiIsDisabled() {
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(AiAnalysisController.class);
            assertThat(context).doesNotHaveBean(AiAnalysisService.class);
            assertThat(context).doesNotHaveBean(ChatClient.Builder.class);
        });
    }
}
