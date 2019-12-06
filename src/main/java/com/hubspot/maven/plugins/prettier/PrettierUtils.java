package com.hubspot.maven.plugins.prettier;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileSystemException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Set;
import java.util.UUID;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

public class PrettierUtils {
  private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString("rwxrwxrwx");
  /**
   * Prevent multi-threaded builds from reading/writing partial files
   */
  private static final Object RESOLUTION_LOCK = new Object();
  private static final Object EXTRACTION_LOCK = new Object();

  private final MavenProject project;
  private final String nodeVersion;
  private final String prettierJavaVersion;
  private final RepositorySystemSession repositorySystemSession;
  private final PluginDescriptor pluginDescriptor;
  private final RepositorySystem repositorySystem;
  private final Log log;

  public PrettierUtils(
    MavenProject project,
    String nodeVersion,
    String prettierJavaVersion,
    RepositorySystemSession repositorySystemSession,
    PluginDescriptor pluginDescriptor,
    RepositorySystem repositorySystem,
    Log log
  ) {
    this.project = project;
    this.nodeVersion = nodeVersion;
    this.prettierJavaVersion = prettierJavaVersion;
    this.repositorySystemSession = repositorySystemSession;
    this.pluginDescriptor = pluginDescriptor;
    this.repositorySystem = repositorySystem;
    this.log = log;
  }

  public Path resolveNodeExecutable() throws MojoExecutionException {
    Artifact nodeArtifact = new DefaultArtifact(
      pluginDescriptor.getGroupId(),
      pluginDescriptor.getArtifactId(),
      determineNodeClassifier(),
      "exe",
      pluginDescriptor.getVersion()
    );

    if (log.isDebugEnabled()) {
      log.debug("Resolving node artifact " + nodeArtifact);
    }

    File nodeExecutable = resolve(nodeArtifact).getFile();
    if (!nodeExecutable.setExecutable(true, false)) {
      throw new MojoExecutionException(
        "Unable to make file executable " + nodeExecutable
      );
    }

    if (log.isDebugEnabled()) {
      log.debug("Resolved node artifact to " + nodeExecutable);
    }

    return nodeExecutable.toPath();
  }

  public Path extractPrettierJava() throws MojoExecutionException {
    Artifact prettierArtifact = new DefaultArtifact(
      pluginDescriptor.getGroupId(),
      pluginDescriptor.getArtifactId(),
      determinePrettierJavaClassifier(),
      "zip",
      pluginDescriptor.getVersion()
    );

    if (log.isDebugEnabled()) {
      log.debug("Resolving prettier-java artifact " + prettierArtifact);
    }

    prettierArtifact = resolve(prettierArtifact);
    Path extractionPath = determinePrettierJavaExtractionPath(prettierArtifact);

    synchronized (EXTRACTION_LOCK) {
      if (Files.isDirectory(extractionPath)) {
        log.debug("Reusing cached prettier-java at " + extractionPath);
        return extractionPath;
      }

      Path tempDir = extractionPath.resolveSibling(UUID.randomUUID().toString());
      try {
        Files.createDirectories(
            tempDir,
            PosixFilePermissions.asFileAttribute(GLOBAL_PERMISSIONS)
        );
      } catch (IOException e) {
        throw new MojoExecutionException("Error creating temp directory: " + tempDir, e);
      }

      log.debug("Extracting prettier-java to " + tempDir);
      File prettierZip = prettierArtifact.getFile();
      try {
        new ZipFile(prettierZip).extractAll(tempDir.toString());
      } catch (ZipException e) {
        throw new MojoExecutionException("Error extracting prettier " + prettierZip, e);
      }

      log.debug("Copying prettier-java to " + extractionPath);
      try {
        Files.move(tempDir, extractionPath, StandardCopyOption.ATOMIC_MOVE);
      } catch (FileAlreadyExistsException | DirectoryNotEmptyException e) {
        // should be a harmless race condition
        log.debug("Directory already created at: " + extractionPath);
      } catch (IOException e) {
        if (isIgnorableMoveError(e)) {
          // should be a harmless race condition
          log.debug("Directory already created at: " + extractionPath);
        } else {
          String message = String.format(
              "Error moving directory from %s to %s",
              tempDir,
              extractionPath
          );

          throw new MojoExecutionException(message, e);
        }
      }

      return extractionPath;
    }
  }

  private Path determinePrettierJavaExtractionPath(Artifact prettierArtifact) {
    String directoryName = String.join(
      "-",
      prettierArtifact.getArtifactId(),
      prettierArtifact.getVersion(),
      prettierArtifact.getClassifier()
    );

    // check for unresolved snapshot
    if (
      prettierArtifact.isSnapshot() && prettierArtifact.getVersion().endsWith("-SNAPSHOT")
    ) {
      // in this case, extract into target dir since we can't trust the local repo
      return Paths.get(project.getBuild().getDirectory()).resolve(directoryName);
    } else {
      return prettierArtifact.getFile().toPath().resolveSibling(directoryName);
    }
  }

  private Artifact resolve(Artifact artifact) throws MojoExecutionException {
    ArtifactRequest artifactRequest = new ArtifactRequest()
      .setArtifact(artifact)
      .setRepositories(project.getRemoteProjectRepositories());

    final ArtifactResult result;
    try {
      synchronized (RESOLUTION_LOCK) {
        result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
      }
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Error resolving artifact " + nodeVersion, e);
    }

    return result.getArtifact();
  }

  private String determinePrettierJavaClassifier() {
    return "prettier-java-" + prettierJavaVersion;
  }

  private String determineNodeClassifier() throws MojoExecutionException {
    String osFullName = System.getProperty("os.name");
    if (osFullName == null) {
      throw new MojoExecutionException("No os.name system property set");
    } else {
      osFullName = osFullName.toLowerCase();
    }

    final String osShortName;
    if (osFullName.startsWith("linux")) {
      osShortName = "linux";
    } else if (osFullName.startsWith("mac os x")) {
      osShortName = "mac_os_x";
    } else if (osFullName.startsWith("windows")) {
      osShortName = "windows";
    } else {
      throw new MojoExecutionException("Unknown os.name " + osFullName);
    }

    return "node-" + nodeVersion + "-" + osShortName;
  }

  private static boolean isIgnorableMoveError(IOException e) {
    return e instanceof FileAlreadyExistsException ||
           e instanceof DirectoryNotEmptyException ||
           (e instanceof FileSystemException &&
            e.getMessage().contains("Directory not empty"));
  }
}
