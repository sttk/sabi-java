<div align="center">
  <a href="https://github.com/sttk/sabi-java">
    <img src="./images/sabi-java.png" width="250" height="auto" alt="Sabi"/>
  </a>

  <h2>
  "sabi" - A small framework to separate logic and data access
  </h2>
  <br>

  [![Maven Central][mvn-img]][mvn-url] [![MVN Repository][mvnrepo-img]][mvnrepo-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]
</div>

## Overview

**sabi** was developed with the goal of thoroughly separating business logic from data access. However, it differs from conventional Dependency Injection (DI) frameworks that merely invert dependencies by placing an interface between the two layers—**sabi** draws a clear line beyond that simple approach.

What elevates **sabi** to an advanced framework in particular are the following two key techniques: introducing a data-access interface optimized for each individual piece of logic, and routing input/output from the controller layer directly to the data access layer via `DataSrc`, completely bypassing the logic layer.

### Introducing Data-Access Interfaces Optimized Per Logic Unit

The former approach thoroughly embodies the Interface Segregation Principle (ISP)—one of the SOLID principles that has often ended up more nominal than real in practice. Each piece of logic (use case) defines, on the logic side, its own dedicated interface that specifies only the operations it truly needs. Meanwhile, the data access side implements interfaces based on its responsibility as a data provider. The `DataHub` then mediates and maps between the two, so that the logic side never needs to be aware of the data access side's structure, and the data access side never needs to depend on the structure of individual pieces of logic—each maintains its own independent responsibility.

This design is grounded in the philosophy that "the world does not exist as a single, fixed, objective reality, but rather reveals its meaning and form according to the questions and purposes held by the observing subject." The interface that logic should see should not be dictated by the constraints of the data access side, but should instead be defined based on the logic's own context and needs. The same holds true in reverse for the interface that data access should see. What matters to the logic is not the structure of the database or the ORM, but the "capability required to realize this particular use case." What matters to the data access side, on the other hand, is how to access storage or external services. By having both sides define their interfaces according to their own respective contexts, and having the `DataHub` bridge them together, true loose coupling is achieved—one where neither side depends on the other's internal structure.

Furthermore, because logic never needs to know any implementation details of data access, it can easily be swapped out for mocks that provide the necessary capabilities during testing, achieving high testability as well.

### A Structure That Routes Controller-Layer I/O Directly to the Data Access Layer, Bypassing the Logic Layer

The latter approach—routing input/output from the controller layer directly to the data access layer, completely bypassing the logic layer—was conceived by decomposing the controller layer's role into two elements: "invoking logic" and "input/output data." In conventional architectures, because these two elements were never separated, a layered structure whose sole responsibility was data flow (the so-called "data bucket brigade") became unavoidable, forcing data to be transformed every time it crossed a layer. However, by routing input/output data directly to the infrastructure layer (the data access division), this redundant data flow—and the layered structure that existed to support it—becomes entirely unnecessary.

As a result, the hierarchical dependency that would normally remain between logic and data access is completely eliminated, and is elevated instead into an equal relationship based on a contract defined by the interface's method signatures. This can be seen as an evolutionary extension of the Dependency Inversion Principle (DIP)—taking the conventional DIP, which merely reverses the direction of dependency, a step further by minimizing and localizing the dependency itself within the boundary of the contract.

### An AI-Friendly, Capability-Oriented Framework

Moreover, this structure is also well-suited to AI-driven automated programming. For AI-driven automated programming to be performed safely and accurately, the following conditions are required:

* **Localized dependencies**: The scope of dependencies an AI must grasp for a single change should be confined to a narrow portion of the system, not the system as a whole.
* **Explicit boundaries of side effects**: The extent to which side effects can propagate should be made explicit in the code itself, rather than relying on implicit call ordering.
* **Localized impact of change**: Adding features or swapping implementations should not ripple out into existing code that doesn't use them.
* **Substitutability through contract**: An implementation should be safely replaceable as long as it satisfies the contract (interface), without needing to know its implementation details.

**sabi** satisfies all of these conditions at the structural level by defining a dedicated data access interface for each piece of logic and making that logic depend only on that narrow interface—so that adding functionality to a particular `DataAcc` never affects existing logic, and swapping implementations can be done safely as long as the interface's contract is satisfied. In this way, **sabi** is not merely a DI framework, but an AI-friendly, capability-oriented framework suited to the modern AI era.


## Install

This package can be installed from [Maven Central Repository][mvn-url].

The examples of declaring that repository and the dependency on this package in Maven `pom.xml` and Gradle `build.gradle` are as follows:

### For Maven

```
  <dependencies>
    <dependency>
      <groupId>io.github.sttk</groupId>
      <artifactId>sabi</artifactId>
      <version>0.8.0</version>
    </dependency>
  </dependencies>
```

### For Gradle

```
repositories {
  mavenCentral()
}
dependencies {
  implementation 'io.github.sttk:sabi:0.8.0'
}
```

## Usage

### 1. Implementing DataSrc and DataConn

First, you'll define `DataSrc` which manages connections to external data services and creates `DataConn`. Then, you'll define `DataConn` which represents a session-specific connection and implements transactional operations.

```java
import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.AsyncGroup;

class FooDataSrc implements DataSrc {
  @Override public void setup(AsyncGroup ag) throws Err {}
  @Override public void close() {}
  @Override public DataConn createDataConn() throws Err { return new FooDataConn(); }
}

class FooDataConn implements DataConn {
  @Override public void commit(AsyncGroup ag) throws Err {}
  @Override public void rollback(AsyncGroup ag) {}
  @Override public void close(AsyncGroup ag) {}
}

class BarDataSrc implements DataSrc {
  @Override public void setup(AsyncGroup ag) throws Err {}
  @Override public void close() {}
  @Override public DataConn createDataConn() throws Err { return new BarDataConn(); }
}

class BarDataConn implements DataConn {
  @Override public void commit(AsyncGroup ag) throws Err {}
  @Override public void rollback(AsyncGroup ag) {}
  @Override public void close(AsyncGroup ag) {}
}
```

### 2. Implementing logic functions and data interfaces

Define interfaces and functions that express your application logic. These interfaces are independent of specific data source implementations, improving testability.

```java
import com.github.sttk.errs.Err;
import com.github.sttk.sabi.Logic;

interface MyData {
  String getText() throws Err;
  void setText(String text) throws Err;
}

class MyLogic implements Logic<MyData> {
  @Override public void run(MyData data) throws Err {
    String text = data.getText();
    data.setText(text);
  }
}
```

### 3. Implementing DataAcc derived classes

The `DataAcc` interface abstracts access to data connections. The methods defined here will be used to obtain data connections via `DataHub` and perform actual data operations.

```java
import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataAcc;

interface GettingDataAcc extends DataAcc, MyData {
  @Override default String getText() throws Err {
    var conn = getDataConn("foo", FooDataConn.class);
    // ...
    return "output text";
  }
}

interface SettingDataAcc extends DataAcc, MyData {
  @Override default void setText(String text) throws Err {
    var conn = getDataConn("bar", BarDataConn.class);
    // ...
  }
}
```

### 4. Integrating data interfaces and DataAcc derived classes into `DataHub`

The `DataHub` is the central component that manages all `DataSrc` and `DataConn`, providing access to them for your application logic. By implementing the data interface (`MyData`) from step 2 and the `DataAcc` class from step 3 on `DataHub`, you integrate them.

```java
import com.github.sttk.errs.Err;
import com.github.sttk.sabi.DataHub;

class MyDataHub extends DataHub implements GettingDataAcc, SettingDataAcc {}
```

### 5. Using logic functions and `DataHub`

Inside your init function, register your global `DataSrc`. Next, main function calls run function, and inside run function, setup the sabi framework. Then, create an instance of `DataHub` and register the necessary local `DataSrc` using the Uses method. Finally, use the txn method of `DataHub` to execute your defined application logic function (`MyLogic`) within a transaction. This automatically handles transaction commits and rollbacks.

```java
import com.github.sttk.errs.Err;
import com.github.sttk.sabi.Sabi;

public class Main {
  static {
    // Register global DataSrc.
    Sabi.uses("foo", new FooDataSrc());
  }

  public static void main(String[] args) {
    // Set up the sabi framework.
    try (var ac = Sabi.setup()) {

      // Creates a new instance of DataHub.
      var hub = new MyDataHub();

      // Register session-local DataSrc to DataHub.
      hub.uses("bar", new BarDataSrc());

      // Execute application logic within a transaction.
      // MyLogic performs data operations via DataHub.
      hub.txn(new MyLogic());

    } catch (Exception e) {
      System.exit(1);
    }
  }
}
```


## Native build

This framework supports native build with GraalVM.

See the following pages to setup native build environment on Linux/macOS or Windows.
- [Setup native build environment on Linux/macOS](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Setup native build environment on Windows](https://www.graalvm.org/latest/docs/getting-started/windows/#prerequisites-for-native-image-on-windows)

And see the following pages to build native image with Maven or Gradle.
- [Native image building with Maven plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)
- [Native image building with Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)

Since this framework does not use Java reflections, etc., any native build configuration files are not needed.

And all logic and data access implementations should not use them, too.
However, some of client libraries provided for data sources might use them,
and it might be needed those configuration files.


## Supporting JDK versions

This framework supports JDK 21 or later.

### Actually checked JDK versions:

- Oracle GraalVM 25.0.1+8.1 (build 25.0.1+8-LTS-jvmci-b01)
- Oracle GraalVM 24.0.2+11.1 (build 24.0.2+11-jvmci-b01)
- Oracle GraalVM 23.0.2+7.1 (build 23.0.2+7-jvmci-b01)
- Oracle GraalVM 21.0.9+7.1 (build 21.0.9+7-LTS-jvmci-23.1-b79)

## License

Copyright (C) 2022-2026 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[mvn-img]: https://img.shields.io/badge/maven_central-0.8.0-276bdd.svg
[mvn-url]: https://central.sonatype.com/artifact/io.github.sttk/sabi/0.8.0
[mvnrepo-img]: https://img.shields.io/badge/mvn_repository-0.8.0-498df4.svg
[mvnrepo-url]: https://mvnrepository.com/artifact/io.github.sttk/sabi
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk.github.io/sabi-java/
[ci-img]: https://github.com/sttk/sabi-java/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk/sabi-java/actions?query=branch%3Amain
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
