package com.ThanhND05.url_shortener.iam.api;

import com.ThanhND05.url_shortener.iam.entity.User;
import com.ThanhND05.url_shortener.iam.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IamPublicApiImpl implements IamPublicApi {
    private final UserRepository userRepository;

    @Override
    public Map<UUID, String> getUserEmails(Set<UUID> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();
        return userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getEmail));
    }

    @Override
    public String getUserEmail(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getEmail).orElse(null);
    }

    @Override
    public long countTotalUsers() {
        return userRepository.count();
    }

    @Override
    public long countUsersCreatedAfter(Instant time) {
        return userRepository.countByCreatedAtAfter(time);
    }
}
