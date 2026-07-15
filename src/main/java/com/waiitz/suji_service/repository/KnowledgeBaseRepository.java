package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.KnowledgeBase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    List<KnowledgeBase> findByWorkspaceId(UUID workspaceId);

    long countByWorkspaceId(UUID workspaceId);

    List<KnowledgeBase> findByWorkspaceIdAndStatusNot(UUID workspaceId, String status);

    @Query("SELECT k FROM KnowledgeBase k WHERE k.id = :id AND k.status <> 'DELETED'")
    Optional<KnowledgeBase> findActiveById(@Param("id") UUID id);
}
