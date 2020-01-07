package com.hubspot.maven.plugins.prettier.diff;

import java.nio.file.Path;

public class Suggestion {
  private final String id;
  private final String patchTarget;
  private final String fileName;

  public Suggestion(String id, Path patchTarget) {
    this.id = id;
    this.patchTarget = patchTarget.toString();
    this.fileName = String.valueOf(patchTarget.getFileName());
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
    return "Reformat `" + fileName + "` with prettier-java";
  }
}
