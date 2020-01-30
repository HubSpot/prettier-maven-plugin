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
          <version>0.6</version>
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
          <version>0.6</version>
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

### Adding new versions of node

You can download binaries for Linux/OSX/Windows from here:
https://nodejs.org/en/about/releases/

1. Make a new folder located at `src/main/binaries/node/{node-version}` and drop the binaries in there using the existing name formatting.
2. Update the pom.xml to attach these new binaries

### Adding new versions of prettier-java

1. Run `./create-prettier-java-zip.sh {prettier-java-version}` which will spit out the location of a zip file
2. Make a new folder located at `src/main/binaries/prettier-java/{prettier-java-version}` and copy the zip file under there
3. Update the pom.xml to attach this zip file
