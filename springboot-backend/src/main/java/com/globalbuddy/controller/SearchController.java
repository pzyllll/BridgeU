package com.globalbuddy.controller;

import com.globalbuddy.dto.CommunityResponse;
import com.globalbuddy.dto.PostResponse;
import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommunityRepository;
import com.globalbuddy.service.SemanticService;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final SemanticService semanticService;

    @GetMapping
    public SearchResponse search(@RequestParam("q") String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("查询参数 q 不能为空");
        }

        List<CommunityResponseWithScore> communityScores = new ArrayList<>();
        for (Community community : communityRepository.findAll()) {
            double score = semanticService.calculateScore(query, buildCommunityText(community));
            if (score > 0) {
                communityScores.add(new CommunityResponseWithScore(toCommunityResponse(community), score));
            }
        }
        communityScores.sort(Comparator.comparingDouble(CommunityResponseWithScore::getScore).reversed());

        List<PostResponseWithScore> postScores = new ArrayList<>();
        for (CommunityPost post : postRepository.findAll()) {
            double score = semanticService.calculateScore(query, post.getTitle() + " " + post.getBody());
            if (score > 0) {
                postScores.add(new PostResponseWithScore(toPostResponse(post), score));
            }
        }
        postScores.sort(Comparator.comparingDouble(PostResponseWithScore::getScore).reversed());

        List<CommunityResponse> communityResult = communityScores.stream()
                .limit(10)
                .map(CommunityResponseWithScore::toResponse)
                .collect(Collectors.toList());
        List<PostResponse> postResult = postScores.stream()
                .limit(10)
                .map(PostResponseWithScore::toResponse)
                .collect(Collectors.toList());

        return new SearchResponse(query, communityResult, postResult);
    }

    private String buildCommunityText(Community community) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.hasText(community.getTitle())) {
            builder.append(community.getTitle()).append(" ");
        }
        if (StringUtils.hasText(community.getDescription())) {
            builder.append(community.getDescription()).append(" ");
        }
        if (community.getTags() != null) {
            builder.append(String.join(" ", community.getTags()));
        }
        return builder.toString();
    }

    private CommunityResponse toCommunityResponse(Community community) {
        return new CommunityResponse(
                community.getId(),
                community.getTitle(),
                community.getDescription(),
                community.getCountry(),
                community.getLanguage(),
                community.getTags(),
                community.getCreatedBy() != null ? community.getCreatedBy().getId() : null,
                community.getCreatedAt()
        );
    }

    private PostResponse toPostResponse(CommunityPost post) {
        return new PostResponse(
                post.getId(),
                post.getCommunity() != null ? post.getCommunity().getId() : null,
                post.getAuthor() != null ? post.getAuthor().getId() : null,
                post.getTitle(),
                post.getBody(),
                post.getTags(),
                post.getCategory(),
                post.getEmbedding(),
                post.getCreatedAt(),
                post.getUpdatedAt()
        );
    }

    public static class SearchResponse {
        private final String query;
        private final List<CommunityResponse> communities;
        private final List<PostResponse> posts;

        public SearchResponse(String query, List<CommunityResponse> communities, List<PostResponse> posts) {
            this.query = query;
            this.communities = communities;
            this.posts = posts;
        }

        public String getQuery() {
            return query;
        }

        public List<CommunityResponse> getCommunities() {
            return communities;
        }

        public List<PostResponse> getPosts() {
            return posts;
        }
    }

    private static class CommunityResponseWithScore {
        private final CommunityResponse response;
        private final double score;

        CommunityResponseWithScore(CommunityResponse response, double score) {
            this.response = response;
            this.score = score;
        }

        double getScore() {
            return score;
        }

        CommunityResponse toResponse() {
            return response;
        }
    }

    private static class PostResponseWithScore {
        private final PostResponse response;
        private final double score;

        PostResponseWithScore(PostResponse response, double score) {
            this.response = response;
            this.score = score;
        }

        double getScore() {
            return score;
        }

        PostResponse toResponse() {
            return response;
        }
    }
}
