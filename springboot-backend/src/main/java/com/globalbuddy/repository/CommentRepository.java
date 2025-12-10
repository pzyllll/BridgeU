package com.globalbuddy.repository;

import com.globalbuddy.model.Comment;
import com.globalbuddy.model.CommunityPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, String> {
    List<Comment> findByPostOrderByCreatedAtDesc(CommunityPost post);
    @org.springframework.data.jpa.repository.Query("SELECT c FROM Comment c WHERE c.post.id = :postId")
    List<Comment> findByPostId(String postId);
    long countByPost(CommunityPost post);
}

