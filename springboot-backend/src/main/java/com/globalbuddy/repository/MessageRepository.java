package com.globalbuddy.repository;

import com.globalbuddy.model.Conversation;
import com.globalbuddy.model.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, String> {
    List<Message> findByConversationOrderByCreatedAtAsc(Conversation conversation);
    long countByConversationAndIsReadFalseAndReceiver(Conversation conversation, com.globalbuddy.model.AppUser receiver);
}

