package com.hubspot.maven.plugins.prettier;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.maven.plugins.prettier.TestConfiguration.Goal;
import java.util.Arrays;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

public class CheckMojoTest extends AbstractPrettierMojoTest {

  @TestFactory
  public Stream<DynamicTest> itChecksGoodJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksGoodJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksGoodJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksGoodJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_GOOD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksGoodJavaAndGoodJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksGoodJavaAndGoodJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING))
              .setGoal(Goal.CHECK)
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
  public Stream<DynamicTest> itChecksBadJavaWithErrorInPath() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksBadJavaWithErrorInPath_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING_ERROR_PATH))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING_ERROR_PATH));
          assertThat(result.getSuccess()).isFalse();
        }));
  }

  @TestFactory
  public Stream<DynamicTest> itChecksBadJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksBadJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_BAD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksBadJavaAndBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksBadJavaAndBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksInvalidJava() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksInvalidJava_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(invalidJavaSyntax());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksInvalidJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksInvalidJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JS_INVALID_SYNTAX))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(invalidJsSyntax());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksInvalidJavaAndInvalidJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksInvalidJavaAndInvalidJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX, JS_INVALID_SYNTAX))
              .setGoal(Goal.CHECK)
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
  public Stream<DynamicTest> itChecksUnknownExtensions() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksUnknownExtensions_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(UNKNOWN_EXTENSION))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(unknownExtension());
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(EMPTY))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_SUCCESS);
          assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
          assertThat(result.getSuccess()).isTrue();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksGoodJavaAndBadJs() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksGoodJavaAndBadJs_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_BAD_FORMATTING))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }

  @TestFactory
  public Stream<DynamicTest> itChecksGoodJavaAndGoodJsAndEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksGoodJavaAndGoodJsAndEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING, EMPTY))
              .setGoal(Goal.CHECK)
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
  public Stream<DynamicTest> itChecksBadJavaAndBadJsAndEmpty() {
    return getPrettierJavaVersionsToTest().stream().map(prettierJavaVersion ->
        DynamicTest.dynamicTest("itChecksBadJavaAndBadJsAndEmpty_prettier-java@" + prettierJavaVersion, () -> {
          TestConfiguration testConfiguration = TestConfiguration
              .newBuilder()
              .setPrettierJavaVersion(prettierJavaVersion)
              .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING, EMPTY))
              .setGoal(Goal.CHECK)
              .build();

          MavenResult result = runMaven(testConfiguration);

          assertThat(result.getOutput()).contains(BUILD_FAILURE);
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
          assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
          assertThat(result.getSuccess()).isFalse();
        })
    );
  }
}
