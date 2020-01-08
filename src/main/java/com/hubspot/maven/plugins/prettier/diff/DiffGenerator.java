package com.hubspot.maven.plugins.prettier.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.logging.Log;

public interface DiffGenerator {
  void generateDiffs(GenerateDiffArgs args) throws MojoExecutionException, MojoFailureException;

  default void runDiffCommand(ProcessBuilder processBuilder, Log log) throws MojoExecutionException {
    log.debug("Going to generate diff with command: " + processBuilder.command());

    try {
      Process process = processBuilder.start();

      try (
          InputStreamReader stdoutReader = new InputStreamReader(
              process.getInputStream(),
              StandardCharsets.UTF_8
          );
          BufferedReader stdout = new BufferedReader(stdoutReader);
      ) {
        String line;
        while ((line = stdout.readLine()) != null) {
          log.warn(line);
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
}
