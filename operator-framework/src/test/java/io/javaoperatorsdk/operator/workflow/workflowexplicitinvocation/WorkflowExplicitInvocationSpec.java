package io.javaoperatorsdk.operator.workflow.workflowexplicitinvocation;

public class WorkflowExplicitInvocationSpec {

  private String value;

  public String getValue() {
    return value;
  }

  public WorkflowExplicitInvocationSpec setValue(String value) {
    this.value = value;
    return this;
  }
}
