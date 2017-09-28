# jaxrs-path-analyzer
A maven plugin built to break the build when a double mapping declaration is found.

# How to use

As the plugin is not hosted, you'll need to download the project and install it locally, you can do it by running:

`mvn install`

After that, in your project's pom.xml you have to declare using this plugin, simply copy and paste the snippet below in the plugins inside the build section.

```xml
<plugin>
  <groupId>com.contaazul</groupId>
  <artifactId>jaxrs-path-analyzer</artifactId>
  <version>0.0.1-SNAPSHOT</version>
  <executions>
    <execution>
      <id>path-analyzer</id>
      <phase>install</phase>
      <goals>
        <goal>path-analyzer</goal>
      </goals>
    </execution>
  </executions>
</plugin>
```
