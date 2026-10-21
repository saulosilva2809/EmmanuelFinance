package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.shared.modules.creditcard.exceptions.CreditCardNotFound;
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
class CreditCardDocumentSelectorTest {

    @Mock
    private CreditCardDocumentRepository creditCardDocumentRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CreditCardDocumentSelector creditCardDocumentSelector;

    private final UUID userId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(userId);
    }

    @Test
    void shouldReturnTheCreditCardOfTheCurrentUser() {
        CreditCardDocument document = new CreditCardDocument();
        document.setId(UUID.randomUUID());
        when(creditCardDocumentRepository.findByIdAndUserIdAndDeletedFalse(document.getId(), userId))
                .thenReturn(Optional.of(document));

        assertEquals(document, creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(document.getId()));
    }

    @Test
    void shouldThrowNotFoundWhenTheCreditCardDoesNotExistForTheUser() {
        UUID id = UUID.randomUUID();
        when(creditCardDocumentRepository.findByIdAndUserIdAndDeletedFalse(id, userId)).thenReturn(Optional.empty());

        assertThrows(CreditCardNotFound.class, () -> creditCardDocumentSelector.getByIdAndUserIdAndDeletedFalse(id));
    }
}
