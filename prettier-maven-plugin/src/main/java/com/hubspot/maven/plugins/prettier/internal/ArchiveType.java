package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;

public enum ArchiveType {
  ZIP("zip") {

    @Override
    public void extract(Path targetDirectory, Path archive) throws IOException {
      try (
          InputStream inputStream = Files.newInputStream(archive);
          ZipInputStream zipInputStream = new ZipInputStream(inputStream)
      ) {
        ZipEntry zipEntry;
        while ((zipEntry = zipInputStream.getNextEntry()) != null) {
          Path entryPath = targetDirectory.resolve(zipEntry.getName()).normalize();
          if (!entryPath.startsWith(targetDirectory)) {
            throw new IOException("Invalid zip entry: " + zipEntry.getName());
          }

          if (zipEntry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Path parent = entryPath.getParent();
            if (parent != null) {
              Files.createDirectories(parent);
            }

            Files.copy(zipInputStream, entryPath);
          }
        }
      }
    }
  }, TAR_GZ("tar.gz") {

    @Override
    public void extract(Path targetDirectory, Path archive) throws IOException {
      try (
          InputStream inputStream = Files.newInputStream(archive);
          GzipCompressorInputStream gzipInputStream = new GzipCompressorInputStream(inputStream);
          TarArchiveInputStream tarInputStream = new TarArchiveInputStream(gzipInputStream)
      ) {
        TarArchiveEntry tarEntry;
        while ((tarEntry = tarInputStream.getNextTarEntry()) != null) {
          Path entryPath = targetDirectory.resolve(tarEntry.getName()).normalize();
          if (!entryPath.startsWith(targetDirectory)) {
            throw new IOException("Invalid tar entry: " + tarEntry.getName());
          }

          if (tarEntry.isDirectory()) {
            Files.createDirectories(entryPath);
          } else {
            Path parent = entryPath.getParent();
            if (parent != null) {
              Files.createDirectories(parent);
            }

            Files.copy(tarInputStream, entryPath);
          }
        }
      }
    }
  };

  private final String asString;

  ArchiveType(String asString) {
    this.asString = asString;
  }

  public String asString() {
    return asString;
  }

  public abstract void extract(Path targetDirectory, Path archive) throws IOException;
}
