package com.globalbuddy.controller;

import com.globalbuddy.dto.CommunityResponse;
import com.globalbuddy.dto.PostResponse;
import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommunityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;

    @GetMapping
    public SearchResponse search(@RequestParam("q") String query) {
        if (!StringUtils.hasText(query)) {
            throw new IllegalArgumentException("查询参数 q 不能为空");
        }
        final String needle = query.trim().toLowerCase();

        List<CommunityResponse> communityResult = new ArrayList<>();
        for (Community community : communityRepository.findAll()) {
            String haystack = buildCommunityText(community).toLowerCase();
            if (haystack.contains(needle)) {
                communityResult.add(toCommunityResponse(community));
            }
        }

        List<PostResponse> postResult = new ArrayList<>();
        for (CommunityPost post : postRepository.findAll()) {
            String haystack = ((post.getTitle() == null ? "" : post.getTitle()) + " " +
                    (post.getBody() == null ? "" : post.getBody())).toLowerCase();
            if (haystack.contains(needle)) {
                postResult.add(toPostResponse(post));
            }
        }
        communityResult = communityResult.stream().limit(10).collect(Collectors.toList());
        postResult = postResult.stream().limit(10).collect(Collectors.toList());

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

}
