plugins {
    id("org.springframework.boot") version "3.3.2"
    id("io.spring.dependency-management") version "1.1.5"
    id("java")
}

group = "com.amazobank.crm"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-resource-server")
    implementation("org.springframework.security:spring-security-oauth2-authorization-server")
    implementation("org.springframework.security:spring-security-oauth2-client")
    implementation("org.springframework.security:spring-security-oauth2-jose")

    implementation(platform("software.amazon.awssdk:bom:2.27.21"))
    //
    implementation("software.amazon.awssdk:cognitoidentityprovider")

    implementation ("com.amazonaws.serverless:aws-serverless-java-container-springboot3:2.1.5")
    implementation ("com.amazonaws:aws-lambda-java-core:1.2.2")
    implementation ("com.amazonaws:aws-lambda-java-events:3.11.1")
    runtimeOnly ("com.amazonaws:aws-lambda-java-log4j2:1.5.1")

    implementation("software.amazon.awssdk:rds:2.38.2")
    implementation("software.amazon.jdbc:aws-advanced-jdbc-wrapper:2.6.6")

    runtimeOnly("com.mysql:mysql-connector-j")
    testRuntimeOnly("com.h2database:h2")

    implementation ("io.jsonwebtoken:jjwt-api:0.11.5")
    runtimeOnly ("io.jsonwebtoken:jjwt-impl:0.11.5")
    runtimeOnly ("io.jsonwebtoken:jjwt-jackson:0.11.5")

    compileOnly ("org.projectlombok:lombok")
    annotationProcessor ("org.projectlombok:lombok")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

configurations {
    dependencyLocking {
        lockAllConfigurations()
    }
}

tasks.register<Zip>("buildZip") {
    into("lib") {
        from(tasks.jar)
        from(configurations.runtimeClasspath)
    }
}

tasks.bootTestRun {
    systemProperty("spring.profiles.active", "dev")
}

tasks.test {
    useJUnitPlatform()
    systemProperty("spring.profiles.active", "dev")
}
