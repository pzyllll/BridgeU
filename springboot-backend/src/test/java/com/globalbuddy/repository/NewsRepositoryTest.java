package com.globalbuddy.repository;

import com.globalbuddy.model.News;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Calendar;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DataJpaTest
class NewsRepositoryTest {

    @Autowired
    private NewsRepository newsRepository;

    private static Date date(int year, int month1Based, int day, int hour, int minute) {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, month1Based - 1);
        cal.set(Calendar.DAY_OF_MONTH, day);
        cal.set(Calendar.HOUR_OF_DAY, hour);
        cal.set(Calendar.MINUTE, minute);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTime();
    }

    @Test
    @DisplayName("UTC-12: findByKeyword(keyword, pageable) returns matched news ordered by publishDate desc")
    void utc12_findByKeyword_returnsMatchedOrdered() {
        News a = News.builder()
                .title("Thailand visa update")
                .summary("Some summary")
                .originalContent("Content A")
                .publishDate(date(2026, 1, 10, 10, 0))
                .createTime(new Date())
                .build();

        News b = News.builder()
                .title("Other topic")
                .summary("VISA policy changed") // keyword in summary (case-insensitive)
                .originalContent("Content B")
                .publishDate(date(2026, 1, 11, 10, 0)) // newer
                .createTime(new Date())
                .build();

        News c = News.builder()
                .title("No match here")
                .summary("No match")
                .originalContent("Content C")
                .publishDate(date(2026, 1, 12, 10, 0))
                .createTime(new Date())
                .build();

        newsRepository.save(a);
        newsRepository.save(b);
        newsRepository.save(c);

        Pageable pageable = PageRequest.of(0, 10);
        Page<News> result = newsRepository.findByKeyword("visa", pageable);

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());

        // Must be ordered by publishDate desc -> b then a
        assertEquals("Other topic", result.getContent().get(0).getTitle());
        assertEquals("Thailand visa update", result.getContent().get(1).getTitle());
    }

    @Test
    @DisplayName("UTC-12-3: findByKeyword(keyword, pageable) searches in translation fields (titleZh/titleEn/summaryZh/summaryEn)")
    void utc12_findByKeyword_searchesTranslationFields() {
        News zh = News.builder()
                .title("No match")
                .summary("No match")
                .titleZh("中文 标题 包含 关键字")
                .summaryZh("中文摘要")
                .publishDate(date(2026, 1, 10, 10, 0))
                .createTime(new Date())
                .build();

        News en = News.builder()
                .title("No match")
                .summary("No match")
                .titleEn("English Title Contains Keyword")
                .summaryEn("English Summary")
                .publishDate(date(2026, 1, 11, 10, 0))
                .createTime(new Date())
                .build();

        newsRepository.save(zh);
        newsRepository.save(en);

        Page<News> result = newsRepository.findByKeyword("关键字", PageRequest.of(0, 10));
        assertEquals(1, result.getTotalElements());
        assertEquals(1, result.getContent().size());
        assertNotNull(result.getContent().get(0).getTitleZh());
    }

    @Test
    @DisplayName("UTC-12-5: findByKeyword(keyword, pageable) returns empty page when no matches")
    void utc12_findByKeyword_returnsEmptyWhenNoMatches() {
        News a = News.builder()
                .title("Something else")
                .summary("Other")
                .publishDate(date(2026, 1, 10, 10, 0))
                .createTime(new Date())
                .build();
        newsRepository.save(a);

        Page<News> result = newsRepository.findByKeyword("nonexistentkeyword12345", PageRequest.of(0, 10));
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    @Test
    @DisplayName("UTC-12-6: findByKeyword(keyword, pageable) respects pagination")
    void utc12_findByKeyword_respectsPagination() {
        for (int i = 0; i < 15; i++) {
            News n = News.builder()
                    .title("Thailand visa update " + i)
                    .summary("s")
                    .publishDate(date(2026, 1, 1, 0, i))
                    .createTime(new Date())
                    .build();
            newsRepository.save(n);
        }

        Page<News> page0 = newsRepository.findByKeyword("Thailand", PageRequest.of(0, 10));
        assertEquals(15, page0.getTotalElements());
        assertEquals(10, page0.getContent().size());

        Page<News> page1 = newsRepository.findByKeyword("Thailand", PageRequest.of(1, 10));
        assertEquals(15, page1.getTotalElements());
        assertEquals(5, page1.getContent().size());
    }

    @Test
    @DisplayName("UTC-13: findByPublishDateBetweenOrdered(start,end,pageable) respects range + ordering")
    void utc13_findByPublishDateBetweenOrdered_respectsRangeAndOrder() {
        News old = News.builder()
                .title("Old")
                .publishDate(date(2026, 1, 1, 0, 0))
                .createTime(new Date())
                .build();
        News in1 = News.builder()
                .title("InRange-1")
                .publishDate(date(2026, 1, 15, 12, 0))
                .createTime(new Date())
                .build();
        News in2 = News.builder()
                .title("InRange-2")
                .publishDate(date(2026, 1, 20, 12, 0)) // newer
                .createTime(new Date())
                .build();
        News future = News.builder()
                .title("Future")
                .publishDate(date(2026, 2, 1, 0, 0))
                .createTime(new Date())
                .build();

        newsRepository.save(old);
        newsRepository.save(in1);
        newsRepository.save(in2);
        newsRepository.save(future);

        Date start = date(2026, 1, 10, 0, 0);
        Date end = date(2026, 1, 31, 23, 59);

        Page<News> result = newsRepository.findByPublishDateBetweenOrdered(start, end, PageRequest.of(0, 10));

        assertEquals(2, result.getTotalElements());
        assertEquals(2, result.getContent().size());
        assertEquals("InRange-2", result.getContent().get(0).getTitle());
        assertEquals("InRange-1", result.getContent().get(1).getTitle());

        assertTrue(result.getContent().get(0).getPublishDate().after(result.getContent().get(1).getPublishDate()));
    }

    @Test
    @DisplayName("UTC-13-3: findByPublishDateBetweenOrdered(start,end,pageable) includes boundary dates")
    void utc13_findByPublishDateBetweenOrdered_includesBoundaryDates() {
        News atStart = News.builder()
                .title("AtStart")
                .publishDate(date(2026, 1, 15, 0, 0))
                .createTime(new Date())
                .build();
        News middle = News.builder()
                .title("Middle")
                .publishDate(date(2026, 1, 15, 12, 0))
                .createTime(new Date())
                .build();
        News atEnd = News.builder()
                .title("AtEnd")
                .publishDate(date(2026, 1, 15, 23, 59))
                .createTime(new Date())
                .build();

        newsRepository.save(atStart);
        newsRepository.save(middle);
        newsRepository.save(atEnd);

        Date start = date(2026, 1, 15, 0, 0);
        Date end = date(2026, 1, 15, 23, 59);

        Page<News> result = newsRepository.findByPublishDateBetweenOrdered(start, end, PageRequest.of(0, 10));
        assertEquals(3, result.getTotalElements());
        assertEquals(3, result.getContent().size());
        // Ordered DESC -> AtEnd, Middle, AtStart
        assertEquals("AtEnd", result.getContent().get(0).getTitle());
        assertEquals("Middle", result.getContent().get(1).getTitle());
        assertEquals("AtStart", result.getContent().get(2).getTitle());
    }

    @Test
    @DisplayName("UTC-13-4: findByPublishDateBetweenOrdered(start,end,pageable) returns empty page when no matches")
    void utc13_findByPublishDateBetweenOrdered_returnsEmptyWhenNoMatches() {
        News a = News.builder()
                .title("Only Jan")
                .publishDate(date(2026, 1, 10, 10, 0))
                .createTime(new Date())
                .build();
        newsRepository.save(a);

        Date start = date(2026, 12, 1, 0, 0);
        Date end = date(2026, 12, 31, 23, 59);

        Page<News> result = newsRepository.findByPublishDateBetweenOrdered(start, end, PageRequest.of(0, 10));
        assertEquals(0, result.getTotalElements());
        assertEquals(0, result.getContent().size());
    }

    @Test
    @DisplayName("UTC-13-5: findByPublishDateBetweenOrdered(start,end,pageable) respects pagination")
    void utc13_findByPublishDateBetweenOrdered_respectsPagination() {
        // 12 items in range
        for (int i = 0; i < 12; i++) {
            News n = News.builder()
                    .title("InRange-" + i)
                    .publishDate(date(2026, 1, 20, 10, i))
                    .createTime(new Date())
                    .build();
            newsRepository.save(n);
        }

        Date start = date(2026, 1, 1, 0, 0);
        Date end = date(2026, 1, 31, 23, 59);

        Page<News> p0 = newsRepository.findByPublishDateBetweenOrdered(start, end, PageRequest.of(0, 10));
        assertEquals(12, p0.getTotalElements());
        assertEquals(10, p0.getContent().size());

        Page<News> p1 = newsRepository.findByPublishDateBetweenOrdered(start, end, PageRequest.of(1, 10));
        assertEquals(12, p1.getTotalElements());
        assertEquals(2, p1.getContent().size());
    }
}


