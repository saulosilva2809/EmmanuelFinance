package com.emmanuelfinance.account.query;

import com.emmanuelfinance.account.dto.AccountFiltersDTO;
import com.emmanuelfinance.account.enums.TypeEnum;
import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.query.Query;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class AccountSpecificationTest {

    private final UUID userId = UUID.randomUUID();

    @Test
    void shouldAlwaysFilterByUserAndDeletedState() {
        Document filter = AccountSpecification.withFilter(new AccountFiltersDTO(null, null), userId, false)
                .getQueryObject();

        assertEquals(userId, filter.get("userId"));
        assertEquals(false, filter.get("deleted"));
        assertFalse(filter.containsKey("name"));
        assertFalse(filter.containsKey("type"));
    }

    @Test
    void shouldFilterOnlyDeletedAccountsWhenAskedTo() {
        Document filter = AccountSpecification.withFilter(new AccountFiltersDTO(null, null), userId, true)
                .getQueryObject();

        assertEquals(true, filter.get("deleted"));
    }

    @Test
    void shouldSearchTheNameAsATextIgnoringCaseAndNotAsARegex() {
        Query query = AccountSpecification.withFilter(new AccountFiltersDTO("c6 (bank)", null), userId, false);

        Pattern pattern = (Pattern) query.getQueryObject().get("name");
        assertEquals(Pattern.quote("c6 (bank)"), pattern.pattern());
        assertTrue((pattern.flags() & Pattern.CASE_INSENSITIVE) != 0);
        assertTrue(pattern.matcher("Minha Conta C6 (Bank)").find());
    }

    @Test
    void shouldIgnoreABlankName() {
        Document filter = AccountSpecification.withFilter(new AccountFiltersDTO("   ", null), userId, false)
                .getQueryObject();

        assertFalse(filter.containsKey("name"));
    }

    @Test
    void shouldFilterByType() {
        Document filter = AccountSpecification.withFilter(new AccountFiltersDTO(null, TypeEnum.SAVINGS), userId, false)
                .getQueryObject();

        assertEquals(TypeEnum.SAVINGS, filter.get("type"));
    }
}
