#!/usr/bin/env bash

clean() {
  mvn clean
}

compile() {
  mvn compile
}

test() {
  mvn test
}

jar() {
  mvn package
}

javadoc() {
  mvn javadoc:javadoc
}

deps() {
  mvn versions:display-dependency-updates
}

sver() {
  serialver -classpath target/classes $1
}

case "$1" in
clean)
  clean
  ;;
compile)
  compile
  ;;
test)
  test
  ;;
jar)
  jar
  ;;
javadoc)
  javadoc
  ;;
deps)
  deps
  ;;
sver)
  sver $2
  ;;
'')
  clean
  compile
  test
  jar
  javadoc
  ;;
esac

