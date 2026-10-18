package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.Account;
import com.emmanuelfinance.account.AccountMapper;
import com.emmanuelfinance.account.AccountRepository;
import com.emmanuelfinance.account.AccountSelector;
import com.emmanuelfinance.account.dto.CreateAccountDTO;
import com.emmanuelfinance.account.dto.ResponseAccountDTO;
import com.emmanuelfinance.account.dto.UpdateAccountDTO;
import com.emmanuelfinance.account.dto.events.AccountChangedEventDTO;
import com.emmanuelfinance.account.dto.events.AccountDeletedEventDTO;
import com.emmanuelfinance.account.exceptions.AccountDomainException;
import com.emmanuelfinance.account.exceptions.AccountErrorCode;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.account.kafka.AccountEventPublisher;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountEventPublisher accountEventPublisher;
    private final SecurityUtils securityUtils;
    private final AccountCache accountCache;
    private final AccountSelector accountSelector;
    private final ApplicationEventPublisher applicationEventPublisher;

    @Transactional
    public ResponseAccountDTO create(CreateAccountDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountMapper.toEntity(data);
        account.setUserId(userId);
        account.setCurrentBalance(data.initialBalance());

        Account savedAccount = accountRepository.save(account);
        accountEventPublisher.publishAccount(new AccountEventDTO(
                account.getId(),
                account.getUserId(),
                StatusEventEnum.CREATED
        ));
        accountCache.saveAccountOwner(account.getId(), account.getUserId());
        applicationEventPublisher.publishEvent(new AccountChangedEventDTO(savedAccount));

        return accountMapper.toResponseDTO(savedAccount);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#uuid")
    public ResponseAccountDTO update(UUID uuid, UpdateAccountDTO data) {
        Account account = accountSelector.getAccountByIdAndUserId(uuid);

        accountMapper.updateAccountFromDTO(data, account);

        Account savedAccount = accountRepository.save(account);
        applicationEventPublisher.publishEvent(new AccountChangedEventDTO(savedAccount));

        return accountMapper.toResponseDTO(savedAccount);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public void delete(UUID id) {
        Account account = accountSelector.getAccountByIdAndUserId(id);

        accountRepository.delete(account);
        accountEventPublisher.publishAccount(new AccountEventDTO(
                account.getId(),
                account.getUserId(),
                StatusEventEnum.DELETED
        ));
        applicationEventPublisher.publishEvent(new AccountDeletedEventDTO(account.getId()));
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public void restore(UUID id) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(id);

        if (!account.isDeleted()) {
            throw new AccountDomainException(AccountErrorCode.RESTORE_ACCOUNT_NOT_DELETED);
        }

        account.setDeleted(false);
        accountRepository.save(account);

        accountEventPublisher.publishAccount(new AccountEventDTO(
                account.getId(),
                account.getUserId(),
                StatusEventEnum.RESTORE
        ));
        applicationEventPublisher.publishEvent(new AccountChangedEventDTO(account));
    }
}
