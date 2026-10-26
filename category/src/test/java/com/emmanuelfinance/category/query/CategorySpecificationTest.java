package com.emmanuelfinance.category.query;

import com.emmanuelfinance.category.dto.CategoryFiltersDTO;
import com.emmanuelfinance.shared.enums.TypeTransactionEnum;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class CategorySpecificationTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldAlwaysFilterByUserAndDeletedState() {
        Document conditions = conditions(new CategoryFiltersDTO(null, null), false);

        assertEquals(2, conditions.size());
        assertEquals(userId, conditions.get("userId"));
        assertEquals(false, conditions.get("deleted"));
    }

    @Test
    void shouldFilterOnlyDeletedCategoriesWhenAskedTo() {
        Document conditions = conditions(new CategoryFiltersDTO(null, null), true);

        assertEquals(true, conditions.get("deleted"));
    }

    @Test
    void shouldFilterByType() {
        Document conditions = conditions(new CategoryFiltersDTO(null, TypeTransactionEnum.EXPENSE), false);

        assertEquals(TypeTransactionEnum.EXPENSE, conditions.get("type"));
    }

    @Test
    void shouldSearchTheNameIgnoringCase() {
        Document conditions = conditions(new CategoryFiltersDTO("sal", null), false);

        Pattern pattern = (Pattern) conditions.get("name");
        assertTrue(pattern.matcher("Meu SALARIO").find());
    }

    @Test
    void shouldTreatRegexCharactersInTheNameAsPlainText() {
        Document conditions = conditions(new CategoryFiltersDTO("a.b", null), false);

        Pattern pattern = (Pattern) conditions.get("name");
        assertTrue(pattern.matcher("categoria a.b").find());
        assertFalse(pattern.matcher("categoria axb").find());
    }

    @Test
    void shouldIgnoreABlankName() {
        Document conditions = conditions(new CategoryFiltersDTO("   ", null), false);

        assertNull(conditions.get("name"));
    }

    // os critérios são de campos diferentes, então o mongo junta tudo num documento só
    private Document conditions(CategoryFiltersDTO filters, boolean onlyDeleted) {
        return CategorySpecification.withFilter(filters, userId, onlyDeleted).getQueryObject();
    }
}
