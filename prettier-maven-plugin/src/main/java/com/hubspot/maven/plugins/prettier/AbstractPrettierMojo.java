package com.hubspot.maven.plugins.prettier;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.hubspot.maven.plugins.prettier.internal.NodeInstall;
import com.hubspot.maven.plugins.prettier.internal.PrettierPaths;

public abstract class AbstractPrettierMojo extends PrettierArgs {

  private static final Logger LOG = LoggerFactory.getLogger(AbstractPrettierMojo.class);

  private static final Pattern ANSI_COLOR_CODE = Pattern.compile("\u001B\\[[;\\d]*m");

  private static final AtomicBoolean REGISTERED_SHUTDOWN_WARNING = new AtomicBoolean(false);

  @Parameter(defaultValue = "false")
  private boolean skip;

  protected abstract String getPrettierCommand();

  protected abstract void handlePrettierLogLine(String line);

  protected abstract void handlePrettierNonZeroExit(int status)
    throws MojoExecutionException, MojoFailureException;

  protected abstract Optional<String> getSpotlessReplacement();

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    logDeprecation();

    if (REGISTERED_SHUTDOWN_WARNING.compareAndSet(false, true)) {
      Runtime.getRuntime().addShutdownHook(new Thread(this::logDeprecation));
    }

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
          } else if (ANSI_COLOR_CODE.matcher(line).replaceAll("").startsWith("[error]")) {
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

    logDeprecation();
  }

  protected void logDeprecation() {
    getSpotlessReplacement().ifPresent(replacement -> {
      LOG.warn("");
      LOG.warn("");
      LOG.warn("***************************************************************");
      LOG.warn(" ____    ____ ____  ____   ____   ___  ___  ______  ____ ____  ");
      LOG.warn(" || \\\\  ||    || \\\\ || \\\\ ||     //   // \\\\ | || | ||    || \\\\ ");
      LOG.warn(" ||  )) ||==  ||_// ||_// ||==  ((    ||=||   ||   ||==  ||  ))");
      LOG.warn(" ||_//  ||___ ||    || \\\\ ||___  \\\\__ || ||   ||   ||___ ||_// ");
      LOG.warn("                                                               ");
      LOG.warn("***************************************************************");
      LOG.warn("");
      LOG.warn("");
      LOG.warn("Prettier-maven-plugin is DEPRECATED and will soon be removed.");
      LOG.warn("The replacement, Spotless, is drop-in compatible and offers enhanced functionality.");
      LOG.warn("Please transition to Spotless by running `spotless:{}` instead!", replacement);
      LOG.warn("");
      LOG.warn("");
    });
  }

  protected static MojoExecutionException prettierExecutionFailed(int status) throws MojoExecutionException {
    throw new MojoExecutionException(
        "Error trying to run prettier-java: " + status
    );
  }

  protected List<String> basePrettierCommand() throws MojoExecutionException, MojoFailureException {
    NodeInstall nodeInstall = resolveNodeInstall();

    Path prettierJavaDirectory = downloadPrettierJava(nodeInstall);

    Path prettierBin = prettierJavaDirectory.resolve(PrettierPaths.prettierBinPath(prettierJavaVersion));
    Path prettierJavaPlugin = prettierJavaDirectory.resolve(PrettierPaths.prettierJavaPluginPath(prettierJavaVersion));

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
