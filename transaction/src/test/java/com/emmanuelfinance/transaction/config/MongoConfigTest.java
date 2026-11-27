package com.emmanuelfinance.transaction.config;

import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MongoConfigTest {

    @Test
    void shouldWriteTheValueAsDecimal128SoItCanBeComparedAsANumber() {
        Decimal128 written = new MongoConfig.BigDecimalToDecimal128().convert(new BigDecimal("1234.50"));

        assertEquals(new Decimal128(new BigDecimal("1234.50")), written);
    }

    @Test
    void shouldReadDecimal128BackWithoutLosingTheDecimalPlaces() {
        BigDecimal read = new MongoConfig.Decimal128ToBigDecimal()
                .convert(new Decimal128(new BigDecimal("1234.50")));

        assertEquals(new BigDecimal("1234.50"), read);
    }
}
