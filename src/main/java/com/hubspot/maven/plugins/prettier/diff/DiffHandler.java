package com.hubspot.maven.plugins.prettier.diff;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.apache.maven.plugin.logging.Log;

public class DiffHandler {
  private final DiffConfiguration diffConfiguration;
  private final List<String> basePrettierCommand;
  private final Log log;

  public DiffHandler(
      DiffConfiguration diffConfiguration,
      List<String> basePrettierCommand,
      Log log
  ) {
    this.diffConfiguration = diffConfiguration;
    this.basePrettierCommand = basePrettierCommand;
    this.log = log;
  }

  public void handle(List<Path> incorrectlyFormattedFiles) throws MojoExecutionException {
    if (diffConfiguration.isGenerateDiff()) {
      if (
          diffConfiguration.getMaxFiles() > 0 &&
          incorrectlyFormattedFiles.size() > diffConfiguration.getMaxFiles()
      ) {
        incorrectlyFormattedFiles =
            incorrectlyFormattedFiles.subList(0, diffConfiguration.getMaxFiles());
      }

      List<Suggestion> suggestions = new ArrayList<>();

      for (Path fileToFormat : incorrectlyFormattedFiles) {
        fileToFormat = fileToFormat.toAbsolutePath();
        String uuid = UUID.randomUUID().toString();
        Path workspace = Paths.get(System.getenv("WORKSPACE")).toAbsolutePath();
        Path diffPath = Paths.get(System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR")).resolve(uuid + ".diff");
        suggestions.add(new Suggestion(uuid, workspace.relativize(fileToFormat)));

        List<String> prettierArgs = new ArrayList<>(basePrettierCommand);
        prettierArgs.add(quote(fileToFormat));

        String prettierCommand = String.join(" ", prettierArgs);
        String diffCommand = String.format(
            "%s | diff -u %s - > %s",
            prettierCommand,
            quote(workspace.relativize(fileToFormat)),
            quote(diffPath)
        );

        log.debug("Going to generate diff with command: " + diffCommand);

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

      BuildFailure buildFailure = new BuildFailure(suggestions);
      Path buildFailurePath = Paths.get(System.getenv("VIEWABLE_BUILD_ARTIFACTS_DIR")).resolve("prettier-java.buildfailurecause.json");
      try {
        new ObjectMapper().writeValue(buildFailurePath.toFile(), buildFailure);
      } catch (IOException e) {
        throw new MojoExecutionException("Error trying to generate JSON", e);
      }
    }
  }

  private static String quote(Path path) {
    return "'" + path + "'";
  }
}
