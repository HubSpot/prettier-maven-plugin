package com.hubspot.maven.plugins.prettier;

import java.nio.file.Path;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "print-args", requiresProject = false)
public class PrintArgsMojo extends PrettierArgs {

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
    if (endOfLine != null) {
      System.out.println("endOfLine=" + endOfLine);
    }
    if (ignoreConfigFile) {
      System.out.println("noConfig=true");
    }
    if (ignoreEditorConfig) {
      System.out.println("noEditorconfig=true");
    }
    computeInputGlobs().forEach(inputGlob -> System.out.println("inputGlob=" + inputGlob));
  }
}
