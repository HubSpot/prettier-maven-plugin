package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NodeDownloader {
  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

  private final Optional<String> customDownloadUrl;
  private final Path installDirectory;
  private final Log log;

  public NodeDownloader(Optional<String> customDownloadUrl, Path installDirectory, Log log) {
    this.customDownloadUrl = customDownloadUrl;
    this.installDirectory = installDirectory;
    this.log = log;
  }

  public NodeInstall download(String version) throws MojoExecutionException, IOException {
    OperatingSystemFamily os = OperatingSystemFamily.current();
    log.debug("Determined os: " + os);
    return download(version, os);
  }

  public NodeInstall download(String version, OperatingSystemFamily os) throws MojoExecutionException, IOException {
    Path targetDirectory = installDirectory.resolve(os.getNodeDirectoryName(version));
    if (Files.exists(targetDirectory)) {
      log.debug("Reusing cached node at: " + targetDirectory);
    } else {
      String downloadUrl = customDownloadUrl.orElseGet(() -> os.getNodeDownloadUrl(version));
      log.debug("Downloading node from url: " + downloadUrl);

      Optional<Path> maybeNodeArchive = downloadToTmpFile(downloadUrl);
      if (!maybeNodeArchive.isPresent()) {
        Optional<OperatingSystemFamily> fallback = os.getFallback();
        if (fallback.isPresent()) {
          return download(version, fallback.get());
        } else {
          throw new MojoExecutionException("Got 404 when trying to download node from: " + downloadUrl);
        }
      }

      Path nodeArchive = maybeNodeArchive.get();
      log.debug("Downloaded node to: " + nodeArchive);

      Path tmpDir = os.extractToTmpDir(installDirectory, nodeArchive);
      Files.delete(nodeArchive);
      log.debug("Extracted node to: " + tmpDir);

      Path nodeDir = tmpDir.resolve(os.getNodeDirectoryName(version));

      if (os != OperatingSystemFamily.WINDOWS_X64) {
        NodeInstall nodeInstall = os.toNodeInstall(nodeDir);
        os.setGlobalPermissions(Paths.get(nodeInstall.getNodePath()));
      }

      FileUtils.move(nodeDir, targetDirectory);
      Files.delete(tmpDir);
      log.debug("Moved node to: " + targetDirectory);
    }

    return os.toNodeInstall(targetDirectory);
  }

  private Optional<Path> downloadToTmpFile(String downloadUrl) throws MojoExecutionException, IOException {
    Request request = new Request.Builder().url(downloadUrl).build();

    try (Response response = HTTP_CLIENT.newCall(request).execute();
         InputStream responseStream = responseStream(response)) {
      if (response.code() == 404) {
        log.debug("Got 404 when trying to download node from: " + downloadUrl);
        return Optional.empty();
      } else if (!response.isSuccessful()) {
        throw new MojoExecutionException(
            "Got response code " + response.code() + " when trying to download node from: " + downloadUrl
        );
      } else {
        Path tempFile = Files.createTempFile(installDirectory, "node-", ".tmp");
        Files.copy(responseStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
        tempFile.toFile().deleteOnExit();

        return Optional.of(tempFile);
      }
    }
  }

  private static InputStream responseStream(Response response) throws IOException {
    ResponseBody body = response.body();
    if (body == null) {
      throw new IOException("Null body returned for response: " + response);
    }

    return body.byteStream();
  }
}
