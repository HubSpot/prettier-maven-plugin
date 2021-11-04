package com.hubspot.maven.plugins.prettier;

public class MavenResult {
  private final boolean success;
  private final String output;

  public MavenResult(boolean success, String output) {
    this.success = success;
    this.output = output;
  }

  public boolean getSuccess() {
    return success;
  }

  public String getOutput() {
    return output;
  }
}
