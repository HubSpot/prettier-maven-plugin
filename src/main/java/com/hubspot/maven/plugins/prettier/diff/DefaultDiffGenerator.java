package com.hubspot.maven.plugins.prettier.diff;

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

      ProcessBuilder processBuilder = new ProcessBuilder("/bin/sh", "-c", diffCommand)
          .directory(baseDir.toFile());

      runDiffCommand(processBuilder, args.getLog());
    }

    args.getLog().info("Diff file generated at " + baseDir.relativize(diffFile));
  }

  private static String quote(Path path) {
    return "'" + path + "'";
  }
}
