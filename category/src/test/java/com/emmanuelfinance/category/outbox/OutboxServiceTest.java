package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryMapper;
import com.emmanuelfinance.category.CategoryTestDataBuilder;
import com.emmanuelfinance.category.outbox.enums.CategoryOutboxEventType;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.emmanuelfinance.shared.outbox.dtos.OutboxEventSavedDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxServiceTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    private OutboxService outboxService;

    @BeforeEach
    void setUp() {
        OutboxEventFactory outboxEventFactory =
                new OutboxEventFactory(objectMapper, Mappers.getMapper(CategoryMapper.class));
        outboxService = new OutboxService(outboxEventRepository, applicationEventPublisher, outboxEventFactory);
    }

    @Test
    void shouldRegisterTheSnapshotWhenTheCategoryChanges() throws Exception {
        outboxService.categoryChanged(newCategory());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CategoryOutboxEventType.CATEGORY_CHANGED.name(), saved.get(0).getType().name());
        assertFalse(objectMapper.treeToValue(saved.get(0).getPayload(), CategoryDocument.class).isDeleted());
    }

    @Test
    void shouldRegisterTheSnapshotMarkedAsDeletedWhenTheCategoryIsDeleted() throws Exception {
        outboxService.categoryDeleted(newCategory());

        List<OutboxEvent> saved = registeredRows();
        assertEquals(1, saved.size());
        assertEquals(CategoryOutboxEventType.CATEGORY_CHANGED.name(), saved.get(0).getType().name());
        assertTrue(objectMapper.treeToValue(saved.get(0).getPayload(), CategoryDocument.class).isDeleted());
    }

    @Test
    void shouldWakeTheRelayOncePerOperation() {
        outboxService.categoryChanged(newCategory());

        verify(applicationEventPublisher, times(1)).publishEvent(any(OutboxEventSavedDTO.class));
    }

    @SuppressWarnings("unchecked")
    private List<OutboxEvent> registeredRows() {
        ArgumentCaptor<List<OutboxEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(outboxEventRepository, times(1)).saveAll(captor.capture());
        return captor.getValue();
    }

    private Category newCategory() {
        return CategoryTestDataBuilder.categoryEntity(CategoryTestDataBuilder.createCategoryDTO(), UUID.randomUUID(), false);
    }
}
