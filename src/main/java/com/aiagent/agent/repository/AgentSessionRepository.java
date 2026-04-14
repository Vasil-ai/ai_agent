package com.aiagent.agent.repository;

import com.aiagent.agent.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AgentSessionRepository extends JpaRepository<AgentSession, String> {

    List<AgentSession> findByUserIdOrderByUpdatedAtDesc(String userId);

    @Query("SELECT s FROM AgentSession s ORDER BY s.updatedAt DESC")
    List<AgentSession> findAllOrderByUpdatedAtDesc();
}
