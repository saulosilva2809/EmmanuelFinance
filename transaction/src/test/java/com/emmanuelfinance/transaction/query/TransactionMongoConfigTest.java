package com.emmanuelfinance.transaction.query;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TransactionMongoConfigTest {

    @Test
    void shouldWriteTheValueAsDecimal128SoItCanBeComparedAsANumber() {
        Decimal128 written = new TransactionMongoConfig.BigDecimalToDecimal128().convert(new BigDecimal("1234.50"));

        assertEquals(new Decimal128(new BigDecimal("1234.50")), written);
    }

    @Test
    void shouldReadDecimal128BackWithoutLosingTheDecimalPlaces() {
        BigDecimal read = new TransactionMongoConfig.Decimal128ToBigDecimal()
                .convert(new Decimal128(new BigDecimal("1234.50")));

        assertEquals(new BigDecimal("1234.50"), read);
    }
}
