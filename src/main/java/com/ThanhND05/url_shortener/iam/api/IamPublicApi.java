package com.ThanhND05.url_shortener.iam.api;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface IamPublicApi {
    Map<UUID, String> getUserEmails(Set<UUID> userIds);
    String getUserEmail(UUID userId);
    long countTotalUsers();
    long countUsersCreatedAfter(Instant time);
}
