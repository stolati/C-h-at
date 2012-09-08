#!/usr/bin/env bash
projectHome="$PWD"
extApp="$projectHome/external_app"

playName="play-2.0.3"
playHome="$extApp/$playName"

test_cmd(){
  typeset name="$1"
  which "$name" >/dev/null && return
  echo "The command $name is not installed, please install it"
  exit
}

test_cmd mongod
test_cmd java
test_cmd javac
#test_cmd sbt


echo "Chat - git pull"
git pull



mkdir -p "$extApp"
(
  cd "$extApp"

  #echo "Play - git pull"
  #playHome="$extApp/Play20"
  #[ ! -d "$playHome" ] && git clone --depth 1 git://github.com/playframework/Play20.git
  #( cd "$playHome" ; git pull )

  #echo "Sbt - git pull"
  #sbtHome="$extApp/xsbt"
  #[ ! -d "$sbtHome" ] && git clone --depth 1 git://github.com/harrah/xsbt.git
  #( cd "$sbtHome" ; git pull)

  echo "Dl Play"
  if [ ! -d "$playHome" ]; then
    wget "http://download.playframework.org/releases/${playName}.zip"
    unzip "${playName}.zip"
    rm "${playName}.zip"
  fi
)

"$playHome/play" compile

echo "done"

#__EOF__
