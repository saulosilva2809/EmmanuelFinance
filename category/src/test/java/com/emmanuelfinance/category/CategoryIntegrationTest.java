package com.emmanuelfinance.category;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.category.enums.TypeEnum;
import com.emmanuelfinance.shared.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
class CategoryIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Mock
    private SecurityUtils securityUtils;

    private UUID userId;
    private UUID account1;
    private UUID account2;

    @BeforeEach
    void setUp() {
        when(securityUtils.getCurrentUserId()).thenReturn(UUID.randomUUID());
        userId = securityUtils.getCurrentUserId();

        categoryRepository.deleteAll();

        account1 = UUID.randomUUID();
        account2 = UUID.randomUUID();

        Category cat1 = new Category();
        cat1.setUserId(userId);
        cat1.setAccountId(account1);
        cat1.setName("Salário Mensal");
        cat1.setType(TypeEnum.INCOME);

        Category cat2 = new Category();
        cat2.setUserId(userId);
        cat2.setAccountId(account1);
        cat2.setName("Salário Funcionários");
        cat2.setType(TypeEnum.EXPENSE);

        Category cat3 = new Category();
        cat3.setUserId(userId);
        cat3.setAccountId(account2);
        cat3.setName("Investimentos");
        cat3.setType(TypeEnum.INCOME);

        categoryRepository.save(cat1);
        categoryRepository.save(cat2);
        categoryRepository.save(cat3);
    }

    @Test
    void shouldListTheAccountsSuccessfully() {
        CategoryFiltersDTO fIltersDTO = new CategoryFiltersDTO(
                account1,
                "Salá",
                TypeEnum.INCOME
        );
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Category> spec = CategorySpecification.withFilter(fIltersDTO, userId);

        Page<Category> result = categoryRepository.findAll(spec, pageable);

        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Salário Mensal", result.getContent().get(0).getName());
        assertEquals(TypeEnum.INCOME, result.getContent().get(0).getType());
        assertEquals(account1, result.getContent().get(0).getAccountId());
    }

    @Test
    void shouldReturnABlankPage() {
        CategoryFiltersDTO fIltersDTO = new CategoryFiltersDTO(
                account2,
                null,
                TypeEnum.EXPENSE
        );
        Pageable pageable = PageRequest.of(0, 10);
        Specification<Category> spec = CategorySpecification.withFilter(fIltersDTO, userId);

        Page<Category> result = categoryRepository.findAll(spec, pageable);

        assertNotNull(result);
        assertTrue(result.getContent().isEmpty());
        assertEquals(0, result.getTotalElements());
    }
}
