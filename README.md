# ASCII Table

This is a small library to display tables using ascii art.

```scala
AsciiTable()
  .header("first column", "2")
  .row("1", "second value")
  .row("", "")
  .write()
```

will render

```
┌────────────┬────────────┐
│first column│2           │
├────────────┼────────────┤
│1           │second value│
├────────────┼────────────┤
│            │            │
└────────────┴────────────┘
```

## Usage

### Gradle
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  compile 'com.github.ivanfrolovmd:asciitable:v0.0.2'
}
```

### Maven
```
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.github.ivanfrolovmd</groupId>
  <artifactId>asciitable</artifactId>
  <version>v0.0.2</version>
</dependency>
```

### SBT
```
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.ivanfrolovmd" % "asciitable" % "v0.0.2"
```
