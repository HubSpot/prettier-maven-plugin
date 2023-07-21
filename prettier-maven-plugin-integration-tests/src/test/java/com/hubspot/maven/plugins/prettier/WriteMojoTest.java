package com.hubspot.maven.plugins.prettier;

import static org.assertj.core.api.Assertions.assertThat;

import com.hubspot.maven.plugins.prettier.TestConfiguration.Goal;
import java.util.Arrays;
import org.junit.Test;

public class WriteMojoTest extends AbstractPrettierMojoTest {

  @Test
  public void itWritesGoodJava() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
    }
  }

  @Test
  public void itWritesGoodJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_GOOD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
    }
  }

  @Test
  public void itWritesGoodJavaAndGoodJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
    }
  }

  @Test
  public void itWritesBadJava() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
    }
  }

  @Test
  public void itWritesBadJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_BAD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itWritesBadJavaAndBadJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
      assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itWritesInvalidJava() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJavaSyntax());
    }
  }

  @Test
  public void itWritesInvalidJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JS_INVALID_SYNTAX))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJsSyntax());
    }
  }

  @Test
  public void itWritesInvalidJavaAndInvalidJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_INVALID_SYNTAX, JS_INVALID_SYNTAX))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(invalidJavaSyntax());
      assertThat(result.getOutput()).contains(invalidJsSyntax());
    }
  }

  @Test
  public void itWritesUnknownExtensions() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(UNKNOWN_EXTENSION))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(unknownExtension());
    }
  }

  @Test
  public void itWritesEmpty() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(EMPTY))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
    }
  }

  @Test
  public void itWritesGoodJavaAndBadJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_BAD_FORMATTING))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itWritesGoodJavaAndGoodJsAndEmpty() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING, EMPTY))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
      assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
    }
  }

  @Test
  public void itWritesBadJavaAndBadJsAndEmpty() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING, EMPTY))
          .setGoal(Goal.WRITE)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).contains(reformattedFile(JAVA_BAD_FORMATTING));
      assertThat(result.getOutput()).contains(reformattedFile(JS_BAD_FORMATTING));
      assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
    }
  }
}
