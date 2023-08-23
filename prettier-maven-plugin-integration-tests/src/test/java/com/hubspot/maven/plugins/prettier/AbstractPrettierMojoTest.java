package com.hubspot.maven.plugins.prettier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class AbstractPrettierMojoTest {
  private static final Logger LOG = Logger.getLogger(AbstractPrettierMojoTest.class.getName());

  protected static final String JAVA_GOOD_FORMATTING = "java-good-formatting/*.java";
  protected static final String JAVA_BAD_FORMATTING = "java-bad-formatting/*.java";
  protected static final String JAVA_BAD_FORMATTING_ERROR_PATH = "java-bad-formatting-error-path/*.java";
  protected static final String JAVA_INVALID_SYNTAX = "java-invalid-syntax/*.java";
  protected static final String JS_GOOD_FORMATTING = "js-good-formatting/*.js";
  protected static final String JS_BAD_FORMATTING = "js-bad-formatting/*.js";
  protected static final String JS_INVALID_SYNTAX = "js-invalid-syntax/*.js";
  protected static final String UNKNOWN_EXTENSION = "unknown-extension/*.unknown";
  protected static final String EMPTY = "empty/*.java";
  protected static final String BUILD_SUCCESS = "BUILD SUCCESS";
  protected static final String BUILD_FAILURE = "BUILD FAILURE";
  private static final Set<String> PRETTIER_JAVA_VERSIONS_TO_TEST = Set.of("1.6.2", "2.0.0", "2.2.0");

  protected static Set<String> getPrettierJavaVersionsToTest() {
    return PRETTIER_JAVA_VERSIONS_TO_TEST;
  }

  protected static MavenResult runMaven(TestConfiguration testConfiguration) throws IOException {
    Path temp = setupTestDirectory(testConfiguration);

    LOG.log(
        Level.INFO,
        "Testing prettier-java={0}, goal={1}, input={2}",
        new Object[] {
          testConfiguration.getPrettierJavaVersion(),
          testConfiguration.getGoal(),
          testConfiguration.getInputGlobs()
        }
    );

    List<String> command = Arrays.asList(
        "mvn",
        "-e",
        "--batch-mode",
        "-Dstyle.color=never",
        "-Daether.artifactResolver.snapshotNormalization=false",
        "-Daether.connector.resumeDownloads=false",
        "validate"
    );

    Process process = new ProcessBuilder(command.toArray(new String[0]))
        .directory(temp.toFile())
        .redirectErrorStream(true)
        .start();

    try {
      String output = readString(process.getInputStream());
      boolean success = process.waitFor() == 0;
      return new MavenResult(success, output);
    } catch (InterruptedException e) {
      throw new RuntimeException("Interrupted while running maven", e);
    }
  }

  protected static String reformattedFile(String pattern) {
    return "Reformatted file: " + pattern.substring(0, pattern.indexOf('/'));
  }

  protected static String noMatchingFiles(String pattern) {
    return String.format("No files matching the pattern were found: \"%s\"", pattern);
  }

  protected static String incorrectlyFormattedFile(String pattern) {
    return "Incorrectly formatted file: " + pattern.substring(0, pattern.indexOf('/'));
  }

  protected static String invalidJavaSyntax() {
    return "Error: Sad sad panda, parsing errors detected";
  }

  protected static String invalidJsSyntax() {
    return "SyntaxError";
  }

  protected static String unknownExtension() {
    return "No parser could be inferred";
  }

  private static Path setupTestDirectory(TestConfiguration testConfiguration) throws IOException {
    Path temp = Files.createTempDirectory("prettier-maven-plugin-test-");
    temp.toFile().deleteOnExit();

    Path source = Paths.get("src/test/resources/test-files");

    Files.walkFileTree(source, new SimpleFileVisitor<Path>() {

      @Override
      public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
          throws IOException {
        Files.createDirectories(temp.resolve(source.relativize(dir)));
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
          throws IOException {
        Files.copy(file, temp.resolve(source.relativize(file)));
        return FileVisitResult.CONTINUE;
      }
    });

    String template = readString(AbstractPrettierMojoTest.class.getResourceAsStream("/pom-template.xml"));

    String rendered = testConfiguration.render(template);
    Files.write(temp.resolve("pom.xml"), rendered.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    return temp;
  }

  private static String readString(InputStream inputStream) throws IOException {
    try (inputStream) {
      return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }
}
