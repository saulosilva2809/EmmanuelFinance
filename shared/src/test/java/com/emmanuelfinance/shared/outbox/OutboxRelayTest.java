package com.emmanuelfinance.shared.outbox;

import com.emmanuelfinance.shared.outbox.enums.OutboxEventType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    enum FakeType implements OutboxEventType { FIRST, SECOND, UNKNOWN }

    static class FakeEvent extends BaseOutboxEvent {
        private final FakeType type;

        FakeEvent(FakeType type) {
            this(type, UUID.randomUUID().toString());
        }

        FakeEvent(FakeType type, String aggregateId) {
            this.type = type;
            setId(UUID.randomUUID());
            setAggregateId(aggregateId);
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
    void shouldKeepTheRowThatFailedButStillDeliverTheRowsOfOtherDestinations() throws Exception {
        FakeEvent first = new FakeEvent(FakeType.FIRST);
        FakeEvent second = new FakeEvent(FakeType.SECOND);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(first, second));
        doThrow(new IllegalStateException("destino fora do ar")).when(firstDispatcher).dispatch(first);

        outboxRelay.drain();

        verify(secondDispatcher).dispatch(second);
        verify(outboxEventRepository).delete(second);
        verify(outboxEventRepository, never()).delete(first);
    }

    // o caso real: a foto do mongo falha, mas o aviso do kafka do mesmo registro precisa sair
    @Test
    void shouldDeliverTheOtherDestinationEvenForTheSameRecord() throws Exception {
        String recordId = UUID.randomUUID().toString();
        FakeEvent snapshot = new FakeEvent(FakeType.FIRST, recordId);
        FakeEvent notice = new FakeEvent(FakeType.SECOND, recordId);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(snapshot, notice));
        doThrow(new IllegalStateException("mongo fora do ar")).when(firstDispatcher).dispatch(snapshot);

        outboxRelay.drain();

        verify(secondDispatcher).dispatch(notice);
        verify(outboxEventRepository).delete(notice);
        verify(outboxEventRepository, never()).delete(snapshot);
    }

    // a ordem de um mesmo destino e de um mesmo registro é mantida: a foto nova não passa na frente da antiga
    @Test
    void shouldHoldTheNextRowsOfTheSameDestinationAndRecordAfterAFailure() throws Exception {
        String recordId = UUID.randomUUID().toString();
        FakeEvent older = new FakeEvent(FakeType.FIRST, recordId);
        FakeEvent newer = new FakeEvent(FakeType.FIRST, recordId);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(older, newer));
        doThrow(new IllegalStateException("destino fora do ar")).when(firstDispatcher).dispatch(older);

        outboxRelay.drain();

        verify(firstDispatcher, never()).dispatch(newer);
        verify(outboxEventRepository, never()).delete(any());
    }

    @Test
    void shouldNotHoldRowsOfTheSameDestinationButOfAnotherRecord() throws Exception {
        FakeEvent failing = new FakeEvent(FakeType.FIRST);
        FakeEvent other = new FakeEvent(FakeType.FIRST);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(failing, other));
        doThrow(new IllegalStateException("registro com problema")).when(firstDispatcher).dispatch(failing);

        outboxRelay.drain();

        verify(firstDispatcher).dispatch(other);
        verify(outboxEventRepository).delete(other);
    }

    // as linhas presas ficam fora da próxima busca, senão elas esconderiam as que estão atrás
    @Test
    void shouldLookBehindTheRowsThatAreStuck() throws Exception {
        FakeEvent stuck = new FakeEvent(FakeType.FIRST);
        FakeEvent behind = new FakeEvent(FakeType.SECOND);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(stuck));
        when(outboxEventRepository.findTop100ByIdNotInOrderByCreatedAtAsc(argThat((Collection<UUID> ids) ->
                ids.contains(stuck.getId())))).thenReturn(List.of(behind)).thenReturn(List.of());
        doThrow(new IllegalStateException("destino fora do ar")).when(firstDispatcher).dispatch(stuck);

        outboxRelay.drain();

        verify(secondDispatcher).dispatch(behind);
        verify(outboxEventRepository).delete(behind);
    }

    @Test
    void shouldStopWhenEveryRemainingRowIsStuck() throws Exception {
        FakeEvent stuck = new FakeEvent(FakeType.FIRST);
        when(outboxEventRepository.findTop100ByOrderByCreatedAtAsc()).thenReturn(List.of(stuck));
        when(outboxEventRepository.findTop100ByIdNotInOrderByCreatedAtAsc(any())).thenReturn(List.of());
        doThrow(new IllegalStateException("destino fora do ar")).when(firstDispatcher).dispatch(stuck);

        outboxRelay.drain();

        verify(firstDispatcher, times(1)).dispatch(stuck);
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
