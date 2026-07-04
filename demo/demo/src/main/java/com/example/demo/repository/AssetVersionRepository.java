package com.example.demo.repository;

import com.example.demo.entity.AssetVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AssetVersionRepository extends JpaRepository<AssetVersion, String> {
    List<AssetVersion> findByAssetIdOrderByVersionNumberDesc(String assetId);
    Optional<AssetVersion> findTopByAssetIdOrderByVersionNumberDesc(String assetId);
}
