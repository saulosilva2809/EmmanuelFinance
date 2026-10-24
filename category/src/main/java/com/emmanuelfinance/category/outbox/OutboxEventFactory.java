package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryMapper;
import com.emmanuelfinance.category.outbox.enums.CategoryOutboxEventType;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private final ObjectMapper objectMapper;
    private final CategoryMapper categoryMapper;

    public OutboxEvent categorySnapshot(Category category, boolean deleted) {
        CategoryDocument snapshot = categoryMapper.toDocument(category);
        snapshot.setDeleted(deleted);

        return create(CategoryOutboxEventType.CATEGORY_CHANGED, category.getId(), snapshot);
    }

    private OutboxEvent create(CategoryOutboxEventType type, UUID categoryId, Object payload) {
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setType(type);
        outboxEvent.setAggregateId(categoryId.toString());
        outboxEvent.setPayload(objectMapper.valueToTree(payload));
        outboxEvent.setCreatedAt(LocalDateTime.now());

        return outboxEvent;
    }
}
