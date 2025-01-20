import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.libsDirectory

plugins {
	kotlin("jvm") version "1.9.25"
	kotlin("plugin.spring") version "1.9.25"
	id("org.springframework.boot") version "3.4.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(17)
	}
}

repositories {
	mavenCentral()
	flatDir {
		dirs("libs")
	}
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
	implementation("org.jetbrains.kotlin:kotlin-reflect")
	developmentOnly("org.springframework.boot:spring-boot-devtools")
	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
	implementation("org.apache.poi:poi:5.3.0")
	implementation("org.apache.poi:poi-ooxml:5.3.0")
	// Add these dependencies to your build.gradle file (if using Gradle)
	implementation("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:1.9.25")
	implementation("org.jetbrains.kotlin:kotlin-scripting-common:1.9.25")
	implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:1.9.25")

	implementation("net.java.dev.jna:jna:5.12.1")
	implementation("net.java.dev.jna:jna-platform:5.12.1")

	implementation(files("C:/libs/jacob-1.21/jacob.jar"))
	implementation(files("C:/libs/jacob-1.21/jacob-1.21-x64.dll"))

	implementation("org.springframework.boot:spring-boot-starter-data-jpa")

	implementation("mysql:mysql-connector-java:8.0.28")
}

kotlin {
	compilerOptions {
		freeCompilerArgs.addAll("-Xjsr305=strict")
	}
}

tasks.withType<Test> {
	useJUnitPlatform()
}
