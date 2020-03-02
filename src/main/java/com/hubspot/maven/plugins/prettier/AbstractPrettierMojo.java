package com.hubspot.maven.plugins.prettier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

public abstract class AbstractPrettierMojo extends PrettierArgs {

  @Parameter(defaultValue = "false")
  private boolean skip;

  protected abstract String getPrettierCommand();

  protected abstract void handlePrettierLogLine(String line);

  protected abstract void handlePrettierNonZeroExit(int status)
    throws MojoExecutionException, MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    List<Path> inputDirectories = determineInputPaths();
    if (inputDirectories.isEmpty()) {
      getLog().info("No input directories found");
      return;
    }

    try {
      String glob = computeGlob(inputDirectories);

      List<String> command = new ArrayList<>(basePrettierCommand());
      command.add("--" + getPrettierCommand());
      command.add(glob);

      if (getLog().isDebugEnabled()) {
        getLog().debug("Running prettier with args " + command);
      }

      Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(project.getBasedir())
        .start();
      try (
        InputStreamReader stdoutReader = new InputStreamReader(
          process.getInputStream(),
          StandardCharsets.UTF_8
        );
        BufferedReader stdout = new BufferedReader(stdoutReader);
        InputStreamReader stderrReader = new InputStreamReader(
          process.getErrorStream(),
          StandardCharsets.UTF_8
        );
        BufferedReader stderr = new BufferedReader(stderrReader)
      ) {
        String line;
        while ((line = stdout.readLine()) != null) {
          handlePrettierLogLine(line);
        }

        boolean noMatchingFiles = false;
        while ((line = stderr.readLine()) != null) {
          if (line.contains("No matching files.")) {
            noMatchingFiles = true;
          } else if (line.contains("error")) {
            getLog().error(line);
          } else {
            getLog().warn(line);
          }
        }

        int status = process.waitFor();
        if (status != 0) {
          if (status == 2 && noMatchingFiles) {
            getLog().info("No files found matching glob " + glob);
          } else {
            handlePrettierNonZeroExit(status);
          }
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error trying to run prettier-java", e);
    }
  }

  protected List<String> basePrettierCommand() throws MojoExecutionException {
    Path nodeExecutable = resolveNodeExecutable();

    Path prettierJavaDirectory = extractPrettierJava();

    Path prettierBin = prettierJavaDirectory
        .resolve("prettier-java")
        .resolve("node_modules")
        .resolve("prettier")
        .resolve("bin-prettier.js");

    Path prettierJavaPlugin = prettierJavaDirectory
        .resolve("prettier-java")
        .resolve("node_modules")
        .resolve("prettier-plugin-java");

    List<String> command = new ArrayList<>();
    command.add(nodeExecutable.toString());
    command.add(prettierBin.toString());
    command.add("--plugin=" + prettierJavaPlugin.toString());
    command.add("--color");
    if (printWidth != null) {
      command.add("--print-width");
      command.add(printWidth);
    }
    if (tabWidth != null) {
      command.add("--tab-width");
      command.add(tabWidth);
    }
    if (useTabs != null) {
      command.add("--use-tabs");
      command.add(useTabs.toString());
    }
    if (ignoreConfigFile) {
      command.add("--no-config");
    }
    if (ignoreEditorConfig) {
      command.add("--no-editorconfig");
    }

    return command;
  }

  private List<Path> determineInputPaths() {
    List<String> inputPaths = new ArrayList<>();
    // don't use compile source roots because it seems to include generated sources
    inputPaths.add(project.getBuild().getSourceDirectory());
    inputPaths.add(project.getBuild().getTestSourceDirectory());

    Path basePath = project.getBasedir().toPath();
    return inputPaths
      .stream()
      .map(Paths::get)
      .filter(Files::isDirectory)
      .map(basePath::relativize)
      .collect(Collectors.toList());
  }

  private String computeGlob(List<Path> inputPaths) {
    final String joinedPaths;
    if (inputPaths.size() > 1) {
      joinedPaths =
        inputPaths
          .stream()
          .map(Path::toString)
          .collect(Collectors.joining(",", "{", "}"));
    } else {
      joinedPaths = inputPaths.get(0).toString();
    }

    return joinedPaths + "/**/*.java";
  }
}
