package com.emmanuelfinance.shared.clients;

import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "user-server", url = "${application.config.user-service-url}")
@ConditionalOnProperty(name = "application.config.user-service-url")
public interface UserClient {

    @GetMapping("/auth/users/{id}")
    UserSummaryDTO getUserById(@PathVariable UUID id);
}
