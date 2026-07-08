package com.waiitz.suji_service.repository;

import com.waiitz.suji_service.model.entity.CacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CacheEntryRepository extends JpaRepository<CacheEntry, String> {
}
