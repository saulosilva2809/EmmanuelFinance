package com.emmanuelfinance.category.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryProjector implements BaseProjector<CategoryDocument> {

    private final CategoryDocumentRepository categoryDocumentRepository;

    @Override
    public void project(CategoryDocument snapshot) {
        categoryDocumentRepository.save(snapshot);
        log.info("Categoria {} sincronizada no Mongo", snapshot.getId());
    }
}
