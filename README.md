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
          <version>0.1</version>
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
          <version>0.1</version>
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

You can also ran in a one-off fashion via the commandline:  
`mvn com.hubspot.maven.plugins:prettier-maven-plugin:0.1:check`  
or  
`mvn com.hubspot.maven.plugins:prettier-maven-plugin:0.1:write`
