package com.emmanuelfinance.transaction.transaction.services;

import com.emmanuelfinance.shared.modules.payment.InvoicePaymentFailedEvent;
import com.emmanuelfinance.shared.modules.transaction.transaction.enums.StatusTransactionEnum;
import com.emmanuelfinance.shared.modules.transaction.transaction.exceptions.TransactionNotFound;
import com.emmanuelfinance.shared.modules.transaction.transaction.kafka.dto.TransactionFailedEvent;
import com.emmanuelfinance.transaction.transaction.Transaction;
import com.emmanuelfinance.transaction.transaction.TransactionRepository;
import com.emmanuelfinance.transaction.transaction.outbox.TransactionOutbox;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionFailureService {

    private final TransactionRepository transactionRepository;
    private final TransactionOutbox transactionOutbox;

    // a conta ou o cartão não conseguiu aplicar a operação, então a transação volta a mostrar o que de fato aconteceu
    @Transactional
    public void compensate(TransactionFailedEvent event) {
        // busca sem filtrar as excluídas, porque a falha pode ser justamente de uma exclusão
        Transaction transaction = transactionRepository.findById(event.transactionId())
                .orElseThrow(TransactionNotFound::new);

        switch (event.operation()) {
            case CREATE -> fail(transaction);
            case UPDATE -> revertUpdate(transaction, event);
            case DELETE -> transaction.setDeleted(false);
            case RESTORE -> transaction.setDeleted(true);
        }

        transactionRepository.saveAndFlush(transaction);

        if (transaction.isDeleted()) {
            transactionOutbox.transactionDeleted(transaction);
        } else {
            transactionOutbox.transactionChanged(transaction);
        }
    }

    private void fail(Transaction transaction) {
        if (StatusTransactionEnum.FAILED.equals(transaction.getStatus())) {
            return;
        }

        transaction.setStatus(StatusTransactionEnum.FAILED);

        // pagamento de fatura que não saiu da conta: o cartão precisa desfazer a baixa da fatura
        if (transaction.getInvoiceId() != null) {
            transactionOutbox.invoicePaymentFailedEvent(new InvoicePaymentFailedEvent(
                    transaction.getId(),
                    transaction.getCreditCardId(),
                    transaction.getInvoiceId(),
                    transaction.getAmount()
            ));
        }
    }

    // o saldo continua com o valor antigo, então os dados financeiros voltam a ser os antigos
    private void revertUpdate(Transaction transaction, TransactionFailedEvent event) {
        if (!StatusTransactionEnum.PAID.equals(event.oldStatus())) {
            fail(transaction);
            return;
        }

        transaction.setAccountId(event.oldAccountId());
        transaction.setAmount(event.oldAmount());
        transaction.setType(event.oldType());
    }
}
