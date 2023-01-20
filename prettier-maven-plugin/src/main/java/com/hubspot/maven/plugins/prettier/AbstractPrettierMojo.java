package com.hubspot.maven.plugins.prettier;

import com.hubspot.maven.plugins.prettier.internal.NodeInstall;
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

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    // a trivial change
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

      List<String> command = new ArrayList<>(basePrettierCommand());
      command.add("--" + getPrettierCommand());
      command.addAll(globs);

      if (getLog().isDebugEnabled()) {
        getLog().debug("Running prettier with args: " + String.join(" ", command));
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

        boolean hasError = false;
        while ((line = stderr.readLine()) != null) {
          if (line.contains("No matching files.") || line.contains("No files matching")) {
            getLog().info(trimLogLevel(line));
          } else if (line.contains("error")) {
            getLog().error(trimLogLevel(line));
            hasError = true;
          } else {
            handlePrettierLogLine(line);
          }
        }

        int status = process.waitFor();
        getLog().debug("Prettier exit code: " + status);
        if (hasError) {
          prettierExecutionFailed(status);
        } else if (status != 0) {
          handlePrettierNonZeroExit(status);
        }
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error trying to run prettier-java", e);
    }
  }

  protected static MojoExecutionException prettierExecutionFailed(int status) throws MojoExecutionException {
    throw new MojoExecutionException(
        "Error trying to run prettier-java: " + status
    );
  }

  protected List<String> basePrettierCommand() throws MojoExecutionException, MojoFailureException {
    NodeInstall nodeInstall = resolveNodeInstall();

    Path prettierJavaDirectory = downloadPrettierJava(nodeInstall);

    Path prettierBin = prettierJavaDirectory
        .resolve("node_modules")
        .resolve("prettier")
        .resolve("bin-prettier.js");

    Path prettierJavaPlugin = prettierJavaDirectory
        .resolve("node_modules")
        .resolve("prettier-plugin-java");

    List<String> command = new ArrayList<>();
    command.add(nodeInstall.getNodePath());
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

  protected static String trimLogLevel(String line) {
    int closeBracketIndex = line.indexOf(']');
    if (closeBracketIndex < 0) {
      return line;
    }

    int startFileIndex = closeBracketIndex + "] ".length();
    if (startFileIndex >= line.length()) {
      return line;
    }

    // converts something like '[warn] src/main/java/Test.java' -> 'src/main/java/Test.java'
    return line.substring(startFileIndex);
  }

  // Convert Windows Path to Unix style
  private String toString(Path path) {
    return path.toString().replace("\\", "/");
  }
}
