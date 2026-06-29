package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.User;
import com.ThanhND05.url_shortener.iam.enums.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository truy vấn bảng iam.users.
 * Các method tên theo convention Spring Data JPA → tự sinh query SQL.
 */
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    /** Tìm user bằng email — dùng cho đăng nhập. CITEXT DB xử lý case-insensitive. */
    Optional<User> findByEmail(String email);

    /** Kiểm tra email đã tồn tại chưa — dùng khi đăng ký. */
    boolean existsByEmail(String email);

    /** Lấy danh sách user theo status (có phân trang) — dùng cho admin dashboard. */
    Page<User> findByStatus(UserStatus status, Pageable pageable);

    /** Lấy tất cả user chưa bị xóa (status != DELETED). */
    Page<User> findByStatusNot(UserStatus status, Pageable pageable);
}
