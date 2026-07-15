package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.DocumentSnapshot;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, UUID> {

    List<DocumentSnapshot> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM DocumentSnapshot s WHERE s.documentId = :documentId ORDER BY s.versionNo DESC")
    List<DocumentSnapshot> findByDocumentIdOrderByVersionNoDescWithLock(@Param("documentId") UUID documentId);
}
