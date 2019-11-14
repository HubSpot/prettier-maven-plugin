package com.hubspot.maven.plugins.prettier;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;

import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;

public abstract class AbstractPrettierMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = false)
  private MavenProject project;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "12.13.0")
  private String nodeVersion;

  @Parameter(defaultValue = "0.4.0")
  private String prettierJavaVersion;

  @Parameter(defaultValue = "${repositorySystemSession}", required = true, readonly = true)
  private RepositorySystemSession repositorySystemSession;

  @Component
  private PluginDescriptor pluginDescriptor;

  @Component
  private RepositorySystem repositorySystem;

  protected abstract String getPrettierCommand();
  protected abstract void handlePrettierNonZeroExit(int status) throws MojoExecutionException, MojoFailureException;

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    Path nodeExecutable = resolveNodeExecutable();

    Path targetDirectory = Paths.get(project.getBuild().getDirectory());
    extractPrettierJava(targetDirectory);

    Path prettierBin = targetDirectory
        .resolve("prettier-java")
        .resolve("node_modules")
        .resolve("prettier")
        .resolve("bin-prettier.js");

    Path prettierJavaPlugin = targetDirectory
        .resolve("prettier-java")
        .resolve("node_modules")
        .resolve("prettier-plugin-java");

    try {
      List<String> command = new ArrayList<>();
      command.add(nodeExecutable.toString());
      command.add(prettierBin.toString());
      command.add("--" + getPrettierCommand());
      command.add("**/*.java");
      command.add("--plugin=" + prettierJavaPlugin.toString());
      Process process = new ProcessBuilder(command.toArray(new String[0]))
          .redirectErrorStream(true)
          .start();
      BufferedReader output = new BufferedReader(
          new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8)
      );

      while (true) {
        final String line = output.readLine();
        if (line == null) {
          break;
        }
        getLog().info("[prettier] " + line);
      }
      final int status = process.waitFor();
      output.close();
      if (status != 0) {
        handlePrettierNonZeroExit(status);
      }
    } catch (IOException | InterruptedException e) {
      throw new MojoExecutionException("Error trying to run prettier-java", e);
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
      getLog().debug("Resolving node artifact: " + nodeArtifact);
    }

    File nodeExecutable = resolve(nodeArtifact).getFile();
    if (!nodeExecutable.setExecutable(true, false)) {
      throw new MojoExecutionException("Unable to make file executable: " + nodeExecutable);
    }

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolved node artifact to: " + nodeExecutable);
    }

    return nodeExecutable.toPath();
  }

  private void extractPrettierJava(Path extractionPath) throws MojoExecutionException {
    Artifact prettierArtifact = new DefaultArtifact(
        pluginDescriptor.getGroupId(),
        pluginDescriptor.getArtifactId(),
        determinePrettierJavaClassifier(),
        "zip",
        pluginDescriptor.getVersion()
    );

    if (getLog().isDebugEnabled()) {
      getLog().debug("Resolving prettier-java artifact: " + prettierArtifact);
    }


    File prettierZip = resolve(prettierArtifact).getFile();
    try {
      if (getLog().isDebugEnabled()) {
        getLog().debug("Extracting prettier-java to: " + extractionPath);
      }
      new ZipFile(prettierZip).extractAll(extractionPath.toString());
    } catch (ZipException e) {
      throw new MojoExecutionException("Error extracting prettier: " + prettierZip, e);
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
      throw new MojoExecutionException("Error resolving artifact: " + nodeVersion, e);
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
      throw new MojoExecutionException("Unknown os.name: " + osFullName);
    }

    return "node-" + nodeVersion + "-" + osShortName;
  }
}
