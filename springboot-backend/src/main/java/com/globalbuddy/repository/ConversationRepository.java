package com.globalbuddy.repository;

import com.globalbuddy.model.AppUser;
import com.globalbuddy.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {
    // Find conversation between two users
    Optional<Conversation> findByUser1AndUser2(AppUser user1, AppUser user2);
    Optional<Conversation> findByUser2AndUser1(AppUser user1, AppUser user2);
    
    // Find conversations by user (either user1 or user2)
    List<Conversation> findByUser1OrderByLastMessageAtDesc(AppUser user);
    List<Conversation> findByUser2OrderByLastMessageAtDesc(AppUser user);
}
