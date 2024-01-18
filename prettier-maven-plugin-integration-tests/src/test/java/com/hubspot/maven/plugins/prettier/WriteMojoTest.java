package com.hubspot.maven.plugins.prettier;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.maven.plugins.prettier.TestConfiguration.Goal;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class WriteMojoTest extends AbstractPrettierMojoTest {

  @TestFactory
  public Stream<DynamicTest> itWritesGoodJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesGoodJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesGoodJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesGoodJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_GOOD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesGoodJavaAndGoodJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesGoodJavaAndGoodJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesBadJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesBadJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_BAD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesBadJavaAndBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesBadJavaAndBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesInvalidJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesInvalidJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(invalidJavaSyntax());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesInvalidJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesInvalidJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_INVALID_SYNTAX))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(invalidJsSyntax());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesInvalidJavaAndInvalidJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesInvalidJavaAndInvalidJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX, JS_INVALID_SYNTAX))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(invalidJavaSyntax());
          assertThat(result.getOutput()).contains(invalidJsSyntax());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesUnknownExtensions() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesUnknownExtensions_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(UNKNOWN_EXTENSION))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(unknownExtension());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(EMPTY))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesGoodJavaAndBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesGoodJavaAndBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_BAD_FORMATTING))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesGoodJavaAndGoodJsAndEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesGoodJavaAndGoodJsAndEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING, EMPTY))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
          assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itWritesBadJavaAndBadJsAndEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itWritesBadJavaAndBadJsAndEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING, EMPTY))
              .setGoal(Goal.WRITE)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }
}
