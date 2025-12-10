package com.globalbuddy.controller;

import com.globalbuddy.service.NlpService;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nlp")
@RequiredArgsConstructor
public class NlpController {

    private final NlpService nlpService;

    @PostMapping("/qa")
    public ResponseEntity<?> answerQuestion(@RequestBody QuestionRequest request) {
        if (!org.springframework.util.StringUtils.hasText(request.getQuestion())) {
            return ResponseEntity.badRequest().body(java.util.Collections.singletonMap("error", "question 必填"));
        }
        return ResponseEntity.ok(nlpService.answerQuestion(request.getQuestion()));
    }

    @Data
    public static class QuestionRequest {
        @NotBlank
        private String question;
    }
}

