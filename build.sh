#!/usr/bin/env bash

end() {
  exitcd=$1
  if [[ "$exitcd" != "0" ]]; then
    exit $exitcd
  fi
}

clean() {
  mvn clean
  end $?
}

compile() {
  mvn compile
  end $?
}

test() {
  mvn test
  end $?
}

jar() {
  mvn package
  end $?
}

javadoc() {
  mvn javadoc:javadoc
  end $?
}

deps() {
  mvn versions:display-dependency-updates
  end $?
}

sver() {
  serialver -classpath target/classes $1
  end $?
}

native_test() {
  mvn -Pnative test
  end $?
}

deploy() {
  mvn deploy
  end $?
}


if [[ "$#" == "0" ]]; then
  clean
  jar
  javadoc
  native_test
else
  for a in "$@"; do
    case "$a" in
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
    'native-test')
      native_test
      ;;
    deploy)
      deploy
      ;;
    *)
      echo "Bad task: $a"
      exit 1
      ;;
    esac
  done
fi

