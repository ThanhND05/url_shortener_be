package com.ThanhND05.url_shortener.link.repository;

import com.ThanhND05.url_shortener.link.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByOwnerId(UUID ownerId);

    Optional<Tag> findByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);
}
