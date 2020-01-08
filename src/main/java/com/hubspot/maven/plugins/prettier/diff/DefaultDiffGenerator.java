package com.hubspot.maven.plugins.prettier.diff;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;

public class DefaultDiffGenerator implements DiffGenerator {

  @Override
  public void generateDiffs(GenerateDiffArgs args) throws MojoExecutionException {
    Path baseDir = args
        .getProject()
        .getBasedir()
        .toPath()
        .toAbsolutePath();

    Path diffFile = Paths
        .get(args.getProject().getBuild().getDirectory())
        .resolve("prettier-java.diff")
        .toAbsolutePath();

    for (Path fileToFormat : args.getIncorrectlyFormattedFiles()) {
      fileToFormat = fileToFormat.toAbsolutePath();

      List<String> prettierArgs = new ArrayList<>(args.getBasePrettierCommand());
      prettierArgs.add(quote(fileToFormat));

      String prettierCommand = String.join(" ", prettierArgs);
      String diffCommand = String.format(
          "%s | diff -u %s - >> %s",
          prettierCommand,
          quote(baseDir.relativize(fileToFormat)),
          quote(diffFile)
      );

      args.getLog().debug("Going to generate diff with command: " + diffCommand);

      try {
        Process process = new ProcessBuilder("/bin/sh", "-c", diffCommand)
            .directory(baseDir.toFile())
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
            args.getLog().warn(line);
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

    args.getLog().info("Diff file generated at " + baseDir.relativize(diffFile));
  }

  private static String quote(Path path) {
    return "'" + path + "'";
  }
}
