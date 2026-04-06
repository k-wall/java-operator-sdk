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

import io.fabric8.kubernetes.api.model.Secret;
import io.javaoperatorsdk.operator.api.reconciler.Context;
import io.javaoperatorsdk.operator.api.reconciler.dependent.DependentResource;
import io.javaoperatorsdk.operator.processing.dependent.workflow.Condition;

/** Activation condition that always returns true — event source should always be registered. */
public class OptionalApiRoutePresentActivation
    implements Condition<Secret, BulkActivationConditionCustomResource> {

  @Override
  public boolean isMet(
      DependentResource<Secret, BulkActivationConditionCustomResource> dependentResource,
      BulkActivationConditionCustomResource primary,
      Context<BulkActivationConditionCustomResource> context) {
    boolean supports = context.getClient().supports(OptionalApiResource.class);
    if (supports) {
      throw new IllegalStateException("test precondition fails");
    }

    return supports;
  }
}
