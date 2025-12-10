package com.globalbuddy.service;

import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.CommunityPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NlpService {

    private final CommunityPostRepository postRepository;
    private final SemanticService semanticService;

    public QaResult answerQuestion(String question) {
        if (!StringUtils.hasText(question)) {
            throw new IllegalArgumentException("问题不能为空");
        }

        List<CommunityPost> latestPosts = postRepository.findAll().stream()
                .sorted(Comparator.comparing(CommunityPost::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(50)
                .collect(Collectors.toList());

        List<ScoredPost> scored = latestPosts.stream()
                .map(post -> new ScoredPost(
                        post,
                        semanticService.calculateScore(question, post.getTitle() + " " + post.getBody())
                ))
                .sorted(Comparator.comparingDouble(ScoredPost::getScore).reversed())
                .collect(Collectors.toList());

        List<ScoredPost> relevant = scored.stream()
                .filter(sp -> sp.getScore() > 0)
                .limit(3)
                .collect(Collectors.toList());

        List<ScoredPost> selected = relevant.isEmpty()
                ? scored.stream().limit(3).collect(Collectors.toList())
                : relevant;

        String answer = buildAnswer(selected);

        List<QaReference> references = selected.stream()
                .map(sp -> new QaReference(
                        sp.getPost().getId(),
                        sp.getPost().getTitle(),
                        sp.getScore()
                ))
                .collect(Collectors.toList());

        return new QaResult(answer, references);
    }

    private String buildAnswer(List<ScoredPost> posts) {
        if (posts.isEmpty()) {
            return "暂时没有找到相关的帖子，欢迎稍后再试试或发布新问题。";
        }
        StringBuilder builder = new StringBuilder("根据最近的社区讨论，总结如下：\n");
        for (int i = 0; i < posts.size(); i++) {
            CommunityPost post = posts.get(i).getPost();
            String snippet = post.getBody();
            if (snippet.length() > 120) {
                snippet = snippet.substring(0, 120) + "...";
            }
            builder.append(i + 1)
                    .append("、")
                    .append(post.getTitle())
                    .append("：")
                    .append(snippet)
                    .append("\n");
        }
        return builder.toString().trim();
    }

    private static class ScoredPost {
        private final CommunityPost post;
        private final double score;

        ScoredPost(CommunityPost post, double score) {
            this.post = post;
            this.score = score;
        }

        public CommunityPost getPost() {
            return post;
        }

        public double getScore() {
            return score;
        }
    }

    public static class QaResult {
        private final String answer;
        private final List<QaReference> references;

        public QaResult(String answer, List<QaReference> references) {
            this.answer = answer;
            this.references = references;
        }

        public String getAnswer() {
            return answer;
        }

        public List<QaReference> getReferences() {
            return references;
        }
    }

    public static class QaReference {
        private final String id;
        private final String title;
        private final double score;

        public QaReference(String id, String title, double score) {
            this.id = id;
            this.title = title;
            this.score = score;
        }

        public String getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public double getScore() {
            return score;
        }
    }
}
