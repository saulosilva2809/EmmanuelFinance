package com.emmanuelfinance.creditcard.creditcard.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class CreditCardProjector implements BaseProjector<CreditCardDocument> {

    private final CreditCardDocumentRepository creditCardDocumentRepository;

    @Override
    public void project(CreditCardDocument snapshot) {
        creditCardDocumentRepository.save(snapshot);
        log.info("Cartão {} sincronizado no Mongo", snapshot.getId());
    }
}
