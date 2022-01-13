package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;

import org.apache.maven.plugin.MojoExecutionException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class NpmDownloader {
  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

  public Path download(String version) throws MojoExecutionException, IOException {
    OperatingSystemFamily os = OperatingSystemFamily.current();
    String downloadUrl = os.getNodeDownloadUrl(version);
    Path nodeArchive = downloadToTmpFile(downloadUrl);
    return os.extractToTmpDir(nodeArchive);
  }

  private Path downloadToTmpFile(String downloadUrl) throws IOException {
    Request request = new Request.Builder().url(downloadUrl).build();

    try (Response response = HTTP_CLIENT.newCall(request).execute();
         InputStream responseStream = response.body().byteStream()) {
      Path tempFile = null; // TODO random guid in local repo
      Files.copy(responseStream, tempFile);

      return tempFile;
    }
  }

  private enum OperatingSystemFamily {
    LINUX_X64("linux-x64.tar.gz"),
    MAC_X64("darwin-x64.tar.gz"),
    MAC_ARM("darwin-arm64.tar.gz"),
    WINDOWS_X64("win-x64.zip");

    private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString(
        "rwxrwxrwx"
    );

    private final String suffix;

    OperatingSystemFamily(String suffix) {
      this.suffix = suffix;
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
        if ("aarch64".equals(System.getProperty("os.arch"))) {
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

    public String getNodeDownloadUrl(String version) {
      return String.format("https://nodejs.org/dist/v%s/node-v%s-%s", version, version, suffix);
    }

    public Path extractToTmpDir(Path nodeArchive) {

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
}
