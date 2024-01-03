# [Sabi][repo-url] [![GitHub.io][io-img]][io-url] [![CI Status][ci-img]][ci-url] [![MIT License][mit-img]][mit-url]

A small framework to separate logics and data accesses for Java application.

## Concept

The overall concept of this framework is separation and reintegration of
necessary and redundant parts based on the perspectives of the whole and the
parts.
The separation of logics and data accesses is the most prominent and
fundamental part of this concept.


### Separation of logics and data accesses

In general, a program consists of procedures and data.
And procedures include data accesses for operating data, and the rest of
procedures are logics.
So we can say that a program consists of logics, data accesses and data.

We often think to separate an application to multiple layers, for example,
controller layer, business logic layer, and data access layer.
The logics and data accesses mentioned in this framework may appear to follow
such layering.
However, the controller layer also has data accesses such as transforming user
requests and responses for the business logic layer.
Generally, such layers of an application is established as vertically
positioned stages of data processing within a data flow.

In this framework, the relationship between logics and data accesses is not
defined by layers but by lanes.
Although their relationship is vertical in terms of invocation, it is
conceptually horizontal.
`DaxBase` serves as an intermediary that connects both of them.


### Separation of data accesses for each logic

A logic is a functional interface of which the sole method takes a dax
interface as its only one argument.
The type of this dax is declared by the type parameter of the logic interface,
and also the type parameter of the transaction method, `DaxBase#txn`, that
executes logics.

Therefore, since the type of dax can be changed for each logic or transaction,
it is possible to limit data accesses used by the logic, by declaring only
necessary data access methods from among ones defined in `DaxBase` instance.

At the same time, since all data accesses of a logic is done through this sole
dax interface, this dax interface serves as a list of data access methods used
by a logic.


### Separation of data accesses by data sources and reintegration of them

Data access methods are implemented as methods of some `Dax` structs that
embedding a `DaxBase`.
Furthermore these `Dax` structs are integrated into a single new `DaxBase`.

A `Dax` struct can be created at any unit, but it is clearer to create it at
the unit of the data source.
By doing so, the definition of a new `DaxBase` also serves as a list of the
data
sources being used.


## Usage

### Logic and an interface for its data access

A logic is implemented as a functionnal interface.
This sole method takes only an argument, dax, which is an interface that
gathers only the data access methods needed by this logic interface.

Since a dax for a logic conceals details of data access procedures, this
interface only includes logical procedures.
In this logical part, there is no concern about where the data is input from
or where it is output to.

For example, in the following code, `GreetLogic` is a logic interface and
`GreetDax` is a dax interface for `GreetLogic`.

```
interface GreetDax {
  record NoName() {}
  record FailToGetHour() {}
  record FailToOutput(String text) {}

  String getUserName() throws Err;
  int getHour() throws Err;
  void output(String text) throws Err;
}

class GreetLogic implements Logic<GreetDax> {
  @Override public void run(GreetDax dax) throws Err {
    int hour = dax.getHour();

    String s;
    if (5 <= hour && hour < 12) {
      s = "Good morning, ";
    } else if (12 <= hour && hour < 16) {
      s = "Good afternoon, ";
    } else if (16 <= hour && hour < 21) {
      s = "Good evening, ";
    } else {
      s = "Hi, ";
    }
    dax.output(s);

    var name = dax.getUserName();
    dax.output(name + ".\n");
  }
}
```

In `GreetLogic,` there are no codes for inputting the hour, inputting a user
name, and outputing a greeting.
This logic function has only concern to create a greeting text.

### Data accesses for unit testing

To test a logic interface, the simplest dax struct is what using a map.
The following code is an example of a dax struct using a map and having three
methods that are same to `GreetDax` interface methods above.

```
class MapGreetDax extends DaxBase implements GreetDax {
  Map<String, Object> m = new HashMap<>();

  @Override public String getUserName() throws Err {
    var name = this.m.get("username");
    if (name == null) {
      throw new Err(new NoName());
    }
    return String.class.cast(name);
  }

  @Override public int getHour() throws Err {
    var hour = this.m.get("hour");
    if (hour == null) {
      throw new Err(new FailToGetHour());
    }
    return Integer.class.cast(hour);
  }

  @Override public void output(String text) throws Err {
    String s = "";
    var v = this.m.get("greeting");
    if ("error".equals(v)) {
      throw new Err(new FailToOutput(text));
    } else if (v != null) {
      s += v;
    }
    this.m.put("greeting", s + text);
  }
}
```

And the following code is an example of a test case.

```
  @Test void testGreetLogic_morning() {
    var base = new MapGreetDaxBase();
    base.m.put("username", "everyone");
    base.m.put("hour", 10);

    try (base) {
      base.txn(new GreetLogic());
    } catch (Err e) {
      fail(e.toString());
    }

    assertEquals(base.m.get("greeting"), "Good morning, everyone.\n");
  }
```

### Data accesses for actual use

In actual use, multiple data sources are often used.
In this example, an user name and the hour are input as an environment
variable, and greeting is output to console.
Therefore, two dax struct are created and they are integrated into a new
struct based on `DaxBase`.
Since Golang is structural typing language, this new `DaxBase` can be casted
to `GreetDax`.

The following code is an example of a dax struct which inputs an user name and
the hour from an environment variable.

```
interface EnvVarsDax extends GreetDax, Dax {
  @Override default String getUserName() throws Err {
    var u = System.getenv("GREETING_USERNAME");
    if (u == null || u.isBlank()) {
      throw new Err(new NoName());
    }
    return u;
  }

  @Override default int getHour() throws Err {
    var h = System.getenv("GREETING_HOUR");
    try {
      return Integer.valueOf(h);
    } catch (Exception e) {
      throw new Err(new FailToGetHour(), e);
    }
  }
}
```

The following code is an example of a dax struct which output a text to
console.

```
interface ConsoleDax extends GreetDax, Dax {
  @Override default void output(String text) throws Err {
    System.out.print(text);
  }
}
```

And the following code is an example of a constructor function of a struct
based on `DaxBase` into which the above two dax are integrated.
This implementation also serves as a list of the external data sources being
used.

```
class GreetDaxBase extends DaxBase
  implements EnvVarsDax, ConsoleDax {}
```

### Executing a logic

The following code executes the above `GreetLogic` in a transaction process.

```
public class GreetApp {
  public static void main(String[] args) {
    try (var ac = Sabi.startApp()) {
      app();
    } catch (Err e) {
      System.err.println(e.toString());
      System.exit(1);
    }
  }

  static void app() throws Err {
    try (var base = new GreetDaxBase()) {
      base.txn(new GreetLogic());
    }
  }
}
```


### Changing to a dax of another data source

In the above codes, the hour is obtained from command line arguments.
Here, assume that the specification has been changed to retrieve it from
system clock instread.

```
interface SystemClockDax extends GreetDax, Dax {
  @Override default int getHour() throws Err {
    return OffsetTime.now().getHour();
  }
}
```

And the `DaxBase` struct, into which multiple dax structs have been integrated,
is modified as follows.

```
class GreetDaxBase extends DaxBase
  implements EnvVarsDax, SystemClockDax, ConsoleDax {}  // Changed
```

### Moving outputs to next transaction process

The above codes works normally if no error occurs.
But if an error occurs at getting user name, a incomplete string is being
output to console.
Such behavior is not appropriate for transaction processing.

So we should change the above codes to store in memory temporarily in the
existing transaction process, and then output to console in the next
transaction.

The following code is the logic to output text to console in next transaction
process and the dax interface for this logic.

```
interface PrintDax {
  String getText() throws Err;
  void print(String text) throws Err;
}

class PrintLogic extends Logic<PrintDax> {
  @Override public void  run(PrintDax dax) throws Err {
    var text = dax.getText();
    return dax.print(text);
  }
}
```

Here, we try to create a `DaxSrc` and `DaxConn` for memory store, too.
Since a dax interface cannot have its own state, the `DaxSrc` holds the memory
store as its state.

The following codes are the implementations of `MemoryDaxSrc`, `MemoryDaxConn`,
and `MemoryDax`.

```
class MemoryDaxSrc implements DaxSrc {
  StringBuilder buf = new StringBuilder();

  @Override public void setup(AsyncGroup ag) throws Err {
  }

  @Override public void close() {
    buf.setLength(0);
  }

  @Override public DaxConn createDaxConn() throws Err {
    return new MemoryDaxConn(buf);
  }
}
```
```
class MemoryDaxConn implements DaxConn {
  StringBuilder buf;

  public MemoryDaxConn(StringBuilder buf) {
    this.buf = buf;
  }

  public void append(String text) {
    this.buf.append(text);
  }

  public String get() {
    return this.buf.toString();
  }

  @Override public void commit(AsyncGroup ag) throws Err {
  }

  @Override public boolean isCommitted() {
    return true;
  }

  @Override public void rollback(AsyncGroup ag) {
  }

  @Override public void forceBack(AsyncGroup ag) {
    buf.setLength(0);
  }

  @Override public void close() {
  }
}
```
```
interface MemoryDax extends GreetDax, PrintDax, Dax {
  @Override default void output(String text) throws Err {
    MemoryDaxConn conn = getDaxConn("memory");
    conn.append(text);
  }

  @Override default String getText() throws Err {
    MemoryDaxConn conn = getDaxConn("memory");
    return conn.get();
  }
}
```
```
class GreetDaxBase extends DaxBase
  implements EnvVarsDax, SystemClockDax, MemoryDax, ConsoleDax {}  // Changed
```
```
  void app() throws Err {
    try (var base = new GreetDaxBase()) {
      base.uses("memory", new MemoryDaxSrc());  // Added
      base.txn(new GreetLogic());
      base.txn(new PrintLogic());  // Added
    }
  }
```

And we need to change the name of the method `ConsoleDax#output` to avoid name
collision with the method `MemoryDax#output`.

```
interface ConsoleDax extends PrintDax, Dax {  // Changed from GreetDax
  @Override default void print(String text) throws Err { // Changed from Output
    System.out.print(text);
  }
}
```

That completes it.

The important point is that the `GreetLogic` function is not changed.
Since these changes are not related to the existing application logic, it is
limited to the data access part (and the part around the newly added logic)
only.


## Native build

This framework supports native build with GraalVM.

See the following pages to setup native build environment on Linux/macOS or Windows.
- [Setup native build environment on Linux/macOS](https://www.graalvm.org/latest/reference-manual/native-image/)
- [Setup native build environment on Windows](https://www.graalvm.org/latest/docs/getting-started/windows/#prerequisites-for-native-image-on-windows)

And see the following pages to build native image with Maven or Gradle.
- [Native image building with Maven plugin](https://graalvm.github.io/native-build-tools/latest/maven-plugin.html)
- [Native image building with Gradle plugin](https://graalvm.github.io/native-build-tools/latest/gradle-plugin.html)

Since this framework does not use Java reflections, etc., any native build configuration files are not needed.

And all `dax` implementations should not use them, too.
However, some of client libraries provided for data sources might use them,
and it might be needed those configuration files.


## Supporting JDK versions

This framework supports JDK 21 or later.

### Actually checked JDK versions:

- GraalVM CE 21.0.1+12.1 (openjdk version 21.0.1)


## License

Copyright (C) 2022-2023 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk/sabi-java
[io-img]: https://img.shields.io/badge/github.io-Javadoc-4d7a97.svg
[io-url]: https://sttk.github.io/sabi-java/
[ci-img]: https://github.com/sttk/sabi-java/actions/workflows/java-ci.yml/badge.svg?branch=main
[ci-url]: https://github.com/sttk/sabi-java/actions
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
