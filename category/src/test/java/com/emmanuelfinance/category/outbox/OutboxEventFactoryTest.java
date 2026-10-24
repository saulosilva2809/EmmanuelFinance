package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.Category;
import com.emmanuelfinance.category.CategoryMapper;
import com.emmanuelfinance.category.CategoryTestDataBuilder;
import com.emmanuelfinance.category.outbox.enums.CategoryOutboxEventType;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class OutboxEventFactoryTest {

    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();
    private final OutboxEventFactory outboxEventFactory =
            new OutboxEventFactory(objectMapper, Mappers.getMapper(CategoryMapper.class));

    @Test
    void shouldBuildTheCategorySnapshotWithTheWholeCategory() throws Exception {
        Category category = newCategory();

        OutboxEvent row = outboxEventFactory.categorySnapshot(category, false);

        assertEquals(CategoryOutboxEventType.CATEGORY_CHANGED.name(), row.getType().name());
        assertEquals(category.getId().toString(), row.getAggregateId());
        assertNotNull(row.getCreatedAt());

        CategoryDocument snapshot = objectMapper.treeToValue(row.getPayload(), CategoryDocument.class);
        assertEquals(category.getId(), snapshot.getId());
        assertEquals(category.getUserId(), snapshot.getUserId());
        assertEquals(category.getName(), snapshot.getName());
        assertEquals(category.getType(), snapshot.getType());
        assertEquals(category.getCreatedAt(), snapshot.getCreatedAt());
        assertFalse(snapshot.isDeleted());
    }

    // a categoria apagada ainda está com deleted=false na entidade, então o aviso precisa dizer que foi apagada
    @Test
    void shouldMarkTheSnapshotAsDeletedWhenAskedTo() throws Exception {
        Category category = newCategory();

        OutboxEvent row = outboxEventFactory.categorySnapshot(category, true);

        assertFalse(category.isDeleted());
        assertTrue(objectMapper.treeToValue(row.getPayload(), CategoryDocument.class).isDeleted());
    }

    private Category newCategory() {
        return CategoryTestDataBuilder.categoryEntity(CategoryTestDataBuilder.createCategoryDTO(), UUID.randomUUID(), false);
    }
}
