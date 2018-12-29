package com.richslide.atesearch.crawler.helper.utilities;

import com.richslide.atesearch.business.helper.CommonsUtil;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsoupUtil {
    public static Optional<String> getText(final Element element) {
        if (element.hasText()) {
            return Optional.of(element.text().trim());
        } else if (element.childNodeSize() > 0) {
            final StringBuilder text = new StringBuilder();
            for (Element child : element.children()) {
                Optional<String> temp = getText(child);
                temp.ifPresent(s -> text.append(s).append(" "));
            }
            return  Optional.of(text.toString().trim());
        } else {
            return Optional.empty();
        }
    }

    public static Optional<Document> getDocument(final String url, final String baseUrl, final long latency) {
        try {
            TimeUnit.SECONDS.sleep(latency);
            return Optional.of(Jsoup.connect(url)
//                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
//                            "AppleWebKit/537.36 (KHTML, like Gecko) " +
//                            "Chrome/66.0.3359.181 Safari/537.36")
                    .userAgent("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_13_6) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/11.1.2 Safari/605.1.15")
                    .referrer(baseUrl)
                    .get());
        } catch (IOException | InterruptedException e) {
            log.error("getDocument() Failure with IOException [URL:{}({})]", url, e.getMessage());
            return Optional.empty();
        }
    }

    public static <T> void putOn(Map<String, Object> map, final String key, final String value, final Class<T> cls) {
        try {
            final String str = Objects.requireNonNull(value).trim();
            if (str.length() < 1 || str.equals("-")) return;
            T val = CommonsUtil.parsePrimitiveWrapper(cls, str);
            map.put(key, val);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
    }
}
