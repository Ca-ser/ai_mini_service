package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.Document;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByKbId(UUID kbId);

    List<Document> findByParentId(UUID parentId);

    long countByKbId(UUID kbId);

    long countByKbIdAndStatusNot(UUID kbId, String status);

    List<Document> findByKbIdAndStatusNot(UUID kbId, String status);

    List<Document> findByParentIdAndStatusNot(UUID parentId, String status);

    @Query("SELECT d FROM Document d WHERE d.id = :id AND d.status <> 'DELETED'")
    Optional<Document> findActiveById(@Param("id") UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT d FROM Document d WHERE d.id = :id")
    Optional<Document> findByIdWithLock(@Param("id") UUID id);
}
