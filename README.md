# Maven Executable JAR Plugin

This Maven plugin makes a JAR into an executable JAR with ALL its dependencies contained within. The plugin diggers from Maven Assembly Plugin and Maven Shade Plugin, who both can deliver the same, but this plugin comes with a significant advantage - dependencies are NOT unpacked, but rather stores as JAR files inside the artifact JAR>

Usage:
```xml
<plugin>
 <groupId>org.brylex.maven</groupId>
 <artifactId>exec-jar</artifactId>
 <version>develop-SNAPSHOT</version>
 <configuration>
  <mainClass>org.brylex.maven.execjar.MyMain</mainClass>
 </configuration>
 <executions>
  <execution>
   <goals>
    <goal>exec-jar</goal>
   </goals>
  </execution>
 </executions>
</plugin>
```
 
