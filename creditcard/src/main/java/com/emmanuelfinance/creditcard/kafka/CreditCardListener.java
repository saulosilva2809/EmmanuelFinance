package com.emmanuelfinance.creditcard.kafka;

import com.emmanuelfinance.creditcard.services.CreditCardInternalService;
import com.emmanuelfinance.shared.modules.account.kafka.account.AccountEventDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CreditCardListener {

    private final CreditCardInternalService creditCardInternalService;

    @KafkaListener(topics = "account-events", groupId = "credit-card-service-group")
    public void handleAccountDeleted(AccountEventDTO event) {
        log.info("Recebido evento de transação criada para a conta: {}", event.accountId());

        try {
            creditCardInternalService.deactivateCardsByAccountId(event.accountId());
        } catch (Exception e) {
            log.error("Erro ao processar atualização de saldo para o evento de criação: {}", event, e);
        }
    }
}