/*
 * Copyright Java Operator SDK Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.javaoperatorsdk.operator.workflow.optionalapibulkactivationcondition;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.ControllerConfiguration;
import io.javaoperatorsdk.operator.api.reconciler.MaxReconciliationInterval;
import io.javaoperatorsdk.operator.api.reconciler.Reconciler;
import io.javaoperatorsdk.operator.api.reconciler.UpdateControl;
import io.javaoperatorsdk.operator.api.reconciler.Workflow;
import io.javaoperatorsdk.operator.api.reconciler.dependent.Dependent;
import io.javaoperatorsdk.operator.processing.retry.GradualRetry;

/**
 * Workflow:
 *
 * <pre>
 * ConfigMapDependentResource  (reconcilePrecondition = AlwaysFailingPrecondition)
 *   └── SecretBulkDependentResource  (activationCondition = AlwaysTrueActivation)
 * </pre>
 *
 * <p>On first reconciliation: ConfigMap precondition fails → markDependentsForDelete cascades to
 * SecretBulkDependentResource → NodeDeleteExecutor fires for Secret before its event source is ever
 * registered → NoEventSourceForClassException.
 */
@Workflow(
    dependents = {
      @Dependent(
          name = "configmap",
          type = ConfigMapDependentResource.class,
          reconcilePrecondition = AlwaysFailingPrecondition.class),
      @Dependent(
          name = OptionalApiBulkDependentResource.NAME,
          type = OptionalApiBulkDependentResource.class,
          activationCondition = OptionalApiRoutePresentActivation.class,
          dependsOn = "configmap")
    },
    handleExceptionsInReconciler = true)
@GradualRetry(maxAttempts = 0)
@ControllerConfiguration(maxReconciliationInterval = @MaxReconciliationInterval(interval = 0))
public class BulkActivationConditionReconciler
    implements Reconciler<BulkActivationConditionCustomResource> {

  /** Tracks how many times reconcile() or updateErrorStatus() has been called. */
  final AtomicInteger callCount = new AtomicInteger();

  /** Set when updateErrorStatus() is invoked; null means no error occurred. */
  final AtomicReference<Exception> lastError = new AtomicReference<>();

  @Override
  public UpdateControl<BulkActivationConditionCustomResource> reconcile(
      BulkActivationConditionCustomResource primary,
      Context<BulkActivationConditionCustomResource> context) {
    final var workflowResult =
        context
            .managedWorkflowAndDependentResourceContext()
            .getWorkflowReconcileResult()
            .orElseThrow();
    final var erroredDependents = workflowResult.getErroredDependents();
    if (!erroredDependents.isEmpty()) {
      final var exception =
          erroredDependents.get(
              workflowResult
                  .getDependentResourceByName(OptionalApiBulkDependentResource.NAME)
                  .orElseThrow());
      lastError.set(exception);
    }
    callCount.incrementAndGet();
    return UpdateControl.noUpdate();
  }
}
