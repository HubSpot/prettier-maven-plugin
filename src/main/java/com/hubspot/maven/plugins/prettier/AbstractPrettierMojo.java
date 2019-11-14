package com.hubspot.maven.plugins.prettier;

import java.io.File;
import java.nio.file.Path;

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

public abstract class AbstractPrettierMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true, required = true)
  private MavenProject project;

  @Parameter(defaultValue = "false")
  private boolean skip;

  @Parameter(defaultValue = "12.13.0")
  private String nodeVersion;

  @Parameter(defaultValue = "1.19.1")
  private String prettierVersion;

  @Component
  private PluginDescriptor pluginDescriptor;

  @Component
  private RepositorySystem repositorySystem;

  @Component
  private RepositorySystemSession repositorySystemSession;

  protected abstract String getPrettierCommand();

  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      getLog().info("Skipping plugin execution");
      return;
    }

    // resolve node/prettier/prettier-java
    // run prettier via our node binary with prettier-java plugin
  }

  private Path resolveNodeExecutable() throws MojoExecutionException {
    Artifact nodeArtifact = new DefaultArtifact(
        pluginDescriptor.getGroupId(),
        pluginDescriptor.getArtifactId(),
        determineNodeClassifier(),
        "exe",
        pluginDescriptor.getVersion()
    );

    ArtifactRequest artifactRequest = new ArtifactRequest()
        .setArtifact(nodeArtifact)
        .setRepositories(project.getRemoteProjectRepositories());

    final ArtifactResult result;
    try {
      result = repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Error resolving artifact: " + nodeVersion, e);
    }

    File nodeExecutable = result.getArtifact().getFile();
    if (!nodeExecutable.setExecutable(true, false)) {
      throw new MojoExecutionException("Unable to make file executable: " + nodeExecutable);
    }

    return nodeExecutable.toPath();
  }

  private String determineNodeClassifier() throws MojoExecutionException {
    String osFullName = System.getProperty("os.name");
    if (osFullName == null) {
      throw new MojoExecutionException("No os.name system property set");
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
