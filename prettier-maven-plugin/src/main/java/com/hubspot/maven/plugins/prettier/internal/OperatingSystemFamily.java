package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import org.apache.maven.plugin.MojoExecutionException;

public enum OperatingSystemFamily {
  LINUX_X64("linux-x64", ArchiveType.TAR_GZ),
  MAC_X64("darwin-x64", ArchiveType.TAR_GZ),
  MAC_ARM("darwin-arm64", ArchiveType.TAR_GZ),
  WINDOWS_X64("win-x64", ArchiveType.ZIP);

  private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString(
      "rwxrwxrwx"
  );

  private final String classifier;
  private final ArchiveType extension;

  OperatingSystemFamily(String classifier, ArchiveType extension) {
    this.classifier = classifier;
    this.extension = extension;
  }

  public static OperatingSystemFamily current() throws MojoExecutionException {
    String osFullName = System.getProperty("os.name");
    if (osFullName == null) {
      throw new MojoExecutionException("No os.name system property set");
    } else {
      osFullName = osFullName.toLowerCase();
    }

    if (osFullName.startsWith("linux")) {
      return OperatingSystemFamily.LINUX_X64;
    } else if (osFullName.startsWith("mac os x")) {
      if ("aarch64".equalsIgnoreCase(System.getProperty("os.arch"))) {
        return OperatingSystemFamily.MAC_ARM;
      } else {
        return OperatingSystemFamily.MAC_X64;
      }
    } else if (osFullName.startsWith("windows")) {
      return OperatingSystemFamily.WINDOWS_X64;
    } else {
      throw new MojoExecutionException("Unknown os.name " + osFullName);
    }
  }

  public Optional<OperatingSystemFamily> getFallback() {
    if (this == MAC_ARM) {
      return Optional.of(MAC_X64);
    } else {
      return Optional.empty();
    }
  }

  public String getNodeDownloadUrl(String version) {
    return String.format(
        "https://nodejs.org/dist/v%s/node-v%s-%s.%s",
        version,
        version,
        classifier,
        extension.asString()
    );
  }

  public String getNodeDirectoryName(String version) {
    return String.format("node-v%s-%s", version, classifier);
  }

  public NodeInstall toNodeInstall(Path installDirectory) {
    final Path nodePath;
    final Path npmCliPath;
    if (this == WINDOWS_X64) {
      nodePath = installDirectory.resolve("node.exe");
      npmCliPath = installDirectory.resolve("node_modules").resolve("npm").resolve("bin").resolve("npm-cli.js");
    } else {
      nodePath = installDirectory.resolve("bin/node");
      npmCliPath = installDirectory.resolve("lib/node_modules/npm/bin/npm-cli.js");
    }

    return new NodeInstall(
      nodePath.toString(),
      Arrays.asList(nodePath.toString(), npmCliPath.toString())
    );
  }

  public Path extractToTmpDir(Path installDirectory, Path nodeArchive) throws IOException {
    Path tmpDir = Files.createTempDirectory(installDirectory, "node-", getGlobalPermissions());
    extension.extract(tmpDir, nodeArchive);

    return tmpDir;
  }

  public void setGlobalPermissions(Path path) throws IOException {
    if (this != WINDOWS_X64) {
      Files.setPosixFilePermissions(path, OperatingSystemFamily.GLOBAL_PERMISSIONS);
    }
  }

  public FileAttribute<?>[] getGlobalPermissions() {
    if (this == WINDOWS_X64) {
      return new FileAttribute<?>[0];
    } else {
      return new FileAttribute<?>[] {
          PosixFilePermissions.asFileAttribute(GLOBAL_PERMISSIONS)
      };
    }
  }
}
