package com.ThanhND05.url_shortener.iam.config;

import com.ThanhND05.url_shortener.iam.entity.Role;
import com.ThanhND05.url_shortener.iam.entity.User;
import com.ThanhND05.url_shortener.iam.entity.UserRole;
import com.ThanhND05.url_shortener.iam.enums.ScopeType;
import com.ThanhND05.url_shortener.iam.enums.UserStatus;
import com.ThanhND05.url_shortener.iam.repository.RoleRepository;
import com.ThanhND05.url_shortener.iam.repository.UserRepository;
import com.ThanhND05.url_shortener.iam.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.super-admin-email}")
    private String superAdminEmail;

    @Value("${app.admin.super-admin-password}")
    private String superAdminPassword;

    @Value("${app.admin.admin-email}")
    private String adminEmail;

    @Value("${app.admin.admin-password}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        createAdminUserIfNotFound(superAdminEmail, superAdminPassword, "Super Administrator", "super_admin");
        createAdminUserIfNotFound(adminEmail, adminPassword, "Administrator", "admin");
    }

    private void createAdminUserIfNotFound(String email, String password, String displayName, String roleName) {
        if (!userRepository.existsByEmail(email)) {
            log.info("Khởi tạo tài khoản quản trị viên: {}", email);
            User user = User.builder()
                    .email(email)
                    .passwordHash(passwordEncoder.encode(password))
                    .displayName(displayName)
                    .status(UserStatus.ACTIVE)
                    .build();

            user = userRepository.save(user);

            Role role = roleRepository.findByName(roleName).orElse(null);
            if (role != null) {
                UserRole userRole = UserRole.builder()
                        .userId(user.getId())
                        .role(role)
                        .scopeType(ScopeType.GLOBAL)
                        .build();
                userRoleRepository.save(userRole);
                log.info("Đã cấp role {} cho {}", roleName, email);
            } else {
                log.warn("Không tìm thấy role {} trong database, tài khoản {} chưa được cấp quyền!", roleName, email);
            }
        } else {
            log.debug("Tài khoản {} đã tồn tại, bỏ qua bước khởi tạo.", email);
        }
    }
}
