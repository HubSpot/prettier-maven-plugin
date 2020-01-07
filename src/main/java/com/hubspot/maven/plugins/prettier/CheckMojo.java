package com.hubspot.maven.plugins.prettier;

import com.hubspot.maven.plugins.prettier.diff.DiffConfiguration;
import com.hubspot.maven.plugins.prettier.diff.DiffHandler;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

@Mojo(name = "check", threadSafe = true)
public class CheckMojo extends AbstractPrettierMojo {
  private static final String MESSAGE =
    "Code formatting issues found, please run prettier-java";

  @Parameter(defaultValue = "true")
  private boolean fail;

  @Parameter
  private DiffConfiguration diffConfiguration = new DiffConfiguration();

  @Override
  protected String getPrettierCommand() {
    return "check";
  }

  private final List<Path> incorrectlyFormattedFiles = new ArrayList<>();

  @Override
  protected void handlePrettierLogLine(String line) {
    if (line.endsWith(".java")) {
      incorrectlyFormattedFiles.add(Paths.get(line));
      String message = "Incorrectly formatted file: " + line;
      if (fail) {
        getLog().error(message);
      } else {
        getLog().warn(message);
      }
    }
  }

  @Override
  protected void handlePrettierNonZeroExit(int status)
    throws MojoFailureException, MojoExecutionException {
    if (status == 1) {
      if (incorrectlyFormattedFiles.isEmpty()) {
        throw new MojoExecutionException("Error trying to run prettier-java: " + status);
      } else {
        new DiffHandler(
            diffConfiguration,
            basePrettierCommand(),
            getLog()
        ).handle(incorrectlyFormattedFiles);

        if (fail) {
          getLog().error(MESSAGE);
          throw new MojoFailureException(MESSAGE);
        } else {
          getLog().warn(MESSAGE);
        }
      }
    } else {
      throw new MojoExecutionException("Error trying to run prettier-java: " + status);
    }
  }
}
