package com.hubspot.maven.plugins.prettier.internal;

import com.google.common.io.Resources;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
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

  public Path patch(URL patch) throws MojoExecutionException {
    Path patchDirectory = originalDirectory.resolveSibling(originalDirectory.getFileName() + "-patched");

    if (Files.exists(patchDirectory)) {
      log.debug("Reusing patched prettier-java at: " + patchDirectory);
    } else {
      try {
        Path tmpDir = FileUtils.copyDirectory(originalDirectory);
        applyPatch(patch, tmpDir);
        FileUtils.move(tmpDir, patchDirectory);
        log.debug("Patched prettier-java to: " + patchDirectory);
      } catch (IOException e) {
        throw new MojoExecutionException("Error patching prettier-java", e);
      }
    }

    return patchDirectory;
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

    byte[] bytes = Resources.toByteArray(source);
    Files.write(tmpFile, bytes);

    return tmpFile.toFile();
  }
}
