package com.hubspot.maven.plugins.prettier.internal;

import java.nio.file.Path;
import java.nio.file.Paths;

public class PrettierPaths {
  private static final Path OLD_PRETTIER_BIN_PATH = Paths.get("node_modules/prettier/bin-prettier.js");
  private static final Path NEW_PRETTIER_BIN_PATH = Paths.get("node_modules/prettier/bin/prettier.cjs");
  private static final Path OLD_PRETTIER_JAVA_PLUGIN_PATH = Paths.get("node_modules/prettier-plugin-java");
  private static final Path NEW_PRETTIER_JAVA_PLUGIN_PATH = Paths.get("node_modules/prettier-plugin-java/dist/index.js");

  public static Path prettierBinPath(String prettierVersion) {
    if ("2.2.0".compareTo(prettierVersion) > 0) {
      return OLD_PRETTIER_BIN_PATH;
    } else {
      return NEW_PRETTIER_BIN_PATH;
    }
  }

  public static Path prettierJavaPluginPath(String prettierVersion) {
    if ("2.2.0".compareTo(prettierVersion) > 0) {
      return OLD_PRETTIER_JAVA_PLUGIN_PATH;
    } else {
      return NEW_PRETTIER_JAVA_PLUGIN_PATH;
    }
  }
}
