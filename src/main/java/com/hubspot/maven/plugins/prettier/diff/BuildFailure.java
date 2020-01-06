package com.hubspot.maven.plugins.prettier.diff;

import java.util.List;

public class BuildFailure {
  private final List<Suggestion> suggestions;

  public BuildFailure(List<Suggestion> suggestions) {
    this.suggestions = suggestions;
  }

  public List<Suggestion> getSuggestions() {
    return suggestions;
  }

  public String getCause() {
    return "Incorrectly formatted code";
  }
}
