package com.hubspot.maven.plugins.prettier.internal;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

public class PrettierPatcher {
  private final Path originalDirectory;
  private final Log log;

  public PrettierPatcher(Path originalDirectory, Log log) {
    this.originalDirectory = originalDirectory;
    this.log = log;
  }

  public Path patch(URL patch, String prettierJavaVersion) throws MojoExecutionException {
    Path patchDirectory = originalDirectory.resolveSibling(originalDirectory.getFileName() + "-patched");
    Path prettierBin = patchDirectory.resolve(PrettierPaths.prettierBinPath(prettierJavaVersion));

    if (Files.exists(patchDirectory) && Files.exists(prettierBin)) {
      log.debug("Reusing patched prettier-java at: " + patchDirectory);
      return patchDirectory;
    } else if (Files.exists(patchDirectory) && !Files.exists(prettierBin)) {
      log.warn("Corrupted patched prettier install, going to delete and re-download");
      FileUtils.deleteDirectory(patchDirectory);
    }

    try {
      Path tmpDir = FileUtils.copyDirectory(originalDirectory);
      applyPatch(patch, tmpDir);
      FileUtils.move(tmpDir, patchDirectory);

      log.info("Patched prettier-java to: " + patchDirectory);
      return patchDirectory;
    } catch (IOException e) {
      throw new MojoExecutionException("Error patching prettier-java", e);
    }
  }

  private void applyPatch(URL patch, Path directory) throws MojoExecutionException, IOException {
    List<String> command = Arrays.asList("patch", "-p1", "-f");
    log.debug("Running patch command: " + command);

    Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(directory.toFile())
        .redirectInput(copyToFile(patch))
        .redirectOutput(Redirect.INHERIT)
        .redirectError(Redirect.INHERIT)
        .start();

    try {
      int exitCode = process.waitFor();
      if (exitCode != 0) {
        throw new MojoExecutionException("Error patching prettier-java, exit code: " + exitCode);
      }
    } catch (InterruptedException e) {
      throw new MojoExecutionException("Interrupted while patching prettier-java", e);
    }
  }

  private File copyToFile(URL source) throws IOException {
    Path tmpFile = Files.createTempFile(originalDirectory.getParent(), "prettier-", ".patch");
    tmpFile.toFile().deleteOnExit();

    try (
      ReadableByteChannel inputChannel = Channels.newChannel(source.openStream());
      FileChannel outputChannel = FileChannel.open(tmpFile, StandardOpenOption.WRITE)
    ) {
      outputChannel.transferFrom(inputChannel, 0, Long.MAX_VALUE);
    }
    return tmpFile.toFile();
  }
}
