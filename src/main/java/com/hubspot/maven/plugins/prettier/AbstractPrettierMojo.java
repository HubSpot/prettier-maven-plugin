package com.hubspot.maven.plugins.prettier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
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

  protected abstract void handlePrettierFinished() throws MojoFailureException, MojoExecutionException;

  protected List<String> globSuffixes = new ArrayList<>();

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    try {
      List<String> globs = computeInputGlobs();
      if (globs.isEmpty()) {
        getLog().info("No input directories found");
        return;
      }

      globs.forEach(g -> {
        if (g.contains(".")) {
          globSuffixes.add(g.substring(g.lastIndexOf('.')));
        }
        else {
          globSuffixes.add(g);
        }
      });

      List<String> command = new ArrayList<>(basePrettierCommand());
      command.add("--" + getPrettierCommand());
      command.addAll(globs);

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
          if (line.contains("No matching files.") || line.contains("No files matching")) {
            noMatchingFiles = true;
          } else if (line.contains("error")) {
            getLog().error(line);
          } else {
            handlePrettierLogLine(line);
          }
        }

        int status = process.waitFor();
        if (status != 0) {
          if (status == 2 && noMatchingFiles) {
            getLog().info("No files found matching at least one of the input globs: " + globs);
          } else {
            handlePrettierNonZeroExit(status);
          }
        }
        handlePrettierFinished();
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
    command.add(toString(nodeExecutable));
    command.add(toString(prettierBin));
    command.add("--plugin=" + toString(prettierJavaPlugin));
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
    if (endOfLine != null) {
      command.add("--end-of-line");
      command.add(endOfLine);
    }
    if (ignoreConfigFile) {
      command.add("--no-config");
    }
    if (ignoreEditorConfig) {
      command.add("--no-editorconfig");
    }

    return command;
  }

  // Convert Windows Path to Unix style
  private String toString(Path path) {
    return path.toString().replace("\\", "/");
  }
}
