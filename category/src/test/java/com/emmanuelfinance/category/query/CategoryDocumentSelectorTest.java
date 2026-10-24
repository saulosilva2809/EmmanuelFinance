package com.emmanuelfinance.category.query;

import com.emmanuelfinance.shared.modules.category.exceptions.CategoryNotFound;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryDocumentSelectorTest {

    @Mock
    private CategoryDocumentRepository categoryDocumentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CategoryDocumentSelector categoryDocumentSelector;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void shouldReturnTheCategoryOfTheCurrentUser() {
        CategoryDocument document = new CategoryDocument();
        document.setId(UUID.randomUUID());
        when(categoryDocumentRepository.findByIdAndUserIdAndDeletedFalse(document.getId(), userId))
                .thenReturn(Optional.of(document));

        assertEquals(document, categoryDocumentSelector.getCategoryById(document.getId()));
    }

    @Test
    void shouldThrowNotFoundWhenTheCategoryDoesNotExistForTheUser() {
        UUID id = UUID.randomUUID();
        when(categoryDocumentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(CategoryNotFound.class, () -> categoryDocumentSelector.getCategoryById(id));
    }
}
