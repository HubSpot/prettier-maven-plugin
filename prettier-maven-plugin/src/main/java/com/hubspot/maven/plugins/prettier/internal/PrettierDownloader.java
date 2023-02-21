package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class PrettierDownloader {
  private static final String PRETTIER_BIN_PATH = "node_modules/prettier/bin-prettier.js";
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
    Path prettierBin = prettierDirectory.resolve(PRETTIER_BIN_PATH);

    if (Files.exists(prettierDirectory) && Files.exists(prettierBin)) {
      log.debug("Reusing cached prettier-java at: " + prettierDirectory);
      return prettierDirectory;
    } else if (Files.exists(prettierDirectory) && !Files.exists(prettierBin)) {
      log.warn("Corrupted prettier install, going to delete and re-download");
      FileUtils.deleteDirectory(prettierDirectory);
    }

    try {
      Path tmpDir = installPrettierJavaToTmpDir(prettierJavaVersion);
      FileUtils.move(tmpDir, prettierDirectory);

      log.info("Downloaded prettier-java to: " + prettierDirectory);
      return prettierDirectory;
    } catch (IOException e) {
      throw new MojoExecutionException("Error downloading prettier-java", e);
    }
  }

  private Path installPrettierJavaToTmpDir(String prettierJavaVersion) throws MojoExecutionException, IOException {
    Path tmpDir = Files.createTempDirectory(
        installDirectory,
        "prettier-java-", OperatingSystemFamily.current().getGlobalPermissions()
    );

    List<String> command = new ArrayList<>(nodeInstall.getNpmCommand());
    command.add("install");
    command.add("--prefix");
    command.add(".");
    command.add("prettier-plugin-java@" + prettierJavaVersion);

    log.debug("Running npm install command: " + command);

    Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(tmpDir.toFile())
        .inheritIO()
        .start();

    try {
      int exitCode = process.waitFor();
      boolean prettierBinExists = Files.exists(tmpDir.resolve(PRETTIER_BIN_PATH));
      if (exitCode != 0) {
        throw new MojoExecutionException("Error downloading prettier-java, exit code: " + exitCode);
      }
      if (!prettierBinExists) {
        throw new MojoExecutionException("Error downloading prettier-java, prettier bin was not found");
      }
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Interrupted while downloading prettier-java", e);
    }

    return tmpDir;
  }
}
