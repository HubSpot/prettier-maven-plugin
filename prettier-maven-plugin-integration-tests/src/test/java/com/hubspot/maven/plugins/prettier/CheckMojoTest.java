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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
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
  private static final String EMPTY = "empty/*.java";
  private static final String BUILD_SUCCESS = "BUILD SUCCESS";
  private static final String BUILD_FAILURE = "BUILD FAILURE";
  private static final Set<String> PRETTIER_JAVA_VERSIONS_TO_TEST = findAllPrettierJavaVersions();

  @Test
  public void itChecksGoodJava() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
    }
  }

  @Test
  public void itChecksGoodJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_GOOD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
    }
  }

  @Test
  public void itChecksGoodJavaAndGoodJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
    }
  }

  @Test
  public void itChecksBadJava() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
    }
  }

  @Test
  public void itChecksBadJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_BAD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itChecksBadJavaAndBadJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itChecksInvalidJava() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJavaSyntax());
    }
  }

  @Test
  public void itChecksInvalidJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_INVALID_SYNTAX))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJsSyntax());
    }
  }

  @Test
  public void itChecksInvalidJavaAndInvalidJs() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX, JS_INVALID_SYNTAX))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJavaSyntax());
      assertThat(result.getOutput()).contains(invalidJsSyntax());
    }
  }

  @Test
  public void itChecksUnknownExtensions() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(UNKNOWN_EXTENSION))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(unknownExtension());
    }
  }

  @Test
  public void itChecksEmpty() throws Exception {
    for (String prettierJavaVersion : PRETTIER_JAVA_VERSIONS_TO_TEST) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(EMPTY))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).satisfiesAnyOf(
          output -> assertThat(output).contains(noMatchingFiles()),
          output -> assertThat(output).contains(noFilesMatching())
      );
    }
  }

  private static Set<String> findAllPrettierJavaVersions() {
    Path baseDirectory = Paths.get("../prettier-maven-plugin/src/main/binaries/prettier-java");

    // keep the build time down by not testing every older version
    Set<String> ignoredPrettierJavaVersions = ImmutableSet.of(
        "0.6.0",
        "0.7.1",
        "0.8.0",
        "0.8.2",
        "0.8.3",
        "1.0.1",
        "1.1.0",
        "c08da7b2b0486f59980a01cb99c6f0756725450a",
        "6cf5cfdf76550ab4418a6e900696ba35eaa0fbc8",
        "a24aa13714b850ab3d0f4e3f07414137c33321a1"
    );

    try (Stream<Path> files = Files.walk(baseDirectory)) {
      return files
          .filter(Files::isRegularFile)
          .map(Path::getFileName)
          .map(Path::toString)
          .map(
              prettierJavaZip ->
                  prettierJavaZip.substring(
                      "prettier-java-".length(),
                      prettierJavaZip.length() - ".zip".length()
                  )
          )
          .filter(prettierJavaVersion -> !ignoredPrettierJavaVersions.contains(prettierJavaVersion))
          .collect(Collectors.toSet());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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

  private static String invalidJavaSyntax() {
    return "Error: Sad sad panda, parsing errors detected";
  }

  private static String invalidJsSyntax() {
    return "SyntaxError";
  }

  private static String unknownExtension() {
    return "No parser could be inferred";
  }

  private static String noMatchingFiles() {
    return "No matching files";
  }

  private static String noFilesMatching() {
    return "No files matching";
  }
}
