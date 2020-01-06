package com.hubspot.maven.plugins.prettier.diff;

public class Suggestion {
  private final String id;
  private final String target;

  public Suggestion(String id, String target) {
    this.id = id;
    this.target = target;
  }

  public String getId() {
    return id;
  }

  public String getTarget() {
    return target;
  }

  public String getType() {
    return "FixableByPatchFileSuggestion";
  }

  public String getDescription() {
    return "Reformat code with prettier-java";
  }
}
