package com.hubspot.maven.plugins.prettier.diff;

import java.nio.file.Path;
import java.util.List;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;

public class GenerateDiffArgs {
  private final List<Path> incorrectlyFormattedFiles;
  private final List<String> basePrettierCommand;
  private final MavenProject project;
  private final Log log;

  public GenerateDiffArgs(
      List<Path> incorrectlyFormattedFiles,
      List<String> basePrettierCommand,
      MavenProject project,
      Log log
  ) {
    this.incorrectlyFormattedFiles = incorrectlyFormattedFiles;
    this.basePrettierCommand = basePrettierCommand;
    this.project = project;
    this.log = log;
  }

  public List<Path> getIncorrectlyFormattedFiles() {
    return incorrectlyFormattedFiles;
  }

  public List<String> getBasePrettierCommand() {
    return basePrettierCommand;
  }

  public MavenProject getProject() {
    return project;
  }

  public Log getLog() {
    return log;
  }
}
