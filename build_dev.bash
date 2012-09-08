#!/usr/bin/env bash

test_cmd(){
  typeset name="$1"
  which "$name" >/dev/null && return
  echo "The command $name is not installed, please install it"
  exit
}

test_cmd mongod
test_cmd java
test_cmd javac


echo "Chat - git pull"
git pull

echo "Play - git pull"
(
  cd Play20
  git pull
)


echo "done"

#__EOF__
