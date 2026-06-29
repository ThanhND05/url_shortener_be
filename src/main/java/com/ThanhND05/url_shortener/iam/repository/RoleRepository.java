package com.ThanhND05.url_shortener.iam.repository;

import com.ThanhND05.url_shortener.iam.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {
    /** Tìm role bằng slug name — VD: findByName("member"). */
    Optional<Role> findByName(String name);
}
