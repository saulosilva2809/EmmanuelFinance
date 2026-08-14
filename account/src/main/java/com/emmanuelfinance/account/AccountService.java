package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.*;
import com.emmanuelfinance.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.annotation.WithDeletedFilter;
import com.emmanuelfinance.shared.modules.account.AccountCache;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import com.emmanuelfinance.account.kafka.producer.AccountEventPublisher;
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
    private final UserClientCacheService userClientCacheService;
    private final AccountMapper accountMapper;
    private final AccountEventPublisher accountEventPublisher;
    private final SecurityUtils securityUtils;
    private final AccountCache accountCache;

    private ResponseAccountDTO accountAsDTO(Account data) {
        UserSummaryDTO userData = userClientCacheService.getUserById(data.getUserId());

        return new ResponseAccountDTO(
                data.getId(),
                userData,
                data.getName(),
                data.getType(),
                data.getInitialBalance(),
                data.getCurrentBalance(),
                data.getCreatedAt(),
                data.getUpdatedAt(),
                data.isDeleted()
        );
    }

    private Account getAccountByIdAndUserId(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();

        Account account = accountRepository.findByIdAndUserIdAndDeletedFalse(id, userId)
                .orElseThrow(() -> new AccountNotFound());

        return account;
    }

    @Transactional
    public ResponseAccountDTO create(CreateAccountDTO data) {
        UUID userId = securityUtils.getCurrentUserId();

        Account newAccount = new Account();

        newAccount.setUserId(userId);
        newAccount.setName(data.name());
        newAccount.setType(data.type());
        newAccount.setInitialBalance(data.initialBalance());
        newAccount.setCurrentBalance(data.initialBalance());

        Account savedAccount = accountRepository.save(newAccount);
        accountEventPublisher.publishAccount(new AccountEventDTO(
                newAccount.getId(),
                newAccount.getUserId(),
                StatusEventEnum.CREATED
        ));
        accountCache.saveAccountOwner(newAccount.getId(), newAccount.getUserId());

        return accountAsDTO(savedAccount);
    }

    public ResponseAccountDTO view(UUID uuid) {
        Account account = getAccountByIdAndUserId(uuid);

        return accountAsDTO(account);
    }

    @WithDeletedFilter(enabled = true)
    public PageResponseDTO<ResponseAccountDTO> list(AccountFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Account> specification = AccountSpecification.withFilter(filters, userId, false);
        Page<Account> page = accountRepository.findAll(
                specification,
                pageable
        );

        Page<ResponseAccountDTO> dtoPage = page.map(this::accountAsDTO);
        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#uuid")
    public ResponseAccountDTO update(UUID uuid, UpdateAccountDTO data) {
        Account account = getAccountByIdAndUserId(uuid);

        accountMapper.updateAccountFromDTO(data, account);

        Account savedAccount = accountRepository.save(account);

        return accountAsDTO(savedAccount);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public void delete(UUID id) {
        Account account = getAccountByIdAndUserId(id);

        accountRepository.delete(account);
        accountEventPublisher.publishAccount(new AccountEventDTO(
                account.getId(),
                account.getUserId(),
                StatusEventEnum.DELETED
        ));
    }

    public AccountSummaryDTO getAccountSummary(UUID id) {
        UUID userId = securityUtils.getCurrentUserId();
        Account account = accountRepository.findByIdAndUserIdIncludingDeleted(id, userId)
                .orElseThrow(() -> new AccountNotFound());

        return new AccountSummaryDTO(account.getId(), account.getName(), account.isDeleted());
    }

    @WithDeletedFilter(enabled = false)
    public PageResponseDTO<ResponseAccountDTO> listDeleted(AccountFiltersDTO filters, Pageable pageable) {
        UUID userId = securityUtils.getCurrentUserId();

        Specification<Account> specification = AccountSpecification.withFilter(filters, userId, true);

        Page<Account> page = accountRepository.findAll(specification, pageable);
        Page<ResponseAccountDTO> dtoPage = page.map(this::accountAsDTO);

        return PageResponseDTO.from(dtoPage);
    }
}
