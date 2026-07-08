package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.CollabUpdate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface CollabUpdateRepository extends JpaRepository<CollabUpdate, UUID> {
}
