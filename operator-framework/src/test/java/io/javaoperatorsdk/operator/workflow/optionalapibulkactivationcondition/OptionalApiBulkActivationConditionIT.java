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

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.fabric8.kubernetes.api.model.ObjectMetaBuilder;
import io.javaoperatorsdk.annotation.Sample;
import io.javaoperatorsdk.operator.junit.LocallyRunOperatorExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Reproducer for the bug where an informer is started for dependent even though
 * the dependent is not activated.
 *
 * <p>Workflow under test:
 *
 * <pre>
 * ConfigMapDependentResource  (reconcilePrecondition = AlwaysFailingPrecondition)
 *   └── OptionalApiBulkDependentResource  (activationCondition = always returns false)
 * </pre>
 *
 * <p>On first reconciliation the ConfigMap precondition fails → JOSDK calls
 * markDependentsForDelete() → registerOrDeregisterEventSourceBasedOnActivation()
 * which proceeds to unconditionally register an informer for the OptionalApi</p>
 */
@Sample(
    tldr = "Optional API Bulk Dependent Resource with Activation Condition Bug Reproducer",
    description =
        """
        Reproducer for a bug where an informer is started for an API that is not present on the server
        despite the fact that the dependent resource is not activated.
        """)
class OptionalApiBulkActivationConditionIT {

  static final BulkActivationConditionReconciler reconciler =
      new BulkActivationConditionReconciler();

  @RegisterExtension
  static LocallyRunOperatorExtension extension =
      LocallyRunOperatorExtension.builder().withReconciler(reconciler).build();

  @BeforeEach
  void reset() {
    reconciler.lastError.set(null);
    reconciler.callCount.set(0);
  }

  @Test
  void optionalApiInformerRegisteredEvenWhenNotActivated() {
    var primary = new BulkActivationConditionCustomResource();
    primary.setMetadata(
        new ObjectMetaBuilder()
            .withName("test-primary")
            .withNamespace(extension.getNamespace())
            .build());
    extension.create(primary);

    // Wait for reconcile() to be called.
    // If the bug is present, SecretBulkDependentResource will be in error and lastError will be set
    await().atMost(Duration.ofSeconds(10)).until(() -> reconciler.callCount.get() == 1);

    // On unfixed JOSDK this fails: lastError contains NoEventSourceForClassException.
    assertThat(reconciler.lastError.get()).isNull();
  }
}
