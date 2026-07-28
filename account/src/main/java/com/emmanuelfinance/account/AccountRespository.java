package com.emmanuelfinance.account;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AccountRespository extends JpaRepository<Account, UUID>, JpaSpecificationExecutor<Account> {

}