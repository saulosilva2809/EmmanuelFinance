package com.emmanuelfinance.category.query;

import com.emmanuelfinance.shared.mongo.BaseProjector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CategoryProjector implements BaseProjector<CategoryDocument> {

    private final CategoryDocumentRepository categoryDocumentRepository;

    @Override
    // apaga os caches relacionados para avitar conflitos
    @Caching(evict = {
            @CacheEvict(value = "category_summary", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "category_internal_summary", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "category_view", key = "#snapshot.userId + ':' + #snapshot.id"),
            @CacheEvict(value = "category_list", allEntries = true),
            @CacheEvict(value = "transaction_view", allEntries = true),
            @CacheEvict(value = "transaction_list", allEntries = true),
            @CacheEvict(value = "invoice_item_list", allEntries = true)
    })
    public void project(CategoryDocument snapshot) {
        categoryDocumentRepository.save(snapshot);
        log.info("Categoria {} sincronizada no Mongo", snapshot.getId());
    }
}
