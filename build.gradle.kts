plugins {
  idea
  scala
  maven
  id("com.github.maiflai.scalatest") version "0.22"
}

repositories {
  jcenter()
}

dependencies {
  compile("org.scala-lang:scala-library:${properties["scalaVersion"]}")
  testCompile("org.scalatest:scalatest_${properties["scalaMajor"]}:3.0.5")
  testRuntime("org.pegdown:pegdown:1.4.2")
}
