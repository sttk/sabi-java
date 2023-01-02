# [Sabi][repo-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A small framework for Java applications.

- [What is this?](#what-is-this)
- [Usage](#usage)
- [Native build](#native-build)
- [Supporting JDK versions](#support-jdk-versions)
- [License](#license)

<a name="what-is-this"></a>
## What is this?

Sabi is a small framework to separate logics and data accesses for Java applications.

A program consists of procedures and data.
And to operate data, procedures includes data accesses, then the rest of procedures except data accesses are logics.
Therefore, a program consists of logics, data accesses and data.

This package is an application framework which explicitly separates procedures into logics and data accesses as layers.
By using this framework, we can remove codes for data accesses from logic parts, and write only specific codes for each data source (e.g. database, messaging services, files, and so on)  in data access  parts. 
Moreover, by separating as layers, applications using this framework can change data sources easily by switching data access parts.

<a name="usage"></a>
## Usage

This framework enables to write codes and unit tests of logic parts and data access parts separately.

### Writing logic

A logic part is implemented as an instance of `Logic` functional interface.
This `execute` method takes a dax, which is an abbreviation of 'data access', as an argument.
A dax has all methods to be used in a logic, and each method is associated with each data access procedure to target data sources.
Since a dax conceals its data access procedures, only logical procedure appears in a logic part.
In a logic part, these are no concern where a data comes from and a data goes to.

For example, `GreetLogic` a logic class and `GreetDax` is a dax interface:

```
public interface GreetDax {
  String getName() throws Err;
  void say(String greeting) throws Err;
}
```
```
public class GreetLogic implements Logic<GreetDax> {
  @Override
  public void execute(final GreetDax dax) throws Err {
    final String name = dax.getName();
    dax.say("Hello, " + name);
  }
}
```

In `GreetLogic`, there are no detail codes for getting name and putting greeting.
In this logic class, it's only concern to convert a name into a greeting.

### Writing dax for unit tests of logic.

To test a logic, the simplest dax implementation is what using a map.
The following code is an example which implements two methods: `getName` and `say` which are same to `GreetDax` interface above.

```
public class MapDax extends DaxBase implements GreetDax {
  Map<String, String> map = new HashMap<>();

  public record NoName() {}; // An error reason when getting no name.

  public String getName() throws Err {
    try {
      return this.map.get("name");
    } catch (Exception e) {
      throw new Err(new NoName(), e);
    }
  }

  public void say(final String greeting) throws Err {
    this.map.put("greeting", greeting);
  }
}
```

And the following code is an example of a test case.

```
public class GreetLogicTest {
  @Test
  void executeLogic() throws Err {
    var dax = new MapDax();
    dax.map.put("name", "World");

    var proc = new Proc<GreetDax>(dax);
    proc.runTxn(new GreetLogic());

    assertThat(dax.map.get("greeting")).isEqualTo("Hello, World");
  }
}
```

### Writing dax for real data accesses

An actual dax ordinarily consists of multiple sub dax by input sources and output destinations.

The following code is an example of `dax` with no external data source.
This `dax` outputs a greeting to standard output.

```
public interface SayConsoleDax extends GreetDax {
  @Override
  default void say(final String text) throws Err {
    System.out.println(text);
  }
}
```

And the following code is an example of a `dax` with an external data source.
This `UserJdbcDax` accesses to a dataase and provides an implementation of `getName` method of `GreetDax`.

```
public interface UserJdbcDax extends JdbcDax, GreetDax {
  public record NoUser() {}
  public record FailToQueryUserName() {}

  default String getName() throws Err {
    try (
      var conn = this.getJdbcDaxConn("jdbc").getConnection();
      var stmt = conn.prepareStatement("SELECT username FROM users LIMIT 1")
    ) {
      try (var rs = stmt.executeQuery()) {
        if (rs.next()) {
          return rs.getString("username");
        }
        throw new Err(new NoUser());
      }
    } catch (Exception e) {
      throw new Err(new FailToQueryUserName(), e);
    }
  }
}
```

### Mapping dax interface and implementations

A `dax` interface can be related to multiple `dax` implementations.

In the following code, `getName` method of `GreetDax` interface is corresponded to the same named method of `UserJdbcDax`, and `say` method of `GreetDax` interface is corresponded to the same named method of `SayConsoleDax`.

```
class GreetDaxImpl extends DaxBase
  implements SayConsoleDax, UserJdbcDax {}

public class GreetProc extends Proc<GreetDax> {
  public GreetProc() {
    super(new GreetDaxImpl());
  }
}
```

### Executing logic

The following code implements a `main` function which execute a `GreetLogic`.
`GreetLogic` is executed in a transaction process by `Proc#RunTxn`, so the database update can be rollbacked if an error is occured.

The static block registers a `JdbcDaxSrc` which creates a `JdbcDaxConn` which connects to a database.
The `JdbcDaxConn` is registered with a name `"jdbc"` and is obtained by `getJdbcDaxConn("sql")` in `UserJdbcDax#getName`.

```
public class GreetApp {
  static {
    DaxBase.addGlobalDaxSrc("jdbc", new JdbcDaxSrc("database-url"));
    DaxBase.fixGlobalDaxSrcs();
  }

  public static void main(String[] args) {
    var proc = new GreetProc();
    try {
      proc.runTxn(new GreetLogic());
    } catch (Exception e) {
      System.exit(1);
    }
  }
}
```

<a name="native-build"></a>
## Native build

This framework intends to support native build with GraalVM.
This framework does not use Java reflections, and all `dax` implementations should not use them, too.
However, some of client libraries provided for data sources might use them, and it might be needed to prepare a `reflect-config.json` file.

See the following pages to build native image with Maven or Gradle.
- [Native image building with Maven plugin](https://www.graalvm.org/dev/reference-manual/native-image/guides/use-native-image-maven-plugin/)
- [Native image building with Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)

<a name="support-jdk-versions"></a>
## Supporting JDK versions

This framework supports JDK 17 or later.

### Actually checked JDK versions:

- GraalVM CE 22.1.0 (OpenJDK 17.0.3)


<a name="license"></a>
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
