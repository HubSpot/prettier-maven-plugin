package com.hubspot.maven.plugins.prettier;

import com.google.common.io.Resources;
import com.hubspot.maven.plugins.prettier.internal.NodeDownloader;
import com.hubspot.maven.plugins.prettier.internal.NodeInstall;
import com.hubspot.maven.plugins.prettier.internal.OperatingSystemFamily;
import com.hubspot.maven.plugins.prettier.internal.PrettierDownloader;
import com.hubspot.maven.plugins.prettier.internal.PrettierPatcher;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

public abstract class PrettierArgs extends AbstractMojo {
  /**
   * Prevent multi-threaded builds from reading/writing partial files
   */
  private static final Object NODE_DOWNLOAD_LOCK = new Object();
  private static final Object PRETTIER_JAVA_DOWNLOAD_LOCK = new Object();

  @Parameter(defaultValue = "${project}", readonly = true, required = false)
  protected MavenProject project;

  @Parameter(defaultValue = "${settings.localRepository}")
  private String localRepository;

  @Parameter(defaultValue = "16.13.2", property = "prettier.nodeVersion")
  private String nodeVersion;

  @Parameter(defaultValue = "", property = "prettier.nodePath")
  private String nodePath;

  @Parameter(defaultValue = "", property = "prettier.npmPath")
  private String npmPath;

  @Parameter(defaultValue = "0.7.0", property = "prettier.prettierJavaVersion")
  private String prettierJavaVersion;

  // TODO remove
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

  @Parameter(defaultValue = "false", property = "prettier.disableGenericsLinebreaks")
  protected boolean disableGenericsLinebreaks;

  @Nullable
  @Parameter(property = "prettier.endOfLine")
  protected String endOfLine;

  @Parameter(property = "prettier.inputGlobs")
  protected List<String> inputGlobs;

  @Component
  private PluginDescriptor pluginDescriptor;

  protected NodeInstall resolveNodeInstall() throws MojoExecutionException {
    Optional<String> maybeNode = Optional.empty();
    Optional<String> maybeNpm = Optional.empty();

    if (nodePath != null && !nodePath.isEmpty()) {
      getLog().info("Using customized nodePath: " + nodePath);
      maybeNode = Optional.of(nodePath);
    }

    if (npmPath != null && !npmPath.isEmpty()) {
      getLog().info("Using customized npmPath: " + npmPath);
      maybeNpm = Optional.of(npmPath);
    }

    if (maybeNode.isPresent() && maybeNpm.isPresent()) {
      return new NodeInstall(maybeNode.get(), Arrays.asList(maybeNpm.get()));
    } else {
      NodeInstall nodeInstall = downloadNode();

      if (maybeNode.isPresent()) {
        return new NodeInstall(maybeNode.get(), nodeInstall.getNpmCommand());
      } else if (maybeNpm.isPresent()) {
        return new NodeInstall(nodeInstall.getNodePath(), Arrays.asList(maybeNpm.get()));
      } else {
        return nodeInstall;
      }
    }
  }

  protected Path downloadPrettierJava(NodeInstall nodeInstall) throws MojoExecutionException, MojoFailureException {
    synchronized (PRETTIER_JAVA_DOWNLOAD_LOCK) {
      Path installDirectory = localRepositoryDirectory();

      PrettierDownloader prettierDownloader = new PrettierDownloader(
        installDirectory,
        nodeInstall,
        getLog()
      );

      Path prettierJava = prettierDownloader.downloadPrettierJava(prettierJavaVersion);

      if (disableGenericsLinebreaks) {
        if (prettierJavaVersion.startsWith("2")) {
          URL patch = Resources.getResource("no-linebreak-generics.patch");
          return new PrettierPatcher(prettierJava, getLog()).patch(patch);
        } else if ("1.5.0".compareTo(prettierJavaVersion) > 0) {
          // versions before 1.5.0 don't linebreak generics
          return prettierJava;
        } else {
          getLog().error("Disabling generic linebreaks is only supported for prettier-java v2");
          throw new MojoFailureException("Disabling generic linebreaks is only supported for prettier-java v2");
        }
      } else {
        return prettierJava;
      }
    }
  }

  private NodeInstall downloadNode() throws MojoExecutionException {
    synchronized (NODE_DOWNLOAD_LOCK) {
      Path installDirectory = localRepositoryDirectory();

      try {
        NodeDownloader nodeDownloader = new NodeDownloader(installDirectory, getLog());
        return nodeDownloader.download(nodeVersion);
      } catch (IOException e) {
        throw new MojoExecutionException("Error downloading node", e);
      }
    }
  }

  private Path localRepositoryDirectory() throws MojoExecutionException {
    Path localRepositoryDirectory = Paths
        .get(localRepository)
        .resolve(pluginDescriptor.getGroupId().replace('.', File.separatorChar))
        .resolve(pluginDescriptor.getArtifactId())
        .resolve(pluginDescriptor.getVersion());

    try {
      Files.createDirectories(
          localRepositoryDirectory,
          OperatingSystemFamily.current().getGlobalPermissions()
      );
    } catch (IOException e) {
      throw new MojoExecutionException("Error creating directory: " + localRepositoryDirectory, e);
    }

    return localRepositoryDirectory;
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
}
