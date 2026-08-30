package com.emmanuelfinance.account.services;

import com.emmanuelfinance.account.*;
import com.emmanuelfinance.account.dto.*;
import com.emmanuelfinance.account.exceptions.RestoreAccountError;
import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryInternalDTO;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.account.kafka.AccountEventPublisher;
import com.emmanuelfinance.shared.modules.account.kafka.account.enums.StatusEventEnum;
import com.emmanuelfinance.shared.modules.account.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
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

        return accountMapper.toResponseDTO(savedAccount);
    }

    public ResponseAccountDTO view(UUID uuid) {
        Account account = accountSelector.getAccountByIdAndUserId(uuid);

        return accountMapper.toResponseDTO(account);
    }

    @WithDeletedFilter(enabled = true)
    public PageResponseDTO<ResponseAccountDTO> list(AccountFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Account> specification = AccountSpecification.withFilter(filters, userId, false);
        Page<Account> page = accountRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseAccountDTO> dtoPage = page.map(accountMapper::toResponseDTO);
        return PageResponseDTO.from(dtoPage);
    }

    @WithDeletedFilter(enabled = false)
    public PageResponseDTO<ResponseAccountDTO> listDeleted(AccountFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Account> specification = AccountSpecification.withFilter(filters, userId, true);

        Page<Account> page = accountRepository.findAll(specification, pageable);
        Page<ResponseAccountDTO> dtoPage = page.map(accountMapper::toResponseDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#uuid")
    public ResponseAccountDTO update(UUID uuid, UpdateAccountDTO data) {
        Account account = accountSelector.getAccountByIdAndUserId(uuid);

        accountMapper.updateAccountFromDTO(data, account);

        Account savedAccount = accountRepository.save(account);

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
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public void restore(UUID id) {
        Account account = accountSelector.getAccountByIdIncludingDeleted(id);

        if (!account.isDeleted()) {
            throw new RestoreAccountError();
        }

        account.setDeleted(false);
        accountRepository.save(account);

        accountEventPublisher.publishAccount(new AccountEventDTO(
                account.getId(),
                account.getUserId(),
                StatusEventEnum.RESTORE
        ));
    }
}
