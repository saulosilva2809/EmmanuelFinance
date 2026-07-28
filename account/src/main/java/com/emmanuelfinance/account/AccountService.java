package com.emmanuelfinance.account;

import com.emmanuelfinance.account.dto.*;
import com.emmanuelfinance.account.exceptions.AccountNotFound;
import com.emmanuelfinance.shared.dto.AccountSummaryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.dto.UserSummaryDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRespository accountRespository;
    private final UserClientCacheService userClientCacheService;
    private final AccountMapper accountMapper;

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
                data.getUpdatedAt()
        );
    }

    private Account getAccountById(UUID id) {
        Account account = accountRespository.findById(id)
                .orElseThrow(() -> new AccountNotFound());

        return account;
    }

    public ResponseAccountDTO create(Jwt jwt, CreateAccountDTO data) {
        UUID userId = UUID.fromString(jwt.getSubject());

        Account newAccount = new Account();

        newAccount.setUserId(userId);
        newAccount.setName(data.name());
        newAccount.setType(data.type());
        newAccount.setInitialBalance(data.initialBalance());
        newAccount.setCurrentBalance(data.initialBalance());

        Account savedAccount = accountRespository.save(newAccount);

        return accountAsDTO(savedAccount);
    }

    public ResponseAccountDTO view(UUID uuid) {
        Account account = getAccountById(uuid);

        return accountAsDTO(account);
    }

    public PageResponseDTO<ResponseAccountDTO> list(AccountFiltersDTO filters, Pageable pageable) {
        Specification<Account> specification = AccountSpecification.withFilter(filters);

        Page<Account> page = accountRespository.findAll(
                specification,
                pageable
        );

        Page<ResponseAccountDTO> dtoPage = page.map(this::accountAsDTO);

        return PageResponseDTO.from(dtoPage);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#uuid")
    public ResponseAccountDTO update(UUID uuid, UpdateAccountDTO data) {
        Account account = getAccountById(uuid);

        accountMapper.updateAccountFromDTO(data, account);

        Account savedAccount = accountRespository.save(account);

        return accountAsDTO(savedAccount);
    }

    public void delete(UUID id) {
        Account account = getAccountById(id);

        accountRespository.delete(account);
    }

    public AccountSummaryDTO getInternalAccount(UUID id) {
        Account account = getAccountById(id);
        return new AccountSummaryDTO(account.getId(), account.getName());
    }
}
