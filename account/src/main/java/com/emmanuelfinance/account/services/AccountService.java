package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.account.outbox.OutboxService;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final OutboxService outboxService;
    private final SecurityUtils securityUtils;
    private final AccountSelector accountSelector;

    @Transactional
    public ResponseAccountDTO create(CreateAccountDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountMapper.toEntity(data);
        account.setUserId(userId);
        account.setCurrentBalance(data.initialBalance());

        Account savedAccount = accountRepository.save(account);
        outboxService.accountCreated(savedAccount);

        return accountMapper.toResponseDTO(savedAccount);
    }

    @Transactional
    public ResponseAccountDTO update(UUID uuid, UpdateAccountDTO data) {
        Account account = accountSelector.getAccountByIdAndUserId(uuid);

        accountMapper.updateAccountFromDTO(data, account);
        Account savedAccount = accountRepository.saveAndFlush(account);

        outboxService.accountUpdated(savedAccount);
        return accountMapper.toResponseDTO(savedAccount);
    }

    @Transactional
    public void delete(UUID id) {
        Account account = accountSelector.getAccountByIdAndUserId(id);

        accountRepository.delete(account);
        outboxService.accountDeleted(account);
    }

    @Transactional
    public void restore(UUID id) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(id);

        if (!account.isDeleted()) {
            throw new AccountDomainException(AccountErrorCode.RESTORE_ACCOUNT_NOT_DELETED);
        }

        account.setDeleted(false);
        accountRepository.saveAndFlush(account);
        outboxService.accountRestored(account);
    }
}
