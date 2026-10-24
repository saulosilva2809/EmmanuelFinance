package com.emmanuelfinance.category.query;

import com.emmanuelfinance.category.CategoryMapper;
import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.dto.ResponseCategoryDTO;
import com.emmanuelfinance.shared.dto.PageResponseDTO;
import com.emmanuelfinance.shared.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryQueryService {

    private final CategoryDocumentSelector categoryDocumentSelector;
    private final MongoTemplate mongoTemplate;
    private final CategoryMapper categoryMapper;
    private final SecurityUtils securityUtils;

    public ResponseCategoryDTO view(UUID categoryId) {
        CategoryDocument category = categoryDocumentSelector.getCategoryById(categoryId);
        return categoryMapper.toResponseDTO(category);
    }

    public PageResponseDTO<ResponseCategoryDTO> list(CategoryFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, false);
    }

    public PageResponseDTO<ResponseCategoryDTO> listDeleted(CategoryFiltersDTO filters, Pageable pageable) {
        return search(filters, pageable, true);
    }

    private PageResponseDTO<ResponseCategoryDTO> search(CategoryFiltersDTO filters, Pageable pageable, boolean deleted) {
        // captura o userId
        UUID userId = securityUtils.getCurrentUserId();

        // cria a query
        Query query = CategorySpecification.withFilter(filters, userId, deleted);

        // conta o total de registros que atendem o critério
        long total = mongoTemplate.count(query, CategoryDocument.class);

        // faz a busca dos documentos usando mongoTemplate (mais flexível para buscas com filtros)
        List<CategoryDocument> documents = mongoTemplate.find(query.with(pageable), CategoryDocument.class);

        // cria a estrutura da página e converte cada documento em um DTO de resposta do mesmo
        Page<ResponseCategoryDTO> page = new PageImpl<>(documents, pageable, total)
                .map(categoryMapper::toResponseDTO);

        // converte a página em um DTO
        return PageResponseDTO.from(page);
    }
}
