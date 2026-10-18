package com.emmanuelfinance.account.dto.events;

import com.emmanuelfinance.account.Account;

// publicado quando uma conta é criada, alterada, restaurada ou tem o saldo atualizado
public record AccountChangedEventDTO(Account account) {}
