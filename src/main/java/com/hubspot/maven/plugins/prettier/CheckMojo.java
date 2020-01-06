package com.hubspot.maven.plugins.prettier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hubspot.maven.plugins.prettier.diff.BuildFailure;
import com.hubspot.maven.plugins.prettier.diff.DiffConfiguration;
import com.hubspot.maven.plugins.prettier.diff.Suggestion;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check", threadSafe = true)
public class CheckMojo extends AbstractPrettierMojo {
  private static final String MESSAGE =
    "Code formatting issues found, please run prettier-java";

  @Parameter(defaultValue = "true")
  private boolean fail;

  @Parameter
  private DiffConfiguration diffConfiguration = new DiffConfiguration();

  @Override
  protected String getPrettierCommand() {
    return "check";
  }

  private final List<Path> incorrectlyFormattedFiles = new ArrayList<>();

  @Override
  protected void handlePrettierLogLine(String line) {
    if (line.endsWith(".java")) {
      incorrectlyFormattedFiles.add(Paths.get(line));
      String message = "Incorrectly formatted file: " + line;
      if (fail) {
        getLog().error(message);
      } else {
        getLog().warn(message);
      }
    }
  }

  @Override
  protected void handlePrettierNonZeroExit(int status)
    throws MojoFailureException, MojoExecutionException {
    if (status == 1) {
      if (incorrectlyFormattedFiles.isEmpty()) {
        throw new MojoExecutionException("Error trying to run prettier-java: " + status);
      } else {
        handleDiff();

        if (fail) {
          getLog().error(MESSAGE);
          throw new MojoFailureException(MESSAGE);
        } else {
          getLog().warn(MESSAGE);
        }
      }
    } else {
      throw new MojoExecutionException("Error trying to run prettier-java: " + status);
    }
  }

  private void handleDiff() throws MojoExecutionException {
    if (diffConfiguration.isGenerateDiff()) {
      List<Suggestion> suggestions = new ArrayList<>();

      for (Path fileToFormat : incorrectlyFormattedFiles) {
        fileToFormat = fileToFormat.toAbsolutePath();
        String uuid = UUID.randomUUID().toString();
        Path workspace = Paths.get(System.getenv("WORKSPACE")).toAbsolutePath();
        Path diffPath = Paths.get(System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR")).resolve(uuid + ".diff");
        suggestions.add(new Suggestion(uuid, workspace.relativize(fileToFormat).toString()));

        List<String> prettierArgs = new ArrayList<>(basePrettierCommand());
        prettierArgs.add("'" + fileToFormat.toString() + "'");

        String prettierCommand = String.join(" ", prettierArgs);
        String diffCommand = String.format(
            "%s | diff -u %s - > %s",
            prettierCommand,
            "'" + workspace.relativize(fileToFormat) + "'",
            "'" + diffPath + "'"
        );

        getLog().debug("Going to generate diff with command: " + diffCommand);

        try {
          Process process = new ProcessBuilder("/bin/sh", "-c", diffCommand)
              .directory(workspace.toFile())
              .redirectErrorStream(true)
              .start();

          try (
              InputStreamReader stdoutReader = new InputStreamReader(
                  process.getInputStream(),
                  StandardCharsets.UTF_8
              );
              BufferedReader stdout = new BufferedReader(stdoutReader);
          ) {
            String line;
            while ((line = stdout.readLine()) != null) {
              getLog().warn(line);
            }

            int status = process.waitFor();
            if (status != 1) {
              throw new MojoExecutionException(
                  "Error trying to create diff with prettier-java: " + status
              );
            }
          }
        } catch (IOException | InterruptedException e) {
          throw new MojoExecutionException(
              "Error trying to create diff with prettier-java",
              e
          );
        }
      }

      BuildFailure buildFailure = new BuildFailure(suggestions);
      Path buildFailurePath = Paths.get(System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR")).resolve("prettier-java.buildfailurecause.json");
      try {
        new ObjectMapper().writeValue(buildFailurePath.toFile(), buildFailure);
      } catch (IOException e) {
        throw new MojoExecutionException("Error trying to generate JSON", e);
      }
    }
  }
}
