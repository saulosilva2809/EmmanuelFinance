package com.emmanuelfinance.category.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryProjectorTest {

    @Mock
    private CategoryDocumentRepository categoryDocumentRepository;

    @InjectMocks
    private CategoryProjector categoryProjector;

    @Test
    void shouldSaveTheSnapshotInMongo() {
        CategoryDocument snapshot = new CategoryDocument();
        snapshot.setId(UUID.randomUUID());

        categoryProjector.project(snapshot);

        verify(categoryDocumentRepository).save(snapshot);
    }

    @Test
    void shouldSaveASnapshotMarkedAsDeletedWithoutAnyExtraStep() {
        CategoryDocument snapshot = new CategoryDocument();
        snapshot.setId(UUID.randomUUID());
        snapshot.setDeleted(true);

        categoryProjector.project(snapshot);

        verify(categoryDocumentRepository).save(snapshot);
    }
}
