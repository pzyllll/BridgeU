package com.globalbuddy;

import com.globalbuddy.model.News;
import com.globalbuddy.service.NewsCrawlerService;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple smoke test for NewsCrawlerService.
 *
 * Run locally with:
 *   mvn -Dtest=NewsCrawlerSmokeTest test
 *
 * It will print how many items are fetched per source and
 * list up to 5 sample items (title + URL) for each source.
 */
@SpringBootTest
@Disabled("Smoke test hits external sources; keep for local runs only.")
public class NewsCrawlerSmokeTest {

    @Autowired
    private NewsCrawlerService newsCrawlerService;

    @Test
    void crawlAllThaiNews_smokeTest() {
        List<News> all = newsCrawlerService.crawlAllThaiNews();
        System.out.println("==== NewsCrawlerSmokeTest ====");
        System.out.println("TOTAL items fetched: " + all.size());

        Map<String, List<News>> bySource = all.stream()
                .collect(Collectors.groupingBy(News::getSource));

        bySource.forEach((source, list) -> {
            System.out.println();
            System.out.println("Source: " + source + " | count = " + list.size());
            list.stream()
                    .limit(5)
                    .forEach(n -> System.out.println("  - " + safe(n.getTitle()) + " | " + safe(n.getOriginalUrl())));
        });
    }

    private String safe(String s) {
        return s == null ? "(null)" : s;
    }
}


