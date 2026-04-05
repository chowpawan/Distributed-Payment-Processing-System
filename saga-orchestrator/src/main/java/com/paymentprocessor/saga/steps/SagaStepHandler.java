package com.paymentprocessor.saga.steps;

import com.paymentprocessor.saga.statemachine.SagaContext;
import com.paymentprocessor.saga.statemachine.SagaStep;

public interface SagaStepHandler {

    SagaStep handles();

    SagaContext execute(SagaContext context) throws SagaStepException;
}
