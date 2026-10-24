package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.outbox.enums.CategoryOutboxEventType;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.emmanuelfinance.category.query.CategoryProjector;
import com.emmanuelfinance.shared.outbox.BaseOutboxEvent;
import com.emmanuelfinance.shared.outbox.OutboxDispatcher;
import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CategoryOutboxDispatcher implements OutboxDispatcher {

    private final ObjectMapper objectMapper;
    private final CategoryProjector categoryProjector;

    @Override
    public boolean supports(OutboxEventType type) {
        return CategoryOutboxEventType.CATEGORY_CHANGED.is(type);
    }

    @Override
    public void dispatch(BaseOutboxEvent outboxEvent) throws JsonProcessingException {
        CategoryDocument snapshot = objectMapper.treeToValue(outboxEvent.getPayload(), CategoryDocument.class);
        categoryProjector.project(snapshot);
    }
}
