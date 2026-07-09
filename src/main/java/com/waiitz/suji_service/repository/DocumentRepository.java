package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findByKbId(UUID kbId);

    List<Document> findByParentId(UUID parentId);

    long countByKbId(UUID kbId);

    long countByKbIdAndStatusNot(UUID kbId, String status);

    List<Document> findByKbIdAndStatusNot(UUID kbId, String status);

    List<Document> findByParentIdAndStatusNot(UUID parentId, String status);

}
