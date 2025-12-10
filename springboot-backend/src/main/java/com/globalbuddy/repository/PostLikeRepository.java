package com.globalbuddy.repository;

import com.globalbuddy.model.CommunityPost;
import com.globalbuddy.model.PostLike;
import com.globalbuddy.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, String> {
    Optional<PostLike> findByPostAndUser(CommunityPost post, AppUser user);
    @org.springframework.data.jpa.repository.Query("SELECT pl FROM PostLike pl WHERE pl.post.id = :postId")
    List<PostLike> findByPostId(String postId);
    long countByPost(CommunityPost post);
    boolean existsByPostAndUser(CommunityPost post, AppUser user);
}

