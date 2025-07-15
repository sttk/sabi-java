# [sabi][repo-url] [![Maven Central][mvn-img]][mvn-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A small framework for Java designed to separate logic from data access.

It achieves this by connecting the logic layer and the data access layer via traits, similar to traditional Dependency Injection (DI). This reduces the dependency between the two, allowing them to be implemented and tested independently.

However, traditional DI often presented an inconvenience in how methods were grouped. Typically, methods were grouped by external data service like a database or by database table. This meant the logic layer had to depend on units defined by the data access layer's concerns. Furthermore, such traits often contained more methods than a specific piece of logic needed, making it difficult to tell which methods were actually used in the logic without tracing the code.

This framework addresses that inconvenience. The data access interface used by a logic function is unique to that specific logic, passed as an argument to the logic function. This interface declares all the data access methods that specific logic will use.

On the data access layer side, implementations can be provided by concrete types that fulfill multiple `DataAcc` derived classes. This allows for implementation in any arbitrary unit — whether by external data service, by table, or by functional concern.

This is achieved through the following mechanism:

- A `DataHub` class aggregates all data access methods. `DataAcc` derived classes are attached to `DataHub`, giving `DataHub` the implementations of the data access methods.
- Logic functional interfaces accept specific, narrowly defined data access interfaces as arguments. These interfaces declare only the methods relevant to that particular piece of logic.
- The `DataHub` class implements all of these specific data access interfaces. When a `DataHub` instance is passed to a logic functional interface, the logic functional interface interacts with it via the narrower interface, ensuring it only sees and uses the methods it needs. Using Java's inheritance mechanism, a type implements an interface by methods of other classes. The `DataHub` simply needs to have methods that match the signatures of all the methods declared across the various logic-facing data access interfaces.

This approach provides strong compile-time guarantees that logic only uses what it declares, while allowing flexible organization of data access implementations.

## Installation

This package can be installed from [Maven Central Repository][mvn-url].

The examples of declaring that repository and the dependency on this package in Maven `pom.xml` and Gradle `build.gradle` are as follows:

### For Maven

```
  <dependencies>
    <dependency>
      <groupId>io.github.sttk</groupId>
      <artifactId>sabi</artifactId>
      <version>0.5.0</version>
    </dependency>
  </dependencies>
```

### For Gradle

```
repositories {
  mavenCentral()
}
dependencies {
  implementation 'io.github.sttk:sabi:0.5.0'
}
```

## Usage

### 1. Implementing DataSrc and DataConn

First, you'll define `DataSrc` which manages connections to external data services and creates `DataConn`. Then, you'll define `DataConn` which represents a session-specific connection and implements transactional operations.

```java
import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.DataSrc;
import com.github.sttk.sabi.DataConn;
import com.github.sttk.sabi.AsyncGroup;

class FooDataSrc implements DataSrc {
  @Override public void setup(AsyncGroup ag) throws Exc {}
  @Override public void close() {}
  @Override public DataConn createDataConn() throws Exc { return new FooDataConn(); }
}

class FooDataConn implements DataConn {
  @Override public void commit(AsyncGroup ag) throws Exc {}
  @Override public void rollback(AsyncGroup ag) {}
  @Override public void close(AsyncGroup ag) {}
}

class BarDataSrc implements DataSrc {
  @Override public void setup(AsyncGroup ag) throws Exc {}
  @Override public void close() {}
  @Override public DataConn createDataConn() throws Exc { return new BarDataConn(); }
}

class BarDataConn implements DataConn {
  @Override public void commit(AsyncGroup ag) throws Exc {}
  @Override public void rollback(AsyncGroup ag) {}
  @Override public void close(AsyncGroup ag) {}
}
```

### 2. Implementing logic functions and data traits

Define interfaces and functions that express your application logic. These interfaces are independent of specific data source implementations, improving testability.

```java
import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.Logic;

interface MyData {
  String getText() throws Exc;
  void setText(String text) throws Exc;
}

class MyLogic implements Logic<MyData> {
  @Override public void run(MyData data) throws Exc {
    String text = data.getText();
    data.setText(text);
  }
}
```

### 3. Implementing DataAcc derived classes

The `DataAcc` interface abstracts access to data connections. The methods defined here will be used to obtain data connections via `DataHub` and perform actual data operations.

```java
import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.DataAcc;

interface GettingDataAcc extends DataAcc, MyData {
  @Override default String getText() throws Exc {
    var conn = getDataConn("foo", FooDataConn.class);
    // ...
    return "output text";
  }
}

interface SettingDataAcc extends DataAcc, MyData {
  @Override default void setText(String text) throws Exc {
    var conn = getDataConn("bar", BarDataConn.class);
    // ...
  }
}
```

### 4. Integrating data interfaces and DataAcc derived classes into `DataHub`

The `DataHub` is the central component that manages all `DataSrc` and `DataConn`, providing access to them for your application logic. By implementing the data interface (`MyData`) from step 2 and the `DataAcc` class from step 3 on `DataHub`, you integrate them.

```java
import com.github.sttk.errs.Exc;
import com.github.sttk.sabi.DataHub;

class MyDataHub extends DataHub implements GettingDataAcc, SettingDataAcc {}
```

### 5. Using logic functions and `DataHub`

Inside your init function, register your global `DataSrc`. Next, main function calls run function, and inside run function, setup the sabi framework. Then, create an instance of `DataHub` and register the necessary local `DataSrc` using the Uses method. Finally, use the txn method of `DataHub` to execute your defined application logic function (`MyLogic`) within a transaction. This automatically handles transaction commits and rollbacks.

```java
import com.github.sttk.errs.Exc;
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

- Oracle GraalVM 21.0.7+8.1
- Oracle GraalVM 22.0.2+9.1
- Oracle GraalVM 23.0.2+7.1
- Oracle GraalVM 24.0.1+9.1

## License

Copyright (C) 2022-2025 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk/sabi-java
[mvn-img]: https://img.shields.io/badge/maven_central-0.5.0-276bdd.svg
[mvn-url]: https://central.sonatype.com/artifact/io.github.sttk/sabi/0.5.0
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk.github.io/sabi-java/
[ci-img]: https://github.com/sttk/sabi-java/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk/sabi-java/actions?query=branch%3Amain
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
