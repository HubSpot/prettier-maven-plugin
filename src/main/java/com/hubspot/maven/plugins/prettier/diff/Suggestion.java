package com.hubspot.maven.plugins.prettier.diff;

import java.nio.file.Paths;

public class Suggestion {
  private final String id;
  private final String patchTarget;
  private final String fileName;

  public Suggestion(String id, String patchTarget) {
    this.id = id;
    this.patchTarget = patchTarget;
    this.fileName = String.valueOf(Paths.get(patchTarget).getFileName());
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
    return "Reformat " + fileName + " with prettier-java";
  }
}
