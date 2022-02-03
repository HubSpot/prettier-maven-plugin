package com.hubspot.maven.plugins.prettier.internal;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class NodeDownloader {
  private static final OkHttpClient HTTP_CLIENT = new OkHttpClient();

  private final Path installDirectory;
  private final Log log;

  public NodeDownloader(Path installDirectory, Log log) {
    this.installDirectory = installDirectory;
    this.log = log;
  }

  public NodeInstall download(String version) throws MojoExecutionException, IOException {
    OperatingSystemFamily os = OperatingSystemFamily.current();
    log.debug("Determined os: " + os);

    Path targetDirectory = installDirectory.resolve(os.getNodeDirectoryName(version));
    if (Files.exists(targetDirectory)) {
      log.debug("Reusing cached node at: " + targetDirectory);
    } else {
      String downloadUrl = os.getNodeDownloadUrl(version);
      log.debug("Downloading node from url: " + downloadUrl);

      Path nodeArchive = downloadToTmpFile(downloadUrl);
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

  private Path downloadToTmpFile(String downloadUrl) throws IOException {
    Request request = new Request.Builder().url(downloadUrl).build();

    try (Response response = HTTP_CLIENT.newCall(request).execute();
         InputStream responseStream = responseStream(response)) {
      Path tempFile = Files.createTempFile(installDirectory, "node-", ".tmp");
      Files.copy(responseStream, tempFile, StandardCopyOption.REPLACE_EXISTING);
      tempFile.toFile().deleteOnExit();

      return tempFile;
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
