package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    enum FakeType implements OutboxEventType { FIRST, SECOND, UNKNOWN }

    static class FakeEvent extends BaseOutboxEvent {
        private final FakeType type;

        FakeEvent(FakeType type) {
            this.type = type;
            setId(UUID.randomUUID());
            setAggregateId(UUID.randomUUID().toString());
        }

        @Override
        public OutboxEventType getType() {
            return type;
        }
    }

    @Mock
    private BaseOutboxEventRepository<FakeEvent> outboxEventRepository;

    @Mock
    private OutboxDispatcher firstDispatcher;

    @Mock
    private OutboxDispatcher secondDispatcher;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        lenient().when(firstDispatcher.supports(FakeType.FIRST)).thenReturn(true);
        lenient().when(secondDispatcher.supports(FakeType.SECOND)).thenReturn(true);
        outboxRelay = new OutboxRelay(outboxEventRepository, List.of(firstDispatcher, secondDispatcher));
    }

    @Test
    void shouldDeliverEachRowToTheDispatcherOfItsTypeAndDeleteIt() throws Exception {
        FakeEvent first = new FakeEvent(FakeType.FIRST);
        FakeEvent second = new FakeEvent(FakeType.SECOND);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc())
                .thenReturn(List.of(first, second))
                .thenReturn(List.of());

        outboxRelay.drain();

        InOrder inOrder = inOrder(firstDispatcher, secondDispatcher, outboxEventRepository);
        inOrder.verify(firstDispatcher).dispatch(first);
        inOrder.verify(outboxEventRepository).delete(first);
        inOrder.verify(secondDispatcher).dispatch(second);
        inOrder.verify(outboxEventRepository).delete(second);
    }

    @Test
    void shouldKeepTheRowAndStopWhenTheDestinationFails() throws Exception {
        FakeEvent first = new FakeEvent(FakeType.FIRST);
        FakeEvent second = new FakeEvent(FakeType.SECOND);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("destino fora do ar")).when(firstDispatcher).dispatch(first);

        outboxRelay.drain();

        verify(secondDispatcher, never()).dispatch(any());
        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void shouldKeepTheRowWhenNoDispatcherSupportsItsType() throws Exception {
        FakeEvent unknown = new FakeEvent(FakeType.UNKNOWN);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(unknown));

        outboxRelay.drain();

        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void shouldKeepGoingUntilTheOutboxIsEmpty() throws Exception {
        FakeEvent first = new FakeEvent(FakeType.FIRST);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc())
                .thenReturn(List.of(first))
                .thenReturn(List.of());

        outboxRelay.drain();

        verify(outboxEventRepository, times(2)).findTop100ByOrderByCreatedAtAsc();
        verify(firstDispatcher, times(1)).dispatch(first);
    }

    @Test
    void shouldDoNothingWhenThereIsNothingPending() throws Exception {
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of());

        outboxRelay.drain();

        verifyNoInteractions(firstDispatcher, secondDispatcher);
    }
}
