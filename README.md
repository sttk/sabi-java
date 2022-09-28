# [Sabi][repo-url] [![MIT License][mit-img]][mit-url]

A small framework for Java applications.

- [What is this?](#what-is-this)
- [Usage](#usage)
- [Supporting JDK versions](#support-jdk-versions)
- [License](#license)

<a name="what-is-this"></a>
## What is this?

Sabi is a small framework to separate logics and data accesses for Java applications.

A program consists of procedures and data.
And to operate data, procedures includes data accesses, then the rest of procedures except data accesses are logics.
Therefore, a program consists of logics, data accesses and data.

This package is an application framework which explicitly separates procedures into logics and data accesses as layers.
By using this framework, we can remove codes for data accesses from logic parts, and write only specific codes for each data source (e.g. database, messaging services files, and so on)  in data access  parts. 
Moreover, by separating as layers, applications using this framework can change data sources easily by switching data access parts.

<a name="usage"></a>
## Usage

### Write logic

### Write dax

### Write procedure

<a name="support-jdk-versions"></a>
## Supporting JDK versions

This framework supports JDK 17 or later.

### Actually checked JDK versions:

- GraalVM CE 22.1.0 (OpenJDK 17.0.3)

<a name="license"></a>
## License

Copyright (C) 2022 Takayuki Sato

This program is free software under MIT License.<br>
See the file LICENSE in this distribution for more details.


[repo-url]: https://github.com/sttk-java/sabi
[mit-img]: https://img.shields.io/badge/license-MIT-green.svg
[mit-url]: https://opensource.org/licenses/MIT
