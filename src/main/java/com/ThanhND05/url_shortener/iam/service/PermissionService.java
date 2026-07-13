package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.entity.Permission;
import com.ThanhND05.url_shortener.iam.repository.PermissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service quản lý Permission — CRUD nghiệp vụ cho quyền hành động.
 *
 * Chức năng chính:
 * - Liệt kê tất cả permissions.
 * - Xem chi tiết permission.
 * - Tạo permission mới (cặp resource:action phải unique).
 * - Chỉnh sửa permission (partial update, kiểm tra unique khi đổi resource/action).
 * - Xóa permission (kiểm tra không thuộc role nào trước khi xóa).
 *
 * Lưu ý: Khi xóa permission, nếu permission đang được gán cho role,
 * cần remove khỏi role trước hoặc dùng force delete.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionService {

    private final PermissionRepository permissionRepository;

    // ── LIỆT KÊ ──────────────────────────────────────────

    /** Lấy danh sách tất cả permissions. */
    @Transactional(readOnly = true)
    public List<PermissionResponse> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(PermissionResponse::from)
                .collect(Collectors.toList());
    }

    // ── XEM CHI TIẾT ──────────────────────────────────────

    /** Lấy chi tiết permission theo ID. */
    @Transactional(readOnly = true)
    public PermissionResponse getPermissionById(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));
        return PermissionResponse.from(permission);
    }

    // ── TẠO MỚI ──────────────────────────────────────────

    /**
     * Tạo permission mới.
     * Kiểm tra cặp (resource, action) phải là duy nhất.
     */
    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest request) {
        String resource = request.resource().trim().toLowerCase();
        String action = request.action().trim().toLowerCase();

        // Kiểm tra trùng
        if (permissionRepository.existsByResourceAndAction(resource, action)) {
            throw new DuplicateResourceException("Permission", "slug", resource + ":" + action);
        }

        Permission permission = Permission.builder()
                .resource(resource)
                .action(action)
                .description(request.description())
                .build();
        permission = permissionRepository.save(permission);

        log.info("Permission created: {}:{}", resource, action);
        return PermissionResponse.from(permission);
    }

    // ── CHỈNH SỬA ────────────────────────────────────────

    /**
     * Cập nhật permission — partial update.
     * Nếu thay đổi resource hoặc action, kiểm tra cặp mới phải unique.
     */
    @Transactional
    public PermissionResponse updatePermission(Long id, UpdatePermissionRequest request) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        String newResource = (request.resource() != null && !request.resource().isBlank())
                ? request.resource().trim().toLowerCase()
                : permission.getResource();
        String newAction = (request.action() != null && !request.action().isBlank())
                ? request.action().trim().toLowerCase()
                : permission.getAction();

        // Nếu resource hoặc action thay đổi, kiểm tra unique
        boolean slugChanged = !newResource.equals(permission.getResource())
                || !newAction.equals(permission.getAction());
        if (slugChanged && permissionRepository.existsByResourceAndAction(newResource, newAction)) {
            throw new DuplicateResourceException("Permission", "slug", newResource + ":" + newAction);
        }

        permission.setResource(newResource);
        permission.setAction(newAction);

        if (request.description() != null) {
            permission.setDescription(request.description());
        }

        permission = permissionRepository.save(permission);
        log.info("Permission updated: {} → {}:{}", id, newResource, newAction);
        return PermissionResponse.from(permission);
    }

    // ── XÓA ──────────────────────────────────────────────

    /**
     * Xóa permission theo ID.
     *
     * Lưu ý: JPA sẽ tự xóa bản ghi trong bảng trung gian role_permissions
     * nhờ cascade. Nếu DB có FK constraint ON DELETE RESTRICT, sẽ cần
     * remove permission khỏi tất cả roles trước.
     */
    @Transactional
    public void deletePermission(Long id) {
        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Permission", "id", id));

        permissionRepository.delete(permission);
        log.info("Permission deleted: {} ({}:{})", id, permission.getResource(), permission.getAction());
    }
}
