package com.project.backend.features.system.report.service.core;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ReportHtmlTemplateRendererTest {

    private final ReportHtmlTemplateRenderer renderer =
            new ReportHtmlTemplateRenderer();

    @Test
    void rendersTrustedThymeleafTemplateSource() {
        String html = renderer.render(
                "<p th:text=\"${rows[0]['name']}\">empty</p>",
                Map.of("rows", List.of(Map.of("name", "日別帳票")))
        );

        assertThat(html).contains(">日別帳票</p>");
    }
}
