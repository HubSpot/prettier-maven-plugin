package com.hubspot.maven.plugins.prettier;

import java.util.List;
import java.util.stream.Collectors;

import com.google.common.base.MoreObjects;
import com.google.common.base.Preconditions;

public class TestConfiguration {
  public enum Goal {
    CHECK, WRITE
  }

  private final String prettierJavaVersion;
  private final List<String> inputGlobs;
  private final Goal goal;

  private TestConfiguration(String prettierJavaVersion, List<String> inputGlobs, Goal goal) {
    this.prettierJavaVersion = prettierJavaVersion;
    this.inputGlobs = inputGlobs;
    this.goal = goal;
  }

  public static Builder newBuilder() {
    return new Builder();
  }

  public String getPrettierJavaVersion() {
    return prettierJavaVersion;
  }

  public List<String> getInputGlobs() {
    return inputGlobs;
  }

  public Goal getGoal() {
    return goal;
  }

  public String render(String template) {
    return template
        .replace(
            "${nodeVersion}",
            MoreObjects.firstNonNull(System.getenv("PRETTIER_NODE_VERSION"), "16.13.2")
        )
        .replace("${pluginVersion}", System.getenv("PLUGIN_VERSION"))
        .replace("${prettierJavaVersion}", prettierJavaVersion)
        .replace(
            "${inputGlobs}",
            inputGlobs
                .stream()
                .map(inputGlob -> "<inputGlob>" + inputGlob + "</inputGlob>")
                .collect(Collectors.joining("\n"))
        )
        .replace("${goal}", goal.name().toLowerCase());
  }

  public static class Builder {
    private String prettierJavaVersion = null;
    private List<String> inputGlobs = null;
    private Goal goal = null;

    private Builder() {}

  public Builder setPrettierJavaVersion(String prettierJavaVersion) {
    this.prettierJavaVersion = Preconditions.checkNotNull(prettierJavaVersion);
    return this;
  }

  public Builder setInputGlobs(List<String> inputGlobs) {
    this.inputGlobs = Preconditions.checkNotNull(inputGlobs);
    return this;
  }

  public Builder setGoal(Goal goal) {
    this.goal = Preconditions.checkNotNull(goal);
    return this;
  }

    public TestConfiguration build() {
      if (prettierJavaVersion == null) {
        throw new IllegalStateException("prettierJavaVersion must be set");
      }

      if (inputGlobs == null) {
        throw new IllegalStateException("inputGlobs must be set");
      }

      if (goal == null) {
        throw new IllegalStateException("goal must be set");
      }

      return new TestConfiguration(prettierJavaVersion, inputGlobs, goal);
    }
  }
}
