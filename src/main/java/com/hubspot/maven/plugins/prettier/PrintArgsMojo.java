package com.hubspot.maven.plugins.prettier;

import java.nio.file.Path;
import javax.annotation.Nullable;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;

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
    PrettierUtils prettierUtils = new PrettierUtils(
      project,
      nodeVersion,
      prettierJavaVersion,
      repositorySystemSession,
      pluginDescriptor,
      repositorySystem,
      getLog()
    );

    Path nodeExecutable = prettierUtils.resolveNodeExecutable();

    Path prettierJavaDirectory = prettierUtils.extractPrettierJava();

    Path prettierBin = prettierJavaDirectory
      .resolve("prettier-java")
      .resolve("node_modules")
      .resolve("prettier")
      .resolve("bin-prettier.js");

    Path prettierJavaPlugin = prettierJavaDirectory
      .resolve("prettier-java")
      .resolve("node_modules")
      .resolve("prettier-plugin-java");

    /*
    Use System.out rather than getLog() because we don't want any leading characters
     */
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
}
