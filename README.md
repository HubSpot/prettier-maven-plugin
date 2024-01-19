# prettier-maven-plugin

Maven plugin for running [prettier-java](https://github.com/jhipster/prettier-java) during a build. Node, prettier, and prettier-java are downloaded automatically as needed.

There is a `check` goal which (optionally) fails the build if code isn't formatted correctly, and a `write` goal which rewrites the source code in place. A common setup might be to use the `write` goal during local builds, and the `check` goal during CI builds.

### Example Usage

This example will run the `check` goal inside of Travis CI, and the `write` goal outside of Travis CI. You can update the profile activation conditions based on the CI tool you use.

```xml
<properties>
  <!-- By default just re-write code with prettier -->
  <plugin.prettier.goal>write</plugin.prettier.goal>
</properties>

<build>
  <plugins>
    <plugin>
      <groupId>com.hubspot.maven.plugins</groupId>
      <artifactId>prettier-maven-plugin</artifactId>
      <version>0.16</version>
      <configuration>
        <prettierJavaVersion>2.0.0</prettierJavaVersion>
        <printWidth>90</printWidth>
        <tabWidth>2</tabWidth>
        <useTabs>false</useTabs>
        <ignoreConfigFile>true</ignoreConfigFile>
        <ignoreEditorConfig>true</ignoreEditorConfig>
        <!-- Use <inputGlobs> to override the default input patterns -->
        <inputGlobs>
          <!-- These are the default patterns, you can omit <inputGlobs> entirely unless you want to override them -->
          <inputGlob>src/main/java/**/*.java</inputGlob>
          <inputGlob>src/test/java/**/*.java</inputGlob>
        </inputGlobs>
      </configuration>
      <executions>
        <execution>
          <phase>validate</phase>
          <goals>
            <goal>${plugin.prettier.goal}</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>

<profiles>
  <profile>
    <id>travis</id>
    <activation>
      <property>
        <name>env.TRAVIS</name>
      </property>
    </activation>
    <properties>
      <!-- But in our CI environment we want to validate that code is formatted -->
      <plugin.prettier.goal>check</plugin.prettier.goal>
    </properties>
  </profile>
</profiles>
```

You can also run in a one-off fashion via the commandline:  
`mvn prettier:check`  
or  
`mvn prettier:write`

You can also run `mvn prettier:print-args` in order to confirm the configuration values

To format additional directories or file types via the commandline, you can pass a comma-separated list of patterns, for example:  
`mvn prettier:write '-Dprettier.inputGlobs=src/main/java/**/*.java,src/test/java/**/*.java,src/main/js/**/*.js'`

### Configuration

If you want to customize the behavior of prettier, you can use a normal prettier configuration [file](https://prettier.io/docs/en/configuration.html). Alternatively, you can configure prettier directly via the Maven plugin using the following options:

| Name                | -D property name             | Default Value                    | Description                                                                                                                                                                                                                                                       |
|---------------------|------------------------------|----------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| skip                | N/A                          | false                            | If set to true, plugin execution will be skipped                                                                                                                                                                                                                  |
| fail                | N/A                          | true                             | **Only appplies to `check` goal**. If set to true, the plugin execution will fail if any unformatted files are found                                                                                                                                              |
| generateDiff        | N/A                          | false                            | **Only appplies to `check` goal. Be sure to have to sh and diff in your PATH**. If set to true, a diff will be generated between the current code and the prettier-formatted code.                                                                                |
| diffGenerator       | prettier.diffGenerator       | _                                | **Only appplies to `check` goal**. Can be used to supply a custom implementation of [`DiffGenerator`](https://github.com/HubSpot/prettier-maven-plugin/blob/master/prettier-maven-plugin/src/main/java/com/hubspot/maven/plugins/prettier/diff/DiffGenerator.java) 
| nodeVersion         | prettier.nodeVersion         | 16.13.1                          | Controls version of Node used to run prettier-java.                                                                                                                                                                                                               |
| nodePath            | prettier.nodePath            | -                                | Can be used to supply your own node executable, rather than having the plugin download it. To use the version of node on your `$PATH`, you can simply set this option to `node`.                  |
| npmPath             | prettier.npmPath             | -                                | Can be used to supply your own npm executable, rather than having the plugin download it. To use the version of npm on your `$PATH`, you can simply set this option to `npm`.                      |
| prettierJavaVersion | prettier.prettierJavaVersion | 0.7.0                            | Controls version of prettier-java that is used.                                                             |
| printWidth          | prettier.printWidth          | `null`                           | If set, will be passed to prettier as `--print-width`. More information [here](https://prettier.io/docs/en/options.html#print-width)                                                                                                                              |
| tabWidth            | prettier.tabWidth            | `null`                           | If set, will be passed to prettier as `--tab-width`. More information [here](https://prettier.io/docs/en/options.html#tab-width)                                                                                                                                  |
| useTabs             | prettier.useTabs             | `null`                           | If set, will be passed to prettier as `--use-tabs`. More information [here](https://prettier.io/docs/en/options.html#tabs)                                                                                                                                        |
| endOfLine           | prettier.endOfLine           | `null`                           | If set, will be passed to prettier as `--end-of-line`. More information [here](https://prettier.io/docs/en/options.html#end-of-line)                                                                                                                              |
| ignoreConfigFile    | prettier.ignoreConfigFile    | `false`                          | If set to true, pretter will be invoked with `--no-config`. More information [here](https://prettier.io/docs/en/cli.html#--no-config)                                                                                                                             |
| ignoreEditorConfig  | prettier.ignoreEditorConfig  | `false`                          | If set to true, pretter will be invoked with `--no-editorconfig`. More information [here](https://prettier.io/docs/en/cli.html#--no-editorconfig)                                                                                                                 |
| inputGlobs          | prettier.inputGlobs          | `src/{main,test}/java/**/*.java` | Controls the input paths passed to prettier, useful for formatting additional directories or file types. More information [here](https://prettier.io/docs/en/cli.html#file-patterns)                                                                              |
| disableGenericsLinebreaks | prettier.disableGenericsLinebreaks | `false` | Prevents prettier from adding linebreaks to generic type declarations (see https://github.com/HubSpot/prettier-maven-plugin/pull/78 for more background) |

### Generic Linebreaks

The `disableGenericsLinebreaks` option is implemented by patching prettier-plugin-java after downloading. As new versions of prettier-java are released, we may need to create updated patches. The basic flow for creating a new patch is:
1. Download the new version of prettier-java
  - `cd /tmp && npm install prettier-plugin-java@{version}`
2. Make two copies of prettier-java code
  - `mkdir -p a/node_modules && cp -r node_modules/prettier-plugin-java a/node_modules`)
  - `mkdir -p b/node_modules && cp -r node_modules/prettier-plugin-java b/node_modules`)
3. Update the code within `b/node_modules/prettier-plugin-java/dist/` as needed to revert the generic linebreak behavior (for now, these are the relevant PRs: [#512](https://github.com/jhipster/prettier-java/pull/512), [#584](https://github.com/jhipster/prettier-java/pull/584))
4. Generate a diff between `a/` (original code) and `b/` (updated code)
  - `git diff -p a b --no-prefix > no-linebreak-generics-{version}.patch`
5. Add the patch to `src/main/resources/` and update the logic in `PrettierArgs.java` to use it as appropriate

### Note

For convenience, this plugin downloads Node, prettier, and prettier-java as needed. Node is downloaded from https://nodejs.org/dist/ and prettier-plugin-java is downloaded via npm
