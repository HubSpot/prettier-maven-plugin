package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class PrettierDownloader {
  private final Path installDirectory;
  private final NodeInstall nodeInstall;
  private final Log log;

  public PrettierDownloader(Path installDirectory, NodeInstall nodeInstall, Log log) {
    this.installDirectory = installDirectory;
    this.nodeInstall = nodeInstall;
    this.log = log;
  }

  public Path downloadPrettierJava(String prettierJavaVersion) throws MojoExecutionException {
    Path prettierDirectory = installDirectory.resolve("prettier-java-" + prettierJavaVersion);

    if (Files.exists(prettierDirectory)) {
      log.debug("Reusing cached prettier-java at: " + prettierDirectory);
    } else {
      try {
        Path tmpDir = installPrettierJavaToTmpDir(prettierJavaVersion);
        FileUtils.move(tmpDir, prettierDirectory);
        log.debug("Downloaded prettier-java to: " + prettierDirectory);
      } catch (IOException e) {
        throw new MojoExecutionException("Error downloading prettier-java", e);
      }
    }

    return prettierDirectory;
  }

  private Path installPrettierJavaToTmpDir(String prettierJavaVersion) throws MojoExecutionException, IOException {
    Path tmpDir = Files.createTempDirectory(
        installDirectory,
        "prettier-java-", OperatingSystemFamily.current().getGlobalPermissions()
    );

    List<String> command = new ArrayList<>(nodeInstall.getNpmCommand());
    command.add("install");
    command.add("prettier-plugin-java@" + prettierJavaVersion);

    log.debug("Running npm install command: " + command);

    Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(tmpDir.toFile())
        .inheritIO()
        .start();

    try {
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new MojoExecutionException("Error downloading prettier-java, exit code: " + exitCode);
      }
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Interrupted while downloading prettier-java", e);
    }

    return tmpDir;
  }
}
