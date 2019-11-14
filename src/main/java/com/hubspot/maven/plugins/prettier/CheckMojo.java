package com.hubspot.maven.plugins.prettier;

import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check", requiresProject = false, threadSafe = true)
public class CheckMojo extends AbstractPrettierMojo {

  @Parameter(defaultValue = "true")
  private boolean fail;

  @Override
  protected String getPrettierCommand() {
    return "check";
  }
}
