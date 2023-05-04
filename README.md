# [Sabi][repo-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A small framework to separate logics and data accesses for Java application.

## Concept

Sabi is a small framework for Java applications. This framework separates an application to logic parts and data access parts, and enables to implement each of them independently, then to combine them.

### Separation of logics and data accesses

In general, a program consists of procedures and data.
And procedures include data accesses for operating data, and the rest of procedures are logics.
So we can say that a program consists of logics, data accesses and data.

Furthermore, we often think to separate an application to multiple layers, for example, controller layer, application logic layer, and data access layer.
The logic and data access mentioned in this framework are partially matched those layers, but are not matched in another part.
For example, in the controller layer, there are input data and output data.
(In a web application there are request data and response data, and in a command line application there are console input and output.)
Even though all logical processes are moved into the application logic layer, it is remained to transform input data of the controller layer into input data of the application logic layer, and to transform output data of the application logic layer into the output data of the controller layer.
The data accesses mentioned in this framework also includes those data accesses.

### Changes composition of data access methods by concerns

Dax is a collection of data access methods.
These methods will be collected/divided by data source from an implementation perspective.
On the other hand, they will be collected/divided by logic from a usage perspective

In general programming, a developer chooses the necessary methods for their logic from among all available methods.
And after programming, those methods will be buried in the program code of the logic, and it will become unclear which methods are used without tracing the logic.

In applications using the Sabi framework, a logic is implemented as a function that takes only one argument, a dax interface.
And this interface can define only the methods required by the logic.
Therefore, a dax interface can make clear which methods are used in a logic.
And also, a dax interface can constraint methods available for a logic.

## Install

This package can be installed from [local-m2-repository][local-m2-repo-url] which is the Maven repository located on your local environment.

The examples of declaring that repository and the dependency on this package in Maven `pom.xml` and Gradle `build.gradle` are as follows:

### for Maven

```
<project ...>
  <repositories>
    <repository>
      <id>local-m2-repository</id>
      <url>file://${user.dir}/../local-m2-repository</url>
    </repository>
  </repositories>
  <dependencies>
    <dependency>
      <groupId>sttk-java</groupId>
      <artifactId>sabi</artifactId>
      <version>0.1.0</version>
    </dependency>
  </dependencies>
</project>
```

### for Gradle

```
repositories {
  maven {
    url "${rootDir}/../local-m2-repository"
  }
}
dependencies {
  implementation 'sttk-java:sabi:0.1.0'
}
```


## Usage

This framework enables to write codes and unit tests of logic parts and data access parts separately.

### Logic

A logic part is implemented as an instance of `Logic` functional interface.
This interface has `run` method which performs this operation, and this method has only an argument, `dax` which is an interface and collects data access methods used in this logic.
Since the dax hides details of data access procedures, only logical procedure appears in this logic.
In this logic part, it's no concern where a data comes from or goes to.

For example, in the following code, `GreetLogic` is a logic class which implements `Logic` interface, and `GreetDax` is a dax interfacce.

```
public interface GreetDax {
  String getUserName() throws Err;
  void say(String greeting) throws Err;

  // possible error reasons
  static record NoName() {}
  static record FailToOutput(String text) {}
}
```
```
public class GreetLogic implements Logic<GreetDax> {
  @Override
  public void run(final GreetDax dax) throws Err {
    final String name = dax.getUserName();
    dax.say("Hello, " + name);
  }
}
```

In `GreetLogic` class, there are no codes for getting a user name and output
a greeting text.
In this logic class, it's only concern to create a greeting text from a user name.

### Dax for unit tests

To test a logic class, the simplest dax implementation is what using a map.
The following code is an example of a dax implementation using a map and having two methods: `getUserName` and `say` which are same to `GreetDax` interface above.

```
public class MapGreetDaxBase extends GreetDax, DaxBase {
  private Map<String, Object> map = new HashMap<>();

  public String getName() throws Err {
    var username = this.map.get("username");
    if (username != null) {
      throw new Err(new NoName());
    }
    return String.class.cast(username);
  }

  public void say(String greeting) throw Err {
    this.map.put("greeting", greeting);
  }
}
```

And the following code is an example of a test case.

```
public GreetLogicTest {

  @Test
  void should_run_GreetLogic_normally() throws Err {
    var base = new MapGreetDaxBase();
    base.map.put("username", "World");
    try {
      Txn.run(base, new GreetLogic());
      assertThat(base.map.get("greeting")).isEqualTo("Hello, World");
    } catch (Err e) {
      fail(e);
    }
  }
}
```

### Dax for real data access

In actual case, multiple data sources are often used.
In this example, an user name is input as command line argument, and greeting is output to standard output (console output).
Therefore, two dax implementations are attached to the single GreetDax interface.

The following code is an example of a dax implementation which inputs an user name from command line argument.

```
import dax.clidax.ArgsDax;
import dax.clidax.ArgsDaxConn;

public interface CliArgsUserDax extends GreetDax, ArgsDax {
  default String getUserName() throws Err {
    final ArgDaxConn conn = getArgDaxConn("cliargs");
    if (conn.args.length <= 1) {
      throw new Err(new NoName());
    }
    return conn.args.get(0);
  }
}
```

In addition, the following code is an example of a dax implementation which outputs a greeting test to console.

```
public interface ConsoleOutputDax extends GreetDax {
  default void say(final String text) throws Err {
    try {
      System.out.println(text);
    } catch (Exception e) {
      throw new Err(new FailToOutput(text), e);
    }
  }
}
```

And these dax implementations are combined to a `DaxBase` as follows:

```
public class GreetDaxBase extends DaxBase
  implements CliArgsUserDax, ConsoleOutputDax {}
```

### Executing logic

The following code implements a `main` function which execute a `GreetLogic`.
`Txn.run` executes the `GreetLogic` function in a transaction process.

```
import sabi.*;
import sabi.DaxBase.*;
import dax.clidax.ArgsDaxSrc;

public class GreetApp {
  public static void main(String[] args) {
    try {
      addGlobalDaxSrc("cliargs", new ArgsDaxSrc(args));
      startUpGlobalDaxSrcs();

      var base = new GreetDaxBase();
      Txn.run(base, new GreetLogic());

    } catch (Err e) {
      System.exit(1);
    } finally {
      shutownGlobalDaxSrcs();
    }
  }
}
```

### Moving outputs to another transaction process

`Txn.run` executes operations of logic classes in a transaction.
If an logic operation updates database and causes an error in the transaction, its update is rollbacked.
If console output is executed in the same transaction with database update, the rollbacked result is possible to be output to console.
Therefore, console output is wanted to execute after the transaction of database update in successfully completed.

What should be done to achieve it are to add a dax interface for next transaction, to change `ConsoleOutputDax` to hold a greeting text in `say` method, add a new method to output it in next transaction, and to execute the next transaction in the `main` function.

```
public interface PrintDax {
  void print() throws Err;
}
```
```
import dax.mapdax.MapDax;
import dax.mapdax.MapDaxConn;

public interface ConsoleOutputDax extends GreetDax, MapDax {
  default void say(final String text) throws Err {
    final MapDaxConn conn = getMapDaxConn("map");
    conn.map.put("text", text);
  }
  default void print() throws Err {
    try {
      final MapDaxConn conn = getMapDaxConn("map");
      var text = String.class.cast(conn.map.put("text");
      System.out.println(text);
    } catch (Exception e) {
      throw new Err(new FailToOutput(text), e);
    }
  }
}
```

And the `main` function is modified as follows:

```
import sabi.*;
import sabi.DaxBase.*;
import dax.clidax.CliDaxSrc;
import dax.mapdax.MapDaxSrc;

public class GreetApp {
  public static void main(String[] args) {
    try {
      addGlobalDaxSrc("cliargs", new CliDaxSrc(args));
      addGlobalDaxSrc("map", new MapDaxSrc());
      startUpGlobalDaxSrcs();

      var base = new GreetDaxBase();
      Txn.run(base, new GreetLogic());
      Txn.run(base, (PrintDax dax) -> {
        return dax.Print();
      });

    } catch (Err e) {
      System.exit(1);
    } finally {
      shutownGlobalDaxSrcs();
    }
  }
}
```

Or, the `main` function is able to rewrite as follows:

```
import sabi.*;
import sabi.DaxBase.*;
import dax.clidax.CliDaxSrc;
import dax.mapdax.MapDaxSrc;

public class GreetApp {
  public static void main(String[] args) {
    try {
      addGlobalDaxSrc("cliargs", new CliDaxSrc(args));
      addGlobalDaxSrc("map", new MapDaxSrc());
      startUpGlobalDaxSrcs();

      var base = new GreetDaxBase();
      var txn0 = new Txn(base, new GreetLogic());
      var txn1 = new Txn(base, (PrintDax dax) -> {
        return dax.Print();
      });
      Seq.run(txn0, txn1);

    } catch (Err e) {
      System.exit(1);
    } finally {
      shutownGlobalDaxSrcs();
    }
  }
}
```

The important point is that the `GreetLogic` class is not changed. Since this change is not related to the application logic, it is confined to the data access part only.

## Native build

This framework intends to support native build with GraalVM.
This framework does not use Java reflections, and all `dax` implementations should not use them, too.
However, some of client libraries provided for data sources might use them, and it might be needed to prepare a `reflect-config.json` file.

See the following pages to build native image with Maven or Gradle.
- [Native image building with Maven plugin](https://www.graalvm.org/dev/reference-manual/native-image/guides/use-native-image-maven-plugin/)
- [Native image building with Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)

## Supporting JDK versions

This framework supports JDK 17 or later.

### Actually checked JDK versions:

- GraalVM CE 22.3.0 (OpenJDK 19.0.1)
- GraalVM CE 22.3.0 (OpenJDK 17.0.5)
- GraalVM CE 22.2.0 (OpenJDK 17.0.4)
- GraalVM CE 22.1.0 (OpenJDK 17.0.3)


## License

Copyright (C) 2022-2023 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk-java/sabi
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk-java.github.io/sabi/
[ci-img]: https://github.com/sttk-java/sabi/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk-java/sabi/actions
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT

[local-m2-repo-url]: https://github.com/sttk-java/local-m2-repository
