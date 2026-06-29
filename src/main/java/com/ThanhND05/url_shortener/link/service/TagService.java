package com.ThanhND05.url_shortener.link.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.link.dto.request.CreateTagRequest;
import com.ThanhND05.url_shortener.link.dto.response.TagResponse;
import com.ThanhND05.url_shortener.link.entity.Tag;
import com.ThanhND05.url_shortener.link.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý tags — nhãn phân loại cho short links.
 * Mỗi tag thuộc về một user cụ thể (scoped by ownerId).
 */
@Service
@RequiredArgsConstructor
public class TagService {

    private final TagRepository tagRepository;

    @Transactional
    public TagResponse createTag(UUID ownerId, CreateTagRequest request) {
        if (tagRepository.existsByOwnerIdAndNameIgnoreCase(ownerId, request.name())) {
            throw new DuplicateResourceException("Tag", "name", request.name());
        }
        Tag tag = Tag.builder().ownerId(ownerId).name(request.name().trim()).build();
        tag = tagRepository.save(tag);
        return TagResponse.from(tag);
    }

    @Transactional(readOnly = true)
    public List<TagResponse> listTags(UUID ownerId) {
        return tagRepository.findByOwnerId(ownerId).stream()
                .map(TagResponse::from).collect(Collectors.toList());
    }

    @Transactional
    public void deleteTag(Long tagId, UUID ownerId) {
        Tag tag = tagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", tagId));
        if (!tag.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Tag", "id", tagId);
        }
        tagRepository.delete(tag);
    }
}
