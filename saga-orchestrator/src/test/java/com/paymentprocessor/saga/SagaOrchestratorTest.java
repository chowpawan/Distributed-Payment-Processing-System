package com.paymentprocessor.saga;

import com.paymentprocessor.events.PaymentInitiated;
import com.paymentprocessor.saga.compensation.CompensationHandler;
import com.paymentprocessor.saga.domain.SagaState;
import com.paymentprocessor.saga.repository.SagaStateRepository;
import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.statemachine.SagaOrchestrator;
import com.paymentprocessor.saga.statemachine.SagaStep;
import com.paymentprocessor.saga.steps.SagaStepException;
import com.paymentprocessor.saga.steps.SagaStepHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaOrchestratorTest {

    @Mock private SagaStateRepository sagaStateRepository;
    @Mock private CompensationHandler compensationHandler;
    @Mock private SagaStepHandler chargeStep;
    @Mock private SagaStepHandler recordStep;
    @Mock private SagaStepHandler notifyStep;

    private SagaOrchestrator orchestrator;

    @BeforeEach
    void setUp() {
        when(chargeStep.handles()).thenReturn(SagaStep.CHARGE);
        when(recordStep.handles()).thenReturn(SagaStep.RECORD);
        when(notifyStep.handles()).thenReturn(SagaStep.NOTIFY);

        SagaState savedState = new SagaState();
        savedState.setPaymentId(UUID.randomUUID());
        savedState.setStatus("RUNNING");
        savedState.setCurrentStep("CHARGE");
        when(sagaStateRepository.save(any(SagaState.class))).thenReturn(savedState);

        orchestrator = new SagaOrchestrator(
                List.of(chargeStep, recordStep, notifyStep),
                sagaStateRepository,
                compensationHandler
        );
    }

    private PaymentInitiated buildEvent(String paymentId) {
        return new PaymentInitiated(paymentId, "cust_001", new BigDecimal("99.99"),
                "usd", "key-001", Instant.now(), UUID.randomUUID().toString());
    }

    @Test
    void execute_happyPath_allStepsExecutedInOrder() throws SagaStepException {
        String paymentId = UUID.randomUUID().toString();
        when(sagaStateRepository.existsByPaymentId(any())).thenReturn(false);
        when(chargeStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notifyStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.execute(buildEvent(paymentId));

        var inOrder = inOrder(chargeStep, recordStep, notifyStep);
        inOrder.verify(chargeStep).execute(any(SagaContext.class));
        inOrder.verify(recordStep).execute(any(SagaContext.class));
        inOrder.verify(notifyStep).execute(any(SagaContext.class));
        verify(compensationHandler, never()).compensate(any(), any());
    }

    @Test
    void execute_chargeStepFails_compensationTriggered() throws SagaStepException {
        String paymentId = UUID.randomUUID().toString();
        when(sagaStateRepository.existsByPaymentId(any())).thenReturn(false);
        when(chargeStep.execute(any())).thenThrow(new SagaStepException("Stripe timeout"));

        orchestrator.execute(buildEvent(paymentId));

        verify(compensationHandler).compensate(any(SagaContext.class), any(SagaStepException.class));
        verify(recordStep, never()).execute(any());
        verify(notifyStep, never()).execute(any());
    }

    @Test
    void execute_recordStepFails_compensationTriggered() throws SagaStepException {
        String paymentId = UUID.randomUUID().toString();
        when(sagaStateRepository.existsByPaymentId(any())).thenReturn(false);
        when(chargeStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordStep.execute(any())).thenThrow(new SagaStepException("DB write failed"));

        orchestrator.execute(buildEvent(paymentId));

        verify(chargeStep).execute(any());
        verify(compensationHandler).compensate(any(SagaContext.class), any(SagaStepException.class));
        verify(notifyStep, never()).execute(any());
    }

    @Test
    void execute_duplicatePaymentId_skipped() throws SagaStepException {
        String paymentId = UUID.randomUUID().toString();
        when(sagaStateRepository.existsByPaymentId(any())).thenReturn(true);

        orchestrator.execute(buildEvent(paymentId));

        verify(chargeStep, never()).execute(any());
        verify(sagaStateRepository, never()).save(any(SagaState.class));
    }

    @Test
    void execute_sagaStatePersistedAfterEachStep() throws SagaStepException {
        String paymentId = UUID.randomUUID().toString();
        when(sagaStateRepository.existsByPaymentId(any())).thenReturn(false);
        when(chargeStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));
        when(recordStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));
        when(notifyStep.execute(any())).thenAnswer(inv -> inv.getArgument(0));

        orchestrator.execute(buildEvent(paymentId));

        // 1 initial save + 1 per step (3) + 1 final completion = 5 saves
        verify(sagaStateRepository, times(5)).save(any(SagaState.class));
    }
}
