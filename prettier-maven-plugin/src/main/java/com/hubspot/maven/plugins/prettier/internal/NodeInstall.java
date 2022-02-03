package com.hubspot.maven.plugins.prettier.internal;

import java.nio.file.Path;
import java.util.List;

public class NodeInstall {
  private final String nodePath;
  private final List<String> npmCommand;

  public NodeInstall(String nodePath, List<String> npmCommand) {
    this.nodePath = nodePath;
    this.npmCommand = npmCommand;
  }

  public String getNodePath() {
    return nodePath;
  }

  public List<String> getNpmCommand() {
    return npmCommand;
  }
}
