package com.hubspot.maven.plugins.prettier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

import com.google.common.io.Resources;
import com.hubspot.maven.plugins.prettier.TestConfiguration.Goal;

public class CheckMojoTest {
  private static final String JAVA_GOOD_FORMATTING = "java-good-formatting/*.java";
  private static final String JAVA_BAD_FORMATTING = "java-bad-formatting/*.java";
  private static final String JAVA_INVALID_SYNTAX = "java-invalid-syntax/*.java";
  private static final String JS_GOOD_FORMATTING = "js-good-formatting/*.js";
  private static final String JS_BAD_FORMATTING = "js-bad-formatting/*.js";
  private static final String JS_INVALID_SYNTAX = "js-invalid-syntax/*.js";
  private static final String UNKNOWN_EXTENSION = "unknown-extension/*.unknown";
  private static final String BUILD_SUCCESS = "BUILD SUCCESS";
  private static final String BUILD_FAILURE = "BUILD FAILURE";

  @Test
  public void itChecksGoodJava() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getOutput()).contains("BUILD SUCCESS");
    assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
  }

  @Test
  public void itChecksGoodJs() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JS_GOOD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getOutput()).contains("BUILD SUCCESS");
    assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
  }

  @Test
  public void itChecksGoodJavaAndGoodJs() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isTrue();
    assertThat(result.getOutput()).contains("BUILD SUCCESS");
    assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
    assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
  }

  @Test
  public void itChecksBadJava() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getOutput()).contains("BUILD FAILURE");
    assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
  }

  @Test
  public void itChecksBadJs() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JS_BAD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getOutput()).contains("BUILD FAILURE");
    assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
  }

  @Test
  public void itChecksBadJavaAndBadJs() throws Exception {
    TestConfiguration testConfiguration = TestConfiguration
        .newBuilder()
        .setPrettierJavaVersion("1.5.0")
        .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING))
        .setGoal(Goal.CHECK)
        .build();

    MavenResult result = runMaven(testConfiguration);

    assertThat(result.getSuccess()).isFalse();
    assertThat(result.getOutput()).contains("BUILD FAILURE");
    assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
    assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
  }

  private static MavenResult runMaven(TestConfiguration testConfiguration)
      throws IOException, VerificationException {
    Path temp = setupTestDirectory(testConfiguration);

    Verifier verifier = new Verifier(temp.toAbsolutePath().toString());
    verifier.setAutoclean(false);
    Throwable t = catchThrowable(() -> verifier.executeGoal("validate"));
    verifier.resetStreams();

    return new MavenResult(
        t == null,
        verifier
            .loadFile(verifier.getBasedir(), verifier.getLogFileName(), false)
            .stream()
            .map(Verifier::stripAnsi)
            .collect(Collectors.joining("\n"))
    );
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

    String template = Resources.toString(
        Resources.getResource("pom-template.xml"),
        StandardCharsets.UTF_8
    );

    String rendered = testConfiguration.render(template);
    Files.write(temp.resolve("pom.xml"), rendered.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    return temp;
  }

  private static String noMatchingFiles(String pattern) {
    return String.format("No files matching the pattern were found: \"%s\"", pattern);
  }

  private static String incorrectlyFormattedFile(String pattern) {
    return "Incorrectly formatted file: " + pattern.substring(0, pattern.indexOf('/'));
  }
}
