package com.hubspot.maven.plugins.prettier;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "write", requiresProject = false, threadSafe = true)
public class WriteMojo extends AbstractPrettierMojo {

  @Override
  protected String getPrettierCommand() {
    return "write";
  }

  @Override
  protected void handlePrettierNonZeroExit(int status) throws MojoExecutionException {
    throw new MojoExecutionException(
        "Error trying to format code with prettier-java: " + status
    );
  }
}
