package com.paymentprocessor.saga.statemachine;

import com.paymentprocessor.events.PaymentInitiated;
import com.paymentprocessor.saga.compensation.CompensationHandler;
import com.paymentprocessor.saga.domain.SagaState;
import com.paymentprocessor.saga.repository.SagaStateRepository;
import com.paymentprocessor.saga.steps.SagaStepException;
import com.paymentprocessor.saga.steps.SagaStepHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class SagaOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(SagaOrchestrator.class);

    private static final List<SagaStep> FORWARD_STEPS = List.of(
            SagaStep.CHARGE,
            SagaStep.RECORD,
            SagaStep.NOTIFY
    );

    private final Map<SagaStep, SagaStepHandler> stepHandlers;
    private final SagaStateRepository sagaStateRepository;
    private final CompensationHandler compensationHandler;

    public SagaOrchestrator(List<SagaStepHandler> handlers,
                             SagaStateRepository sagaStateRepository,
                             CompensationHandler compensationHandler) {
        this.stepHandlers = handlers.stream()
                .collect(Collectors.toMap(SagaStepHandler::handles, h -> h));
        this.sagaStateRepository = sagaStateRepository;
        this.compensationHandler = compensationHandler;
    }

    public void execute(PaymentInitiated event) {
        UUID paymentId = UUID.fromString(event.paymentId());

        // Guard against Kafka at-least-once redelivery
        if (sagaStateRepository.existsByPaymentId(paymentId)) {
            log.warn("Duplicate saga execution skipped for paymentId={}", event.paymentId());
            return;
        }

        // Persist initial state BEFORE executing — enables crash recovery
        SagaState state = new SagaState();
        state.setPaymentId(paymentId);
        state.setStatus(SagaStatus.RUNNING.name());
        state.setCurrentStep(SagaStep.CHARGE.name());
        state = sagaStateRepository.save(state);

        SagaContext context = SagaContext.from(event, state.getSagaId().toString());

        for (SagaStep step : FORWARD_STEPS) {
            try {
                context = stepHandlers.get(step).execute(context);

                // Persist progress after each step — if the service crashes here,
                // a future recovery job can resume from currentStep
                state.setCurrentStep(step.name());
                sagaStateRepository.save(state);

                log.debug("Saga step {} completed for paymentId={}", step, event.paymentId());
            } catch (SagaStepException ex) {
                log.error("Saga step {} failed for paymentId={}: {}",
                        step, event.paymentId(), ex.getMessage());
                state.setStatus(SagaStatus.FAILED.name());
                state.setLastError(ex.getMessage());
                sagaStateRepository.save(state);
                compensationHandler.compensate(context, ex);
                return;
            }
        }

        state.setStatus(SagaStatus.COMPLETED.name());
        state.setCurrentStep(SagaStep.COMPLETED.name());
        state.setCompletedAt(Instant.now());
        sagaStateRepository.save(state);
        log.info("Saga completed successfully for paymentId={}", event.paymentId());
    }
}
