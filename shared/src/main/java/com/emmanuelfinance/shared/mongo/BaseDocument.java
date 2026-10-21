package com.emmanuelfinance.shared.mongo;

import jakarta.persistence.Id;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public abstract class BaseDocument {

    @Id
    private UUID id = UUID.randomUUID();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean deleted;
}
