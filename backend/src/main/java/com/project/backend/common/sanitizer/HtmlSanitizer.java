package com.project.backend.common.sanitizer;

import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private final Safelist safelist;

    public HtmlSanitizer() {
        this.safelist = Safelist.relaxed()
                .addTags("span", "div", "mark")
                .addAttributes("a", "href", "target", "rel")
                .addAttributes("span", "style")
                .addAttributes("mark", "style")
                .addAttributes("p", "style")
                .addAttributes("div", "style")
                .addAttributes("img", "src", "alt", "title", "width", "height")
                .addProtocols("a", "href", "http", "https", "mailto")
                .addProtocols("img", "src", "http", "https", "data");
    }

    public String sanitize(String html) {
        if (html == null) {
            return null;
        }

        return Jsoup.clean(html, safelist);
    }
}