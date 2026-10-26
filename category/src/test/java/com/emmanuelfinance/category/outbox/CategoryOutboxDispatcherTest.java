package com.emmanuelfinance.category.outbox;

import com.emmanuelfinance.category.outbox.enums.CategoryOutboxEventType;
import com.emmanuelfinance.category.query.CategoryDocument;
import com.emmanuelfinance.category.query.CategoryProjector;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryOutboxDispatcherTest {

    @Mock
    private CategoryProjector categoryProjector;

    @Spy
    private ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @InjectMocks
    private CategoryOutboxDispatcher dispatcher;

    @Test
    void shouldSupportOnlyTheCategoryChangedType() {
        assertTrue(dispatcher.supports(CategoryOutboxEventType.CATEGORY_CHANGED));
        assertFalse(dispatcher.supports(() -> "OUTRO_TIPO"));
    }

    // do banco o tipo volta como texto, e não como o enum
    @Test
    void shouldSupportTheTypeWhenItComesBackAsPlainText() {
        assertTrue(dispatcher.supports(() -> "CATEGORY_CHANGED"));
    }

    @Test
    void shouldApplyTheSnapshotInMongo() throws Exception {
        CategoryDocument snapshot = newSnapshot();

        dispatcher.dispatch(row(snapshot));

        verify(categoryProjector).project(argThat(received ->
                received.getId().equals(snapshot.getId())
                        && received.getName().equals("Salário")
                        && received.getType() == TypeTransactionEnum.INCOME
                        && received.isDeleted()));
    }

    @Test
    void shouldLetTheFailureOfMongoGoUpSoTheRowStaysPending() {
        doThrow(new IllegalStateException("mongo fora do ar")).when(categoryProjector).project(any());

        assertThrows(IllegalStateException.class, () -> dispatcher.dispatch(row(newSnapshot())));
    }

    private CategoryDocument newSnapshot() {
        CategoryDocument snapshot = new CategoryDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setUserId(UUID.randomUUID());
        snapshot.setName("Salário");
        snapshot.setType(TypeTransactionEnum.INCOME);
        snapshot.setDeleted(true);
        return snapshot;
    }

    private OutboxEvent row(CategoryDocument snapshot) {
        OutboxEvent row = new OutboxEvent();
        row.setType(CategoryOutboxEventType.CATEGORY_CHANGED);
        row.setAggregateId(snapshot.getId().toString());
        row.setPayload(objectMapper.valueToTree(snapshot));
        return row;
    }
}
