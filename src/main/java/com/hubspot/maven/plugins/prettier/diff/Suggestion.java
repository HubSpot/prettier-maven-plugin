package com.hubspot.maven.plugins.prettier.diff;

public class Suggestion {
  private final String id;
  private final String patchTarget;

  public Suggestion(String id, String patchTarget) {
    this.id = id;
    this.patchTarget = patchTarget;
  }

  public String getId() {
    return id;
  }

  public String getPatchTarget() {
    return patchTarget;
  }

  public String getType() {
    return "FixableByPatchFileSuggestion";
  }

  public String getDescription() {
    return "Reformat code with prettier-java";
  }
}
