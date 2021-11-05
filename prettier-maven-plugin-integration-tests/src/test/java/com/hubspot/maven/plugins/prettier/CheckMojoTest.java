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

public class CheckMojoTest extends AbstractPrettierMojoTest {

  @Test
  public void itChecksGoodJava() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
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
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(EMPTY))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      if (isNewishVersion(prettierJavaVersion)) {
        assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
      }
    }
  }

  @Test
  public void itChecksGoodJavaAndBadJs() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_BAD_FORMATTING))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
    }
  }

  @Test
  public void itChecksGoodJavaAndGoodJsAndEmpty() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_GOOD_FORMATTING, JS_GOOD_FORMATTING, EMPTY))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isTrue();
      assertThat(result.getOutput()).contains(BUILD_SUCCESS);
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JAVA_GOOD_FORMATTING));
      assertThat(result.getOutput()).doesNotContain(noMatchingFiles(JS_GOOD_FORMATTING));
      if (isNewishVersion(prettierJavaVersion)) {
        assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
      }
    }
  }

  @Test
  public void itChecksBadJavaAndBadJsAndEmpty() throws Exception {
    for (String prettierJavaVersion : getPrettierJavaVersionsToTest()) {
      TestConfiguration testConfiguration = TestConfiguration
          .newBuilder()
          .setPrettierJavaVersion(prettierJavaVersion)
          .setInputGlobs(Arrays.asList(JAVA_BAD_FORMATTING, JS_BAD_FORMATTING, EMPTY))
          .setGoal(Goal.CHECK)
          .build();

      MavenResult result = runMaven(testConfiguration);

      assertThat(result.getSuccess()).isFalse();
      assertThat(result.getOutput()).contains(BUILD_FAILURE);
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JAVA_BAD_FORMATTING));
      assertThat(result.getOutput()).contains(incorrectlyFormattedFile(JS_BAD_FORMATTING));
      if (isNewishVersion(prettierJavaVersion)) {
        assertThat(result.getOutput()).contains(noMatchingFiles(EMPTY));
      }
    }
  }
}
