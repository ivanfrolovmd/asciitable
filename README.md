[![JitPack](https://jitpack.io/v/ivanfrolovmd/asciitable.svg "Jit Pack Build Status")](https://jitpack.io/#ivanfrolovmd/asciitable)

# ASCII Table

This is a small library to render tables using ascii art. The column widths are calculated automatically by sampling the
first 50 rows (configurable) to fit the table in desired screen width.

### Multi-line mode

When used in multi-line mode, cells overflowing the column width are rendered using several lines, for example:

```scala
AsciiTable()
  .width(40) // screen width to fit table in
  .multiline(true) // render rows across multiple lines
  .columnMinWidth(7) // column width will be 7 characters or more
  .rowMaxHeight(3) // clip cells at 3 lines
  .header("N", "column", "column with long values")
  .row("1", "value 1", "Lorem ipsum dolor sit amet, consectetur")
  .row("2", "value 2", "Lorem ipsum dolor sit amet, consectetur" * 3)
  .row("3", "foo", "bar")
  .write()
```

produces:

```text
<------------ 40 characters ----------->
╔═╤═══════╤════════════════════════════╗
║N│column │column with long values     ║
╠═╪═══════╪════════════════════════════╣
║1│value 1│Lorem ipsum dolor sit amet, ║
║ │       │consectetur                 ║
╟─┼───────┼────────────────────────────╢ ↑
║2│value 2│Lorem ipsum dolor sit amet, ║ |
║ │       │consecteturLorem ipsum dolor║ 3 lines max
║ │       │ sit amet, consecteturLorem…║ |
╟─┼───────┼────────────────────────────╢ ↓
║3│foo    │bar                         ║
╚═╧═══════╧════════════════════════════╝
   <- 7 -> 
```

### One-line mode

In one-line mode cells are clipped at column width:

```scala
AsciiTable()
  .width(40) // screen width to fit table in
  .multiline(false) // one line per row
  .columnMinWidth(7) // column width is be 7 characters or more
  .header("N", "column", "column with long values")
  .row("1", "value 1", "Lorem ipsum dolor sit amet, consectetur")
  .row("2", "value 2", "Lorem ipsum dolor sit amet, consectetur" * 3)
  .row("3", "foo", "bar")
```

```text
<------------ 40 characters ----------->
╔═╤═══════╤════════════════════════════╗
║N│column │column with long values     ║
╠═╪═══════╪════════════════════════════╣
║1│value 1│Lorem ipsum dolor sit amet,…║
║2│value 2│Lorem ipsum dolor sit amet,…║
║3│foo    │bar                         ║
╚═╧═══════╧════════════════════════════╝
```

### ASCII-only mode

Previous sections used [box drawing characters](https://en.wikipedia.org/wiki/Box-drawing_character), which contain 
unicode symbols. If ASCII-only set is required, it is possible to switch to legacy mode:

```scala
AsciiTable()
  .useAscii(true) // use ASCII-only characters
  .width(40)
  .multiline(true)
  .columnMinWidth(7)
  .rowMaxHeight(3)
  .header("N", "column", "column with long values")
  .row("1", "value 1", "Lorem ipsum dolor sit amet, consectetur")
  .row("2", "value 2", "Lorem ipsum dolor sit amet, consectetur" * 3)
  .row("3", "foo", "bar")
  .write()
```

```text
+-+-------+----------------------------+
|N|column |column with long values     |
+-+-------+----------------------------+
|1|value 1|Lorem ipsum dolor sit amet, |
| |       |consectetur                 |
+-+-------+----------------------------+
|2|value 2|Lorem ipsum dolor sit amet, |
| |       |consecteturLorem ipsum dolor|
| |       | sit amet, consecteturLorem_|
+-+-------+----------------------------+
|3|foo    |bar                         |
+-+-------+----------------------------+
```

## Installation

### Gradle
```
allprojects {
  repositories {
    ...
    maven { url 'https://jitpack.io' }
  }
}

dependencies {
  compile 'com.github.ivanfrolovmd:asciitable:0.0.7'
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
  <version>0.0.7</version>
</dependency>
```

### SBT
```
resolvers += "jitpack" at "https://jitpack.io"
libraryDependencies += "com.github.ivanfrolovmd" % "asciitable" % "0.0.7"
```
