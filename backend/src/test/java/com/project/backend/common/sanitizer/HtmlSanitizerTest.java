package com.project.backend.common.sanitizer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @Test
    void sanitize_shouldKeepSafeHtml() {
        String html = """
                <div>
                    <h1>タイトル</h1>
                    <p>本文です。</p>
                    <a href="https://example.com">リンク</a>
                </div>
                """;

        String result = sanitizer.sanitize(html);

        assertThat(result).contains("<h1>タイトル</h1>");
        assertThat(result).contains("<p>本文です。</p>");
        assertThat(result).contains("href=\"https://example.com\"");
    }

    @Test
    void sanitize_shouldRemoveScriptTag() {
        String html = """
                <div>
                    test
                    <script>alert('xss')</script>
                </div>
                """;

        String result = sanitizer.sanitize(html);

        assertThat(result).doesNotContain("<script>");
        assertThat(result).doesNotContain("alert(");
        assertThat(result).contains("test");
    }

    @Test
    void sanitize_shouldRemoveJavascriptHref() {
        String html = """
                <a href="javascript:alert('xss')">
                    click
                </a>
                """;

        String result = sanitizer.sanitize(html);

        assertThat(result).doesNotContain("javascript:");
        assertThat(result).contains("click");
    }

    @Test
    void sanitize_shouldAllowImageTag() {
        String html = """
                <img src="https://example.com/test.png" alt="test">
                """;

        String result = sanitizer.sanitize(html);

        assertThat(result).contains("<img");
        assertThat(result).contains("src=\"https://example.com/test.png\"");
    }

    @Test
    void sanitize_shouldReturnNull_whenInputIsNull() {
        String result = sanitizer.sanitize(null);

        assertThat(result).isNull();
    }
}