package com.waiitz.ai_mini_service.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(schema = "pub", name = "publication_tasks")
public class PublicationTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "document_id", nullable = false)
    private UUID documentId;

    @Column(name = "snapshot_id")
    private UUID snapshotId;

    @Column(name = "channel_id", nullable = false)
    private UUID channelId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(length = 500)
    private String digest;

    @Column(length = 100)
    private String author;

    @Column(name = "cover_asset_id")
    private UUID coverAssetId;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(name = "external_draft_id", length = 100)
    private String externalDraftId;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
