package com.hubspot.maven.plugins.prettier.diff;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

public interface DiffGenerator {
  void generateDiffs(GenerateDiffArgs args) throws MojoExecutionException, MojoFailureException;
}
