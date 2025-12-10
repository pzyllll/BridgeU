package com.globalbuddy.service;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.ParseException;

import java.io.IOException;

/**
 * 爬虫服务示例
 * 演示如何使用 Jsoup 和 Apache HttpClient 进行网页爬取
 */
@Service
public class CrawlerService {

    /**
     * 使用 Jsoup 直接连接并解析网页
     */
    public String crawlWithJsoup(String url) throws IOException {
        Document doc = Jsoup.connect(url)
                .userAgent("Mozilla/5.0")
                .timeout(10000)
                .get();
        
        // 提取标题
        String title = doc.title();
        
        // 提取正文内容（示例：提取所有段落）
        String body = doc.body().text();
        
        return String.format("标题: %s\n内容: %s", title, body.substring(0, Math.min(500, body.length())));
    }

    /**
     * 使用 Apache HttpClient 获取网页内容，然后用 Jsoup 解析
     */
    public String crawlWithHttpClient(String url) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            request.setHeader("User-Agent", "Mozilla/5.0");
            
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String html;
                try {
                    html = EntityUtils.toString(response.getEntity());
                } catch (ParseException e) {
                    throw new IOException("解析 HTTP 响应失败", e);
                }
                
                // 使用 Jsoup 解析 HTML
                Document doc = Jsoup.parse(html);
                String title = doc.title();
                String body = doc.body().text();
                
                return String.format("标题: %s\n内容: %s", title, body.substring(0, Math.min(500, body.length())));
            }
        }
    }
}

