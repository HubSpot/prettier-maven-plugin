package com.hubspot.maven.plugins.prettier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    if (Files.isDirectory(extractionPath)) {
      log.debug("Reusing cached prettier-java at " + extractionPath);
      return extractionPath;
    } else {
      log.debug("Extracting prettier-java to " + extractionPath);
    }

    File prettierZip = prettierArtifact.getFile();
    try {
      new ZipFile(prettierZip).extractAll(extractionPath.toString());
    } catch (ZipException e) {
      throw new MojoExecutionException("Error extracting prettier " + prettierZip, e);
    }

    return extractionPath;
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
      result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
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
}
