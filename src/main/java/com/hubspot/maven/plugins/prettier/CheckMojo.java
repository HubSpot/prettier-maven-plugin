package com.hubspot.maven.plugins.prettier;

import com.hubspot.maven.plugins.prettier.diff.DiffGenerator;
import com.hubspot.maven.plugins.prettier.diff.GenerateDiffArgs;
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

  @Parameter(defaultValue = "false")
  private boolean generateDiff;

  @Parameter(property = "prettier.diffGenerator", alias = "diffGenerator", defaultValue = "com.hubspot.maven.plugins.prettier.DefaultDiffGenerator")
  private String diffGeneratorType;

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
    if (status != 1 || incorrectlyFormattedFiles.isEmpty()) {
      throw new MojoExecutionException("Error trying to run prettier-java: " + status);
    }

    if (generateDiff) {
      generateDiff();
    }

    if (fail) {
      getLog().error(MESSAGE);
      throw new MojoFailureException(MESSAGE);
    } else {
      getLog().warn(MESSAGE);
    }
  }

  private void generateDiff() throws MojoExecutionException, MojoFailureException {
    DiffGenerator diffGenerator = instantiateDiffGenerator();
    GenerateDiffArgs args = new GenerateDiffArgs(
        incorrectlyFormattedFiles,
        basePrettierCommand(),
        project,
        getLog()
    );

    diffGenerator.generateDiffs(args);
  }

  private DiffGenerator instantiateDiffGenerator() throws MojoExecutionException {
    try {
      return (DiffGenerator) Class.forName(diffGeneratorType).getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException e) {
      throw new MojoExecutionException("Unable to find diff generator implementation", e);
    } catch (ReflectiveOperationException e) {
      throw new MojoExecutionException("Unable to instantiate diff generator", e);
    } catch (ClassCastException e) {
      throw new MojoExecutionException("Must implement DiffGenerator interface", e);
    }
  }
}
