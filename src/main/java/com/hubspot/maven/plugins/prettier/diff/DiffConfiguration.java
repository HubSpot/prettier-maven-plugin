package com.hubspot.maven.plugins.prettier.diff;

public class DiffConfiguration {
  private boolean generateDiff = true;
  private boolean printDiff = true;
  private int maxFiles = -1;

  public boolean isGenerateDiff() {
    return generateDiff;
  }

  public void setGenerateDiff(boolean generateDiff) {
    this.generateDiff = generateDiff;
  }

  public boolean isPrintDiff() {
    return printDiff;
  }

  public void setPrintDiff(boolean printDiff) {
    this.printDiff = printDiff;
  }

  public int getMaxFiles() {
    return maxFiles;
  }

  public void setMaxFiles(int maxFiles) {
    this.maxFiles = maxFiles;
  }
}
