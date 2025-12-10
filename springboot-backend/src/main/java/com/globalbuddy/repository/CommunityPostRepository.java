package com.globalbuddy.repository;

import com.globalbuddy.model.Community;
import com.globalbuddy.model.CommunityPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommunityPostRepository extends JpaRepository<CommunityPost, String> {
    List<CommunityPost> findAllByOrderByCreatedAtDesc();
    List<CommunityPost> findByCommunityOrderByCreatedAtDesc(Community community);
    
    // 按状态查询帖子（分页）
    Page<CommunityPost> findByStatus(CommunityPost.Status status, Pageable pageable);
    
    // 按状态统计帖子数量
    long countByStatus(CommunityPost.Status status);
    
    // 按用户查询帖子
    List<CommunityPost> findByAuthorIdOrderByCreatedAtDesc(String authorId);
    
    // 按状态查询帖子（不分页）
    List<CommunityPost> findByStatusOrderByCreatedAtDesc(CommunityPost.Status status);
    
    // 按标题查询帖子（用于检查是否已存在）
    List<CommunityPost> findByTitle(String title);
}

