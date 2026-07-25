package com.ThanhND05.url_shortener.platform.repository;

import com.ThanhND05.url_shortener.platform.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {

    /**
     * Lấy tất cả config, sắp xếp theo key.
     */
    List<SystemConfig> findAllByOrderByConfigKeyAsc();
}
