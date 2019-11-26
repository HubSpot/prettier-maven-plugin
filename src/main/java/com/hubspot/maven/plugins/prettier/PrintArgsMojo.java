package com.hubspot.maven.plugins.prettier;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.annotation.Nullable;
import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.ZipFile;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

// TODO share code with AbstractPrettierMojo
@Mojo(name = "print-args", requiresProject = false)
public class PrintArgsMojo extends AbstractMojo {
  @Parameter(defaultValue = "${project}", readonly = true, required = false)
  private MavenProject project;

  @Parameter(defaultValue = "12.13.0")
  private String nodeVersion;

  @Parameter(defaultValue = "0.4.0")
  private String prettierJavaVersion;

  @Nullable
  @Parameter(property = "prettier.printWidth")
  private String printWidth;

  @Nullable
  @Parameter(property = "prettier.tabWidth")
  private String tabWidth;

  @Nullable
  @Parameter(property = "prettier.useTabs")
  private Boolean useTabs;

  @Parameter(
    defaultValue = "${repositorySystemSession}",
    required = true,
    readonly = true
  )
  private RepositorySystemSession repositorySystemSession;

  @Component
  private PluginDescriptor pluginDescriptor;

  @Component
  private RepositorySystem repositorySystem;

  @Override
  public final void execute() throws MojoExecutionException {
    Path nodeExecutable = resolveNodeExecutable();

    Path prettierJavaDirectory = extractPrettierJava();

    Path prettierBin = prettierJavaDirectory
      .resolve("prettier-java")
      .resolve("node_modules")
      .resolve("prettier")
      .resolve("bin-prettier.js");

    Path prettierJavaPlugin = prettierJavaDirectory
      .resolve("prettier-java")
      .resolve("node_modules")
      .resolve("prettier-plugin-java");

    System.out.println("nodeExecutable=" + nodeExecutable);
    System.out.println("prettierBin=" + prettierBin);
    System.out.println("prettierJavaPlugin=" + prettierJavaPlugin);
    if (printWidth != null) {
      System.out.println("printWidth=" + printWidth);
    }
    if (tabWidth != null) {
      System.out.println("tabWidth=" + tabWidth);
    }
    if (useTabs != null) {
      System.out.println("useTabs=" + useTabs);
    }
  }

  private Path resolveNodeExecutable() throws MojoExecutionException {
    Artifact nodeArtifact = new DefaultArtifact(
      pluginDescriptor.getGroupId(),
      pluginDescriptor.getArtifactId(),
      determineNodeClassifier(),
      "exe",
      pluginDescriptor.getVersion()
    );

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolving node artifact " + nodeArtifact);
    }

    File nodeExecutable = resolve(nodeArtifact).getFile();
    if (!nodeExecutable.setExecutable(true, false)) {
      throw new MojoExecutionException(
        "Unable to make file executable " + nodeExecutable
      );
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolved node artifact to " + nodeExecutable);
    }

    return nodeExecutable.toPath();
  }

  private Path extractPrettierJava() throws MojoExecutionException {
    Artifact prettierArtifact = new DefaultArtifact(
      pluginDescriptor.getGroupId(),
      pluginDescriptor.getArtifactId(),
      determinePrettierJavaClassifier(),
      "zip",
      pluginDescriptor.getVersion()
    );

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolving prettier-java artifact " + prettierArtifact);
    }

    prettierArtifact = resolve(prettierArtifact);
    Path extractionPath = determinePrettierJavaExtractionPath(prettierArtifact);
    if (Files.isDirectory(extractionPath)) {
      getLog().debug("Reusing cached prettier-java at " + extractionPath);
      return extractionPath;
    } else {
      getLog().debug("Extracting prettier-java to " + extractionPath);
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
