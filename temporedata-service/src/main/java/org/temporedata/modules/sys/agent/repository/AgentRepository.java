package org.temporedata.modules.sys.agent.repository;

import org.temporedata.modules.sys.agent.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AgentRepository extends JpaRepository<AgentEntity, String> {
    List<AgentEntity> findBySessionIdOrderByCreateDateTimeAsc(String sessionId);
    List<AgentEntity> findBySessionId(String sessionId);
    void deleteBySessionId(String sessionId);
}
