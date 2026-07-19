package com.project.backend.features.dashboard.service.renderer;

import org.springframework.stereotype.Component;

import com.project.backend.common.sanitizer.HtmlSanitizer;
import com.project.backend.features.dashboard.enums.NoticeContentFormat;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class NoticeContentRenderer {

    private final HtmlSanitizer htmlSanitizer;

    public String render(
            NoticeContentFormat contentFormat,
            String content
    ) {
        if (content == null) {
            return null;
        }

        NoticeContentFormat format = contentFormat != null
                ? contentFormat
                : NoticeContentFormat.PLAIN_TEXT;

        return switch (format) {
            case HTML -> htmlSanitizer.sanitize(content);
            case MARKDOWN -> renderMarkdown(content);
            case PLAIN_TEXT -> content;
        };
    }

    private String renderMarkdown(String markdown) {
        // いったんMarkdownはプレーンテキスト扱い。
        // 将来 commonmark などを入れて HTML変換してから sanitize する。
        return markdown;
    }
}