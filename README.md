# prettier-maven-plugin

Maven plugin for running [prettier-java](https://github.com/jhipster/prettier-java) during a build. Node, prettier, and prettier-java are bundled into the plugin.

There is a `check` goal which (optionally) fails the build if code isn't formatted correctly, and a `write` goal which rewrites the source code in place. A common setup might be to use the `write` goal during local builds, and the `check` goal during CI builds.

### Example Usage

This example will run the `check` goal inside of Travis CI, and the `write` goal outside of Travis CI. You can update the profile activation conditions based on the CI tool you use.

```xml
<profiles>
  <profile>
    <id>travis</id>
    <activation>
      <property>
        <name>env.TRAVIS</name>
      </property>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>com.hubspot.maven.plugins</groupId>
          <artifactId>prettier-maven-plugin</artifactId>
          <version>0.7</version>
          <executions>
            <execution>
              <phase>validate</phase>
              <goals>
                <goal>check</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
  <profile>
    <id>local</id>
    <activation>
      <property>
        <name>!env.TRAVIS</name>
      </property>
    </activation>
    <build>
      <plugins>
        <plugin>
          <groupId>com.hubspot.maven.plugins</groupId>
          <artifactId>prettier-maven-plugin</artifactId>
          <version>0.7</version>
          <executions>
            <execution>
              <phase>validate</phase>
              <goals>
                <goal>write</goal>
              </goals>
            </execution>
          </executions>
        </plugin>
      </plugins>
    </build>
  </profile>
</profiles>
```

You can also run in a one-off fashion via the commandline:  
`mvn prettier:check`  
or  
`mvn prettier:write`

### Configuration

The plugin supports the following configuration options:

| Name | -D property name | Default Value | Description |
| ---- | ---------------- | ------------- | ----------- |
| skip | N/A | false | If set to true, plugin execution will be skipped |
| fail | N/A | true | **Only appplies to `check` goal**. If set to true, the plugin execution will fail if any unformatted files are found |
| generateDiff | N/A | false | **Only appplies to `check` goal. Default diff implementation does not work on Windows**. If set to true, a diff will be generated between the current code and the prettier-formatted code. |
| diffGenerator | prettier.diffGenerator | _ | **Only appplies to `check` goal**. Can be used to supply a custom implementation of [`DiffGenerator`](https://github.com/HubSpot/prettier-maven-plugin/blob/master/src/main/java/com/hubspot/maven/plugins/prettier/diff/DiffGenerator.java)
| nodeVersion | prettier.nodeVersion | 12.13.0 | Controls version of Node used to run prettier-java. Valid values can be found [here](https://github.com/HubSpot/prettier-maven-plugin/tree/master/src/main/binaries/node) |
| prettierJavaVersion | prettier.prettierJavaVersion | 0.7.0 | Controls version of prettier-java that is used. Valid values can be found [here](https://github.com/HubSpot/prettier-maven-plugin/tree/master/src/main/binaries/prettier-java) |
| printWidth | prettier.printWidth | `null` | If set, will be passed to prettier as `--print-width`. More information [here](https://prettier.io/docs/en/options.html#print-width) |
| tabWidth | prettier.tabWidth | `null` | If set, will be passed to prettier as `--tab-width`. More information [here](https://prettier.io/docs/en/options.html#tab-width) |
| useTabs | prettier.useTabs | `null` | If set, will be passed to prettier as `--use-tabs`. More information [here](https://prettier.io/docs/en/options.html#tabs) |
| ignoreConfigFile | prettier.ignoreConfigFile | `false` | If set to true, pretter will be invoked with `--no-config`. More information [here](https://prettier.io/docs/en/cli.html#--no-config) |
| ignoreEditorConfig | prettier.ignoreEditorConfig | `false` | If set to true, pretter will be invoked with `--no-editorconfig`. More information [here](https://prettier.io/docs/en/cli.html#--no-editorconfig) |

### Developing the plugin

For convenience, this plugin bundles Node, prettier, and prettier-java. Over time, these bundled dependencies will need to be kept up to date. Below are some directions for adding new versions of Node and prettier-java.

#### Adding new versions of node

You can download binaries for Linux/OSX/Windows from here:
https://nodejs.org/en/about/releases/

1. Make a new folder located at `src/main/binaries/node/{node-version}` and drop the binaries in there using the existing name formatting.
2. Update the pom.xml to attach these new binaries

#### Adding new versions of prettier-java

1. Run `./create-prettier-java-zip.sh {prettier-java-version}` which will spit out the location of a zip file
2. Make a new folder located at `src/main/binaries/prettier-java/{prettier-java-version}` and copy the zip file under there
3. Update the pom.xml to attach this zip file
