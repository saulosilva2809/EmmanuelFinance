package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.enums.TypeEnum;
import com.emmanuelfinance.account.dto.events.AccountChangedEventDTO;
import com.emmanuelfinance.account.dto.events.AccountDeletedEventDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountProjectorTest {

    @Mock
    private AccountDocumentRepository accountDocumentRepository;

    @Spy
    private AccountMapper accountMapper = Mappers.getMapper(AccountMapper.class);

    @InjectMocks
    private AccountProjector accountProjector;

    @Test
    void shouldSaveTheDocumentWhenAccountChanges() {
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUserId(UUID.randomUUID());
        account.setName("Caixa");
        account.setType(TypeEnum.CASH);
        account.setCurrentBalance(new BigDecimal("50.00"));

        accountProjector.onAccountChanged(new AccountChangedEventDTO(account));

        ArgumentCaptor<AccountDocument> captor = ArgumentCaptor.forClass(AccountDocument.class);
        verify(accountDocumentRepository).save(captor.capture());
        assertEquals(account.getId(), captor.getValue().getId());
        assertEquals("Caixa", captor.getValue().getName());
        assertEquals(new BigDecimal("50.00"), captor.getValue().getCurrentBalance());
        assertFalse(captor.getValue().isDeleted());
    }

    @Test
    void shouldMarkTheDocumentAsDeletedWhenAccountIsDeleted() {
        AccountDocument document = new AccountDocument();
        document.setId(UUID.randomUUID());
        when(accountDocumentRepository.findById(document.getId())).thenReturn(Optional.of(document));

        accountProjector.onAccountDeleted(new AccountDeletedEventDTO(document.getId()));

        assertTrue(document.isDeleted());
        verify(accountDocumentRepository).save(document);
    }

    @Test
    void shouldDoNothingWhenDeletedAccountIsNotInMongo() {
        UUID id = UUID.randomUUID();
        when(accountDocumentRepository.findById(id)).thenReturn(Optional.empty());

        accountProjector.onAccountDeleted(new AccountDeletedEventDTO(id));

        verify(accountDocumentRepository, never()).save(any());
    }
}
