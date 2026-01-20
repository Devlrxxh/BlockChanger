plugins {
  `java-library`
  id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
  id("maven-publish")
}

group = "dev.lrxh"
version = "2.0-SNAPSHOT"
description = "BlockChanger library built with Paperweight"

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

dependencies {
  paperweight.paperDevBundle("1.21.9-R0.1-SNAPSHOT")
  implementation("net.bytebuddy:byte-buddy:1.14.3")
  implementation("net.bytebuddy:byte-buddy-agent:1.17.8")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

tasks {
  compileJava {
    options.release = 21
  }
  javadoc {
    options.encoding = Charsets.UTF_8.name()
  }
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      groupId = "dev.lrxh"
      artifactId = "BlockChanger"
      version = "2.0-SNAPSHOT"

      from(components["java"])
    }
  }
  repositories {
    mavenLocal()
  }
}
