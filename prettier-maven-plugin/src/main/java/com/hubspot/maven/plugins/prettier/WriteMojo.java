package com.hubspot.maven.plugins.prettier;

import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "write", threadSafe = true)
public class WriteMojo extends AbstractPrettierMojo {
  private static final Predicate<String> COLORIZED_LINE = Pattern
    .compile("(\\x9B|\\x1B\\[)[0-?]*[ -/]*[@-~]")
    .asPredicate();

  @Override
  protected String getPrettierCommand() {
    return "write";
  }

  @Override
  protected void handlePrettierLogLine(String line) {
    // colorized lines have no changes
    if (line.endsWith("ms") && !COLORIZED_LINE.test(line)) {
      getLog().info("Reformatted file: " + line);
    }
  }

  @Override
  protected void handlePrettierNonZeroExit(int status) {}
}
