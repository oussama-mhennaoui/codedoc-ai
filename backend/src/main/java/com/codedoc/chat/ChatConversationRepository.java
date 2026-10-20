package com.codedoc.chat;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatConversationRepository extends JpaRepository<ChatConversation, Long> {

    @Query("SELECT c FROM ChatConversation c WHERE c.project.id = :projectId AND c.user.id = :userId ORDER BY c.updatedAt DESC")
    List<ChatConversation> findByProjectIdAndUserIdOrderByUpdatedAtDesc(
            @Param("projectId") Long projectId,
            @Param("userId") Long userId);
}
