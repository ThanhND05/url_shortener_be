package com.ThanhND05.url_shortener.iam.service;

import com.ThanhND05.url_shortener.common.exception.*;
import com.ThanhND05.url_shortener.iam.dto.request.*;
import com.ThanhND05.url_shortener.iam.dto.response.*;
import com.ThanhND05.url_shortener.iam.entity.*;
import com.ThanhND05.url_shortener.iam.enums.ScopeType;
import com.ThanhND05.url_shortener.iam.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service quản lý Role và Permission.
 *
 * Chức năng chính:
 * - Liệt kê roles + permissions.
 * - Tạo custom role (is_system = false), gán permissions.
 * - Gán role cho user (có scope: GLOBAL hoặc WORKSPACE).
 * - Tải effective permissions của user (flatten từ tất cả roles).
 *
 * Lưu ý: System roles (super_admin, admin, member, viewer) không được xóa/sửa tên.
 */
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final UserRoleRepository userRoleRepository;
    private final UserRepository userRepository;

    // ── LIỆT KÊ ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream()
                .map(RoleResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    // ── TẠO CUSTOM ROLE ───────────────────────────────────

    /** Tạo role mới (is_system = false) với danh sách permission slugs. */
    @Transactional
    public RoleResponse createRole(String name, String displayName, String description,
                                   Set<String> permissionSlugs) {
        if (roleRepository.findByName(name).isPresent()) {
            throw new DuplicateResourceException("Role", "name", name);
        }

        Role role = Role.builder()
                .name(name)
                .displayName(displayName)
                .description(description)
                .isSystem(false)
                .permissions(resolvePermissions(permissionSlugs))
                .build();
        role = roleRepository.save(role);
        return RoleResponse.from(role);
    }

    // ── GÁN PERMISSIONS CHO ROLE ──────────────────────────

    /** Cập nhật danh sách permissions của role (thay thế toàn bộ). */
    @Transactional
    public RoleResponse updateRolePermissions(Long roleId, Set<String> permissionSlugs) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
        role.setPermissions(resolvePermissions(permissionSlugs));
        role = roleRepository.save(role);
        return RoleResponse.from(role);
    }

    // ── GÁN ROLE CHO USER ─────────────────────────────────

    /** Gán một role cho user trong scope cụ thể. */
    @Transactional
    public void assignRoleToUser(UUID userId, AssignRoleRequest request, UUID grantedBy) {
        // Validate user tồn tại
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        // Tìm role
        Role role = roleRepository.findByName(request.roleName())
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", request.roleName()));

        ScopeType scopeType = ScopeType.valueOf(request.scopeType());

        // Tạo user-role assignment
        UserRole userRole = UserRole.builder()
                .userId(userId)
                .role(role)
                .scopeType(scopeType)
                .scopeId(request.scopeId())
                .grantedBy(grantedBy)
                .build();
        userRoleRepository.save(userRole);
    }

    // ── EFFECTIVE PERMISSIONS ──────────────────────────────

    /**
     * Tải tất cả permission slugs hiệu lực của user.
     * Flatten: user → user_roles (active) → role → permissions → slug.
     */
    @Transactional(readOnly = true)
    public Set<String> getEffectivePermissions(UUID userId) {
        return userRoleRepository.findByUserId(userId).stream()
                .filter(UserRole::isActive)
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(Permission::toSlug)
                .collect(Collectors.toSet());
    }

    // ── HELPER ────────────────────────────────────────────

    /** Parse danh sách slug "resource:action" → Set<Permission> entities. */
    private Set<Permission> resolvePermissions(Set<String> slugs) {
        return slugs.stream().map(slug -> {
            String[] parts = slug.split(":");
            if (parts.length != 2) {
                throw new BusinessException("Permission slug không hợp lệ: " + slug);
            }
            return permissionRepository.findByResourceAndAction(parts[0], parts[1])
                    .orElseThrow(() -> new ResourceNotFoundException("Permission", "slug", slug));
        }).collect(Collectors.toSet());
    }
}
