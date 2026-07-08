package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.DocumentSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentSnapshotRepository extends JpaRepository<DocumentSnapshot, UUID> {

    List<DocumentSnapshot> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

}
