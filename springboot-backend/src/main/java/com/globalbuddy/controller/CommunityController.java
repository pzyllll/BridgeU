package com.globalbuddy.controller;

import com.globalbuddy.dto.CommunityPostRequest;
import com.globalbuddy.dto.CommunityRequest;
import com.globalbuddy.dto.CommunityResponse;
import com.globalbuddy.dto.PostResponse;
import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.repository.AppUserRepository;
import com.globalbuddy.repository.CommunityPostRepository;
import com.globalbuddy.repository.CommunityRepository;
import com.globalbuddy.service.LanguageDetectionService;
import com.globalbuddy.service.TranslationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/communities")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityRepository communityRepository;
    private final CommunityPostRepository postRepository;
    private final AppUserRepository userRepository;
    private final LanguageDetectionService languageDetectionService;
    private final TranslationService translationService;

    @GetMapping
    public List<CommunityResponse> listCommunities(
            @RequestParam(required = false) String country,
            @RequestParam(required = false) String language
    ) {
        List<Community> communities;
        boolean hasCountry = StringUtils.hasText(country);
        boolean hasLanguage = StringUtils.hasText(language);
        if (hasCountry && hasLanguage) {
            communities = communityRepository.findByCountryAndLanguageOrderByCreatedAtDesc(country, language);
        } else if (hasCountry) {
            communities = communityRepository.findByCountryOrderByCreatedAtDesc(country);
        } else {
            communities = communityRepository.findAllByOrderByCreatedAtDesc();
        }
        return communities.stream().map(this::toCommunityResponse).collect(Collectors.toList());
    }

    @PostMapping
    public ResponseEntity<CommunityResponse> createCommunity(@Valid @RequestBody CommunityRequest request) {
        Community community = Community.create(
                request.getTitle(),
                request.getDescription(),
                request.getCountry(),
                request.getLanguage()
        );
        community.setTags(request.getTags());

        if (request.getCreatedBy() != null) {
            Optional<AppUser> creator = userRepository.findById(request.getCreatedBy());
            creator.ifPresent(community::setCreatedBy);
        }

        Community saved = communityRepository.save(community);
        return ResponseEntity.status(HttpStatus.CREATED).body(toCommunityResponse(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CommunityResponse> getCommunity(@PathVariable String id) {
        return communityRepository.findById(id)
                .map(community -> ResponseEntity.ok(toCommunityResponse(community)))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}/posts")
    public ResponseEntity<List<PostResponse>> listPosts(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "en") String lang) {
        return communityRepository.findById(id)
                .map(community -> {
                    List<PostResponse> posts = postRepository.findByCommunityOrderByCreatedAtDesc(community)
                            .stream()
                            .map(post -> toPostResponse(post, lang))
                            .collect(Collectors.toList());
                    return ResponseEntity.ok(posts);
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/posts")
    public ResponseEntity<PostResponse> createPost(
            @PathVariable String id,
            @Valid @RequestBody CommunityPostRequest request
    ) {
        Optional<Community> communityOpt = communityRepository.findById(id);
        if (!communityOpt.isPresent()) {
            return ResponseEntity.notFound().build();
        }
        Optional<AppUser> authorOpt = userRepository.findById(request.getAuthorId());
        if (!authorOpt.isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        // Detect language of the post content
        String combinedText = (request.getTitle() != null ? request.getTitle() + " " : "") + 
                             (request.getBody() != null ? request.getBody() : "");
        String detectedLang = languageDetectionService.detectLanguage(combinedText);
        log.info("Detected language for post: {} (title: {})", detectedLang, request.getTitle());

        CommunityPost post = new CommunityPost();
        post.setId(java.util.UUID.randomUUID().toString());
        post.setCommunity(communityOpt.get());
        post.setAuthor(authorOpt.get());
        post.setTitle(request.getTitle());
        post.setBody(request.getBody());
        post.setTags(request.getTags());
        post.setCategory(request.getCategory());
        post.setOriginalLanguage(detectedLang);

        // Auto-translate to Chinese and English
        try {
            TranslationService.TranslationResult translationResult = 
                translationService.translateContent(request.getTitle(), request.getBody(), detectedLang);
            
            // Set Chinese title translation
            if (translationResult.getTitleZh() != null && !translationResult.getTitleZh().isEmpty()) {
                post.setTitleZh(translationResult.getTitleZh());
            } else if ("zh".equals(detectedLang)) {
                post.setTitleZh(request.getTitle());
            }
            
            // Set Chinese content translation (with fallback to original if translation fails)
            if (translationResult.getBodyZh() != null && !translationResult.getBodyZh().isEmpty()) {
                post.setContentZh(translationResult.getBodyZh());
                log.info("✅ Chinese translation completed for post: {}", request.getTitle());
            } else {
                // Fallback: if detected language is Chinese, use original; otherwise use original as fallback
                if ("zh".equals(detectedLang)) {
                    post.setContentZh(request.getBody());
                } else {
                    post.setContentZh(request.getBody()); // Fallback to original
                    log.warn("⚠️ Chinese translation failed, using original content as fallback");
                }
            }

            // Set English title translation
            if (translationResult.getTitleEn() != null && !translationResult.getTitleEn().isEmpty()) {
                post.setTitleEn(translationResult.getTitleEn());
            } else if ("en".equals(detectedLang)) {
                post.setTitleEn(request.getTitle());
            }

            // Set English content translation (with fallback to original if translation fails)
            if (translationResult.getBodyEn() != null && !translationResult.getBodyEn().isEmpty()) {
                post.setContentEn(translationResult.getBodyEn());
                log.info("✅ English translation completed for post: {}", request.getTitle());
            } else {
                // Fallback: if detected language is English, use original; otherwise use original as fallback
                if ("en".equals(detectedLang)) {
                    post.setContentEn(request.getBody());
                } else {
                    post.setContentEn(request.getBody()); // Fallback to original
                    log.warn("⚠️ English translation failed, using original content as fallback");
                }
            }
        } catch (Exception e) {
            log.error("❌ Translation failed for post: {} - {}", request.getTitle(), e.getMessage(), e);
            // Fallback: use original content for both languages
            post.setContentZh(request.getBody());
            post.setContentEn(request.getBody());
        }

        CommunityPost saved = postRepository.save(post);
        return ResponseEntity.status(HttpStatus.CREATED).body(toPostResponse(saved, "en"));
    }

    private CommunityResponse toCommunityResponse(Community community) {
        String creatorId = community.getCreatedBy() != null ? community.getCreatedBy().getId() : null;
        return new CommunityResponse(
                community.getId(),
                community.getTitle(),
                community.getDescription(),
                community.getCountry(),
                community.getLanguage(),
                community.getTags(),
                creatorId,
                community.getCreatedAt()
        );
    }

    private PostResponse toPostResponse(CommunityPost post) {
        return toPostResponse(post, "en");
    }

    private PostResponse toPostResponse(CommunityPost post, String lang) {
        String title = post.getTitle();
        String body = post.getBody();

        // Return translated title and content based on language preference
        if ("zh".equals(lang)) {
            // Use Chinese translation if available
            if (post.getTitleZh() != null && !post.getTitleZh().isEmpty()) {
                title = post.getTitleZh();
            }
            if (post.getContentZh() != null && !post.getContentZh().isEmpty()) {
                body = post.getContentZh();
            }
            log.debug("Returning Chinese translation for post: {}", post.getId());
        } else if ("en".equals(lang)) {
            // Use English translation if available
            if (post.getTitleEn() != null && !post.getTitleEn().isEmpty()) {
                title = post.getTitleEn();
            }
            if (post.getContentEn() != null && !post.getContentEn().isEmpty()) {
                body = post.getContentEn();
            }
            log.debug("Returning English translation for post: {}", post.getId());
        } else {
            // Fallback to original content
            log.debug("Using original content for post: {} (lang: {})", post.getId(), lang);
        }

        return new PostResponse(
                post.getId(),
                post.getCommunity() != null ? post.getCommunity().getId() : null,
                post.getAuthor() != null ? post.getAuthor().getId() : null,
                title,
                body,
                post.getTags(),
                post.getCategory(),
                post.getEmbedding(),
                post.getCreatedAt(),
                post.getUpdatedAt(),
                post.getContentZh(),
                post.getContentEn(),
                post.getOriginalLanguage()
        );
    }
}

