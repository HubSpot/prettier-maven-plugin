package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

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
      // don't leave the source dir hanging around
      deleteDirectory(source);

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

  private static void deleteDirectory(Path directory) throws MojoExecutionException {
    try {
      Files.walkFileTree(
          directory,
          new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
              if (exc != null) {
                throw exc;
              }

              Files.delete(dir);
              return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
              Files.delete(file);
              return FileVisitResult.CONTINUE;
            }
          }
      );
    } catch (IOException e) {
      throw new MojoExecutionException("Error cleaning up directory: " + directory, e);
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
