package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.DocumentVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DocumentVersionRepository extends JpaRepository<DocumentVersion, UUID> {

    List<DocumentVersion> findByDocumentIdOrderByVersionNoDesc(UUID documentId);

    Optional<DocumentVersion> findByDocumentIdAndVersionNo(UUID documentId, Integer versionNo);

}
