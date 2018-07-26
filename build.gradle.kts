plugins {
  idea
  scala
  maven
}

repositories {
  jcenter()
}

dependencies {
  compile("org.scala-lang:scala-library:${properties["scalaVersion"]}")
  testCompile("org.scalatest:scalatest_${properties["scalaMajor"]}:3.0.5")
}
