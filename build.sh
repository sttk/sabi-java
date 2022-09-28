#!/usr/bin/env bash

compile() {
  mvn compile
}

test() {
  mvn test
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
compile)
  compile
  ;;
test)
  test
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
esac

