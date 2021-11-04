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
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.annotation.Nullable;
import net.lingala.zip4j.ZipFile;
import net.lingala.zip4j.exception.ZipException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
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

public abstract class PrettierArgs extends AbstractMojo {
  private static final Set<PosixFilePermission> GLOBAL_PERMISSIONS = PosixFilePermissions.fromString(
      "rwxrwxrwx"
  );

  /**
   * Prevent multi-threaded builds from reading/writing partial files
   */
  private static final Object RESOLUTION_LOCK = new Object();
  private static final Object EXTRACTION_LOCK = new Object();

  @Parameter(defaultValue = "${project}", readonly = true, required = false)
  protected MavenProject project;

  @Parameter(defaultValue = "12.13.0", property = "prettier.nodeVersion")
  private String nodeVersion;

  @Parameter(defaultValue = "0.7.0", property = "prettier.prettierJavaVersion")
  private String prettierJavaVersion;

  @Parameter(defaultValue = "false")
  private boolean extractPrettierToTargetDirectory;

  @Nullable
  @Parameter(property = "prettier.printWidth")
  protected String printWidth;

  @Nullable
  @Parameter(property = "prettier.tabWidth")
  protected String tabWidth;

  @Nullable
  @Parameter(property = "prettier.useTabs")
  protected Boolean useTabs;

  @Parameter(defaultValue = "false", property = "prettier.ignoreConfigFile")
  protected boolean ignoreConfigFile;

  @Parameter(defaultValue = "false", property = "prettier.ignoreEditorConfig")
  protected boolean ignoreEditorConfig;

  @Nullable
  @Parameter(property = "prettier.endOfLine")
  protected String endOfLine;

  @Parameter(property = "prettier.inputGlobs")
  protected List<String> inputGlobs;

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

  protected Path resolveNodeExecutable() throws MojoExecutionException {
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

  protected Path extractPrettierJava() throws MojoExecutionException {
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

    synchronized (EXTRACTION_LOCK) {
      if (Files.isDirectory(extractionPath)) {
        getLog().debug("Reusing cached prettier-java at " + extractionPath);
        return extractionPath;
      }

      Path tempDir = extractionPath.resolveSibling(UUID.randomUUID().toString());
      try {
        Files.createDirectories(
            tempDir,
            determineOperatingSystemFamily().getGlobalPermissions()
        );
      } catch (IOException e) {
        throw new MojoExecutionException("Error creating temp directory: " + tempDir, e);
      }

      getLog().debug("Extracting prettier-java to " + tempDir);
      File prettierZip = prettierArtifact.getFile();
      try {
        new ZipFile(prettierZip).extractAll(tempDir.toString());
      } catch (ZipException e) {
        throw new MojoExecutionException("Error extracting prettier " + prettierZip, e);
      }

      getLog().debug("Copying prettier-java to " + extractionPath);
      try {
        Files.move(tempDir, extractionPath, StandardCopyOption.ATOMIC_MOVE);
      } catch (IOException e) {
        if (isIgnorableMoveError(e)) {
          // should be a harmless race condition
          getLog().debug("Directory already created at: " + extractionPath);
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

  protected List<String> computeInputGlobs() {
    if (inputGlobs.isEmpty()) {
      return defaultInputGlobs();
    } else {
      return inputGlobs;
    }
  }

  private List<String> defaultInputGlobs() {
    List<String> defaultGlobs = new ArrayList<>();

    // don't use compile source roots because it seems to include generated sources
    Path sourceDirectory = Paths.get(project.getBuild().getSourceDirectory());
    Path testSourceDirectory = Paths.get(project.getBuild().getTestSourceDirectory());

    if (Files.isDirectory(sourceDirectory)) {
      defaultGlobs.add(project.getBasedir().toPath().relativize(sourceDirectory) + "/**/*.java");
    }
    if (Files.isDirectory(testSourceDirectory)) {
      defaultGlobs.add(project.getBasedir().toPath().relativize(testSourceDirectory) + "/**/*.java");
    }

    return defaultGlobs;
  }

  private Path determinePrettierJavaExtractionPath(Artifact prettierArtifact) {
    String directoryName = String.join(
        "-",
        prettierArtifact.getArtifactId(),
        prettierArtifact.getVersion(),
        prettierArtifact.getClassifier()
    );

    // check for unresolved snapshot
    if (extractPrettierToTargetDirectory || isUnresolvedSnapshot(prettierArtifact)) {
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
        result =
            repositorySystem.resolveArtifact(repositorySystemSession, artifactRequest);
      }
    } catch (ArtifactResolutionException e) {
      throw new MojoExecutionException("Error resolving artifact " + nodeVersion, e);
    }

    return result.getArtifact();
  }

  private String determinePrettierJavaClassifier() {
    return "prettier-java-" + prettierJavaVersion;
  }

  private enum OperatingSystemFamily {
    LINUX("linux"), MAC_OS_X("mac_os_x"), WINDOWS("windows");

    private String shortName;

    OperatingSystemFamily(String shortName) {
      this.shortName = shortName;
    }

    public String getShortName() {
      return shortName;
    }

    public FileAttribute<?>[] getGlobalPermissions() {
      if (this == WINDOWS) {
        return new FileAttribute<?>[0];
      } else {
        return new FileAttribute<?>[] {
            PosixFilePermissions.asFileAttribute(GLOBAL_PERMISSIONS)
        };
      }
    }
  }

  private String determineNodeClassifier() throws MojoExecutionException {
    OperatingSystemFamily osFamily = determineOperatingSystemFamily();
    return "node-" + nodeVersion + "-" + osFamily.getShortName();
  }

  private OperatingSystemFamily determineOperatingSystemFamily() throws MojoExecutionException {
    String osFullName = System.getProperty("os.name");
    if (osFullName == null) {
      throw new MojoExecutionException("No os.name system property set");
    } else {
      osFullName = osFullName.toLowerCase();
    }

    if (osFullName.startsWith("linux")) {
      return OperatingSystemFamily.LINUX;
    } else if (osFullName.startsWith("mac os x")) {
      return OperatingSystemFamily.MAC_OS_X;
    } else if (osFullName.startsWith("windows")) {
      return OperatingSystemFamily.WINDOWS;
    } else {
      throw new MojoExecutionException("Unknown os.name " + osFullName);
    }
  }

  private static boolean isIgnorableMoveError(IOException e) {
    return (
        e instanceof FileAlreadyExistsException ||
        e instanceof DirectoryNotEmptyException ||
        (e instanceof FileSystemException && e.getMessage().contains("Directory not empty"))
    );
  }

  private static boolean isUnresolvedSnapshot(Artifact artifact) {
    return artifact.isSnapshot() && artifact.getVersion().endsWith("-SNAPSHOT");
  }
}
