package com.project.backend.features.dashboard.renderer;

import com.project.backend.common.sanitizer.HtmlSanitizer;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;
import com.project.backend.features.dashboard.service.renderer.NoticeContentRenderer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NoticeContentRendererTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    private final NoticeContentRenderer renderer =
            new NoticeContentRenderer(sanitizer);

    @Test
    void render_shouldReturnPlainText_whenPlainText() {
        String result = renderer.render(
                NoticeContentFormat.PLAIN_TEXT,
                "テスト本文"
        );

        assertThat(result).isEqualTo("テスト本文");
    }

    @Test
    void render_shouldSanitizeHtml_whenHtml() {
        String html = """
                <div>
                    OK
                    <script>alert('xss')</script>
                </div>
                """;

        String result = renderer.render(
                NoticeContentFormat.HTML,
                html
        );

        assertThat(result).contains("OK");
        assertThat(result).doesNotContain("<script>");
    }

    @Test
    void render_shouldReturnMarkdownAsIs_forNow() {
        String markdown = """
                # タイトル
                
                - test
                """;

        String result = renderer.render(
                NoticeContentFormat.MARKDOWN,
                markdown
        );

        assertThat(result).isEqualTo(markdown);
    }

    @Test
    void render_shouldFallbackToPlainText_whenFormatIsNull() {
        String result = renderer.render(
                null,
                "fallback"
        );

        assertThat(result).isEqualTo("fallback");
    }

    @Test
    void render_shouldReturnNull_whenContentIsNull() {
        String result = renderer.render(
                NoticeContentFormat.HTML,
                null
        );

        assertThat(result).isNull();
    }
}