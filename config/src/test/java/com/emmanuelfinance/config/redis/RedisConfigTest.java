package com.emmanuelfinance.config.redis;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigTest {

    enum Kind { CHECKING, SAVINGS }

    record Item(UUID id, Kind kind, BigDecimal balance, LocalDateTime createdAt, LocalDateTime updatedAt, boolean deleted) {}

    record Page(List<Item> content, int page, int size, long totalElements, int totalPages) {}

    private final RedisCacheConfiguration configuration = new RedisConfig().cacheConfiguration();

    @Test
    void shouldKeepCachedEntriesForTenHours() {
        assertEquals(Duration.ofHours(10), configuration.getTtlFunction().getTimeToLive(Object.class, null));
    }

    @Test
    void shouldNotCacheNullValues() {
        assertFalse(configuration.getAllowCacheNullValues());
    }

    // as respostas das listas são records com data, dinheiro e enum: se a ida e a volta mudar algo, o cache devolve lixo
    @Test
    void shouldReadBackExactlyWhatWasCachedForAPageOfRecords() {
        Item item = new Item(UUID.randomUUID(), Kind.SAVINGS, new BigDecimal("1500.75"),
                LocalDateTime.of(2026, 10, 24, 16, 30, 15), null, false);
        Page page = new Page(List.of(item), 0, 10, 1L, 1);

        @SuppressWarnings("unchecked")
        SerializationPair<Object> pair = (SerializationPair<Object>) (SerializationPair<?>) configuration.getValueSerializationPair();

        Object restored = pair.read(pair.write(page));

        assertEquals(page, restored);
    }
}
