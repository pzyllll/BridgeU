package com.globalbuddy.controller;

import com.globalbuddy.model.News;
import com.globalbuddy.repository.NewsRepository;
import com.globalbuddy.security.JwtService;
import com.globalbuddy.service.LanguageDetectionService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = NewsController.class, excludeAutoConfiguration = {
        SecurityAutoConfiguration.class,
        SecurityFilterAutoConfiguration.class
})
@AutoConfigureMockMvc(addFilters = false)
class NewsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private NewsRepository newsRepository;

    @MockBean
    private LanguageDetectionService languageDetectionService;

    // Some security components may still be picked up; provide JwtService to satisfy wiring.
    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsService userDetailsService;

    @Test
    @DisplayName("UTC-10: getDailyBriefing() – Backend returns success + paginated list")
    void utc10_getDailyBriefing_returnsPaginatedList() throws Exception {
        // Arrange
        News n1 = News.builder()
                .id(1L)
                .title("Original EN Title")
                .summary("Original EN Summary")
                .titleEn("Translated EN Title")
                .summaryEn("Translated EN Summary")
                .originalUrl("https://example.com/1")
                .source("Bangkok Post")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        // Act & Assert
        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].title", not(emptyOrNullString())))
                .andExpect(jsonPath("$.pagination.page").value(0))
                .andExpect(jsonPath("$.pagination.size").value(10))
                .andExpect(jsonPath("$.pagination.totalElements").value(1))
                .andExpect(jsonPath("$.pagination.totalPages").value(1))
                .andExpect(jsonPath("$.pagination.hasNext").value(false))
                .andExpect(jsonPath("$.pagination.hasPrevious").value(false));

        verify(newsRepository, atLeastOnce()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-2: getDailyBriefing() – Backend filters by keyword")
    void utc10_getDailyBriefing_filtersByKeyword() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("Thailand visa update")
                .summary("s")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findByKeyword(eq("Thailand"), org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("keyword", "  Thailand  ")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)))
                .andExpect(jsonPath("$.data[0].title", not(emptyOrNullString())));

        verify(newsRepository, times(1)).findByKeyword(eq("Thailand"), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-3: getDailyBriefing() – Backend filters by publish date range")
    void utc10_getDailyBriefing_filtersByDateRange() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("t")
                .summary("s")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findByPublishDateBetweenOrdered(any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findByPublishDateBetweenOrdered(any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-4: getDailyBriefing() – Backend combines keyword + publish date filters")
    void utc10_getDailyBriefing_combinesKeywordAndDateRange() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("Thailand")
                .summary("s")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findByKeywordAndPublishDateBetween(eq("Thailand"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("keyword", "Thailand")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findByKeywordAndPublishDateBetween(eq("Thailand"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findByKeyword(eq("Thailand"), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-7: getDailyBriefing() – Backend filters by source only (all historical news)")
    void utc10_getDailyBriefing_filtersBySourceOnly() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("t")
                .summary("s")
                .source("Bangkok Post")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findBySourceOrdered(eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("source", "Bangkok Post")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findBySourceOrdered(eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-8: getDailyBriefing() – Backend combines source + publish date filters")
    void utc10_getDailyBriefing_combinesSourceAndDateRange() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("t")
                .summary("s")
                .source("Bangkok Post")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findBySourceAndPublishDateBetween(eq("Bangkok Post"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("source", "Bangkok Post")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findBySourceAndPublishDateBetween(eq("Bangkok Post"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findBySourceOrdered(eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-9: getDailyBriefing() – Backend combines keyword + source (no date filter)")
    void utc10_getDailyBriefing_combinesKeywordAndSource_noDate() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("Thailand")
                .summary("s")
                .source("Bangkok Post")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findByKeywordAndSource(eq("Thailand"), eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("keyword", "Thailand")
                        .param("source", "Bangkok Post")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findByKeywordAndSource(eq("Thailand"), eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findByKeyword(eq("Thailand"), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findBySourceOrdered(eq("Bangkok Post"), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-10: getDailyBriefing() – Backend combines keyword + source + publish date filters")
    void utc10_getDailyBriefing_combinesKeywordSourceAndDateRange() throws Exception {
        News n1 = News.builder()
                .id(1L)
                .title("Thailand")
                .summary("s")
                .source("Bangkok Post")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(n1), pageable, 1);

        when(newsRepository.findByKeywordAndSourceAndPublishDateBetween(eq("Thailand"), eq("Bangkok Post"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("keyword", "Thailand")
                        .param("source", "Bangkok Post")
                        .param("startDate", "2026-01-01")
                        .param("endDate", "2026-01-31")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(1)));

        verify(newsRepository, times(1)).findByKeywordAndSourceAndPublishDateBetween(eq("Thailand"), eq("Bangkok Post"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findByKeywordAndPublishDateBetween(eq("Thailand"), any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-5: getDailyBriefing() – Backend ignores invalid date format and returns ALL news")
    void utc10_getDailyBriefing_invalidDateIgnored_returnsAll() throws Exception {
        Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "publishDate"));
        Page<News> page = new PageImpl<>(List.of(), pageable, 0);

        when(newsRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenReturn(page);
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .param("startDate", "invalid-date")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", hasSize(0)));

        verify(newsRepository, times(1)).findAll(org.mockito.ArgumentMatchers.any(Pageable.class));
        verify(newsRepository, never()).findByPublishDateBetweenOrdered(any(Date.class), any(Date.class), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    @DisplayName("UTC-10-6: getDailyBriefing() – Backend returns error response on exception")
    void utc10_getDailyBriefing_returnsErrorOnException() throws Exception {
        when(newsRepository.findAll(org.mockito.ArgumentMatchers.any(Pageable.class))).thenThrow(new RuntimeException("Database connection error"));

        mockMvc.perform(get("/api/news/daily-briefing")
                        .param("page", "0")
                        .param("size", "10")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Database connection error")));
    }

    @Test
    @DisplayName("UTC-11: getNewsDetail() – Backend returns dto + originalContent when not Thai")
    void utc11_getNewsDetail_returnsOriginalContent_whenNotThai() throws Exception {
        News n = News.builder()
                .id(123L)
                .title("Hello")
                .summary("Summary")
                .originalContent("English content")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        when(newsRepository.findById(123L)).thenReturn(Optional.of(n));
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing/123")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123))
                .andExpect(jsonPath("$.originalContent").value("English content"));
    }

    @Test
    @DisplayName("UTC-11-2: getNewsDetail() – Backend returns 404 for non-existent ID")
    void utc11_getNewsDetail_returns404_whenNotFound() throws Exception {
        when(newsRepository.findById(99999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/news/daily-briefing/99999")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("News not found")));
    }

    @Test
    @DisplayName("UTC-11-3: getNewsDetail() – Backend returns Chinese translation when lang=zh")
    void utc11_getNewsDetail_returnsChineseTranslation_whenLangZh() throws Exception {
        News n = News.builder()
                .id(123L)
                .title("Original EN")
                .summary("Original summary")
                .titleZh("中文标题")
                .summaryZh("中文摘要")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        when(newsRepository.findById(123L)).thenReturn(Optional.of(n));
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(true);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        mockMvc.perform(get("/api/news/daily-briefing/123")
                        .param("lang", "zh")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("中文标题"))
                .andExpect(jsonPath("$.data.summary").value("中文摘要"));
    }

    @Test
    @DisplayName("UTC-11-6: getNewsDetail() – Backend returns error response on exception")
    void utc11_getNewsDetail_returnsErrorOnException() throws Exception {
        when(newsRepository.findById(123L)).thenThrow(new RuntimeException("Database error"));

        mockMvc.perform(get("/api/news/daily-briefing/123")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Database error")));
    }

    @Test
    @DisplayName("UTC-11-2: getNewsDetail() – Backend hides originalContent when Thai")
    void utc11_getNewsDetail_hidesOriginalContent_whenThai() throws Exception {
        News n = News.builder()
                .id(123L)
                .title("Hello")
                .summary("Summary")
                .originalContent("ภาษาไทย")
                .publishDate(new Date())
                .createTime(new Date())
                .build();

        when(newsRepository.findById(123L)).thenReturn(Optional.of(n));
        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(true);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(true);

        mockMvc.perform(get("/api/news/daily-briefing/123")
                        .param("lang", "en")
                        .accept(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(123))
                .andExpect(jsonPath("$.originalContent").value(nullValue()));
    }

    @Test
    @DisplayName("UTC-09: convertToDTO(news, lang) – Thai title never leaks; uses EN fallback")
    void utc09_convertToDTO_thaiTitleNeverLeaks_useEnglishFallback() throws Exception {
        // We test the private helper via reflection, to match the current code structure.
        NewsController controller = new NewsController(newsRepository, languageDetectionService);

        News n = News.builder()
                .id(7L)
                .title("ภาษาไทย") // Thai
                .summary("ภาษาไทย") // Thai
                .titleZh("") // no zh translation
                .summaryZh("") // no zh translation
                .titleEn("English Title")
                .summaryEn("English Summary")
                .build();

        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(true);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(true);

        Method m = NewsController.class.getDeclaredMethod("convertToDTO", News.class, String.class);
        m.setAccessible(true);
        Object dtoObj = m.invoke(controller, n, "zh");

        // dto is NewsBriefDTO; access via getters through reflection to avoid coupling imports
        Method getTitle = dtoObj.getClass().getMethod("getTitle");
        Method getSummary = dtoObj.getClass().getMethod("getSummary");

        String title = (String) getTitle.invoke(dtoObj);
        String summary = (String) getSummary.invoke(dtoObj);

        // Should not return Thai original; should fallback to EN translation
        org.junit.jupiter.api.Assertions.assertEquals("English Title", title);
        org.junit.jupiter.api.Assertions.assertEquals("English Summary", summary);
    }

    @Test
    @DisplayName("UTC-09-1: convertToDTO(news, lang) – Uses Chinese translation when lang=zh")
    void utc09_convertToDTO_usesChineseTranslation_whenLangZh() throws Exception {
        NewsController controller = new NewsController(newsRepository, languageDetectionService);

        News n = News.builder()
                .id(1L)
                .title("Original")
                .summary("Original summary")
                .titleZh("中文标题")
                .summaryZh("中文摘要")
                .titleEn("English Title")
                .summaryEn("English Summary")
                .build();

        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(true);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        Method m = NewsController.class.getDeclaredMethod("convertToDTO", News.class, String.class);
        m.setAccessible(true);
        Object dtoObj = m.invoke(controller, n, "zh");

        Method getTitle = dtoObj.getClass().getMethod("getTitle");
        Method getSummary = dtoObj.getClass().getMethod("getSummary");
        String title = (String) getTitle.invoke(dtoObj);
        String summary = (String) getSummary.invoke(dtoObj);

        org.junit.jupiter.api.Assertions.assertEquals("中文标题", title);
        org.junit.jupiter.api.Assertions.assertEquals("中文摘要", summary);
    }

    @Test
    @DisplayName("UTC-09-2: convertToDTO(news, lang) – Uses English translation when lang=en")
    void utc09_convertToDTO_usesEnglishTranslation_whenLangEn() throws Exception {
        NewsController controller = new NewsController(newsRepository, languageDetectionService);

        News n = News.builder()
                .id(2L)
                .title("Original")
                .summary("Original summary")
                .titleEn("English Title")
                .summaryEn("English Summary")
                .build();

        when(languageDetectionService.hasAnyThai(anyString())).thenReturn(false);
        when(languageDetectionService.containsChinese(anyString())).thenReturn(false);
        when(languageDetectionService.containsThai(anyString())).thenReturn(false);

        Method m = NewsController.class.getDeclaredMethod("convertToDTO", News.class, String.class);
        m.setAccessible(true);
        Object dtoObj = m.invoke(controller, n, "en");

        Method getTitle = dtoObj.getClass().getMethod("getTitle");
        Method getSummary = dtoObj.getClass().getMethod("getSummary");
        String title = (String) getTitle.invoke(dtoObj);
        String summary = (String) getSummary.invoke(dtoObj);

        org.junit.jupiter.api.Assertions.assertEquals("English Title", title);
        org.junit.jupiter.api.Assertions.assertEquals("English Summary", summary);
    }
}


