package com.hubspot.maven.plugins.prettier;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.maven.it.VerificationException;
import org.apache.maven.it.Verifier;

import com.google.common.collect.ImmutableSet;
import com.google.common.io.Resources;

public abstract class AbstractPrettierMojoTest {
  protected static final String JAVA_GOOD_FORMATTING = "java-good-formatting/*.java";
  protected static final String JAVA_BAD_FORMATTING = "java-bad-formatting/*.java";
  protected static final String JAVA_INVALID_SYNTAX = "java-invalid-syntax/*.java";
  protected static final String JS_GOOD_FORMATTING = "js-good-formatting/*.js";
  protected static final String JS_BAD_FORMATTING = "js-bad-formatting/*.js";
  protected static final String JS_INVALID_SYNTAX = "js-invalid-syntax/*.js";
  protected static final String UNKNOWN_EXTENSION = "unknown-extension/*.unknown";
  protected static final String EMPTY = "empty/*.java";
  protected static final String BUILD_SUCCESS = "BUILD SUCCESS";
  protected static final String BUILD_FAILURE = "BUILD FAILURE";
  private static final Set<String> PRETTIER_JAVA_VERSIONS_TO_TEST = findPrettierJavaVersionsToTest();

  protected static Set<String> getPrettierJavaVersionsToTest() {
    return PRETTIER_JAVA_VERSIONS_TO_TEST;
  }

  protected static MavenResult runMaven(TestConfiguration testConfiguration)
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

  protected static boolean isNewishVersion(String prettierJavaVersion) {
    return prettierJavaVersion.startsWith("1.");
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

    String template = Resources.toString(
        Resources.getResource("pom-template.xml"),
        StandardCharsets.UTF_8
    );

    String rendered = testConfiguration.render(template);
    Files.write(temp.resolve("pom.xml"), rendered.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);

    return temp;
  }

  private static Set<String> findPrettierJavaVersionsToTest() {
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
}
