package com.hubspot.maven.plugins.prettier;

import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "write", requiresProject = false, threadSafe = true)
public class WriteMojo extends AbstractPrettierMojo {

  @Override
  protected String getPrettierCommand() {
    return "write";
  }
}
