package com.smartassist.repository;

import com.smartassist.model.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {
    List<Message> findByConversationIdOrderByIdAsc(Long conversationId);

    /** The 20 most recent messages, newest first. This is the context sent to the model. */
    List<Message> findTop20ByConversationIdOrderByIdDesc(Long conversationId);

    void deleteByConversationId(Long conversationId);
}
