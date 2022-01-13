package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.apache.maven.plugin.MojoExecutionException;

public class FileUtils {

  public static void move(Path source, Path target) throws MojoExecutionException {
    try {
      Files.move(
        source,
        target,
        StandardCopyOption.ATOMIC_MOVE
      );
    } catch (IOException e) {
      if (isIgnorableMoveError(e)) {
        // should be a harmless race condition
      } else {
        String message = String.format(
            "Error moving directory from %s to %s",
            source,
            target
        );

        throw new MojoExecutionException(message, e);
      }
    }
  }

  private static boolean isIgnorableMoveError(IOException e) {
    return (
      e instanceof FileAlreadyExistsException ||
      e instanceof DirectoryNotEmptyException ||
      (e instanceof FileSystemException && e.getMessage().contains("Directory not empty"))
    );
  }
}
