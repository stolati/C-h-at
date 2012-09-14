#!/usr/bin/env sh
#to load this : 
#wget -O - https://github.com/stolati/C-h-at/raw/master/other/amazon_EC2.sh | sudo sh
set -eux

step_done(){ echo "step [$1] completed" ; }

home="$PWD"

apt-get update
apt-get upgrade -y
apt-get install -y git unzip gzip curl

step_done "apt-get"


mkdir -p "$home/bin" "$home/dl"
chmod a+rx "$home/bin" "$home/dl"

#oracle java install
jre_tar_name="jre-7u7-linux-x64"
jdk_tar_name="jdk-7u7-linux-x64"

jre_bin_path="jre1.7.0_07"
jdk_bin_path="jdk1.7.0_07"

java_path="/usr/local/java"

mkdir -p "$java_path"
cd "$home/dl"

wget --no-cookies --header "Cookie:gpw_e24=http%3A%2F%2Fwww.oracle.com" "http://download.oracle.com/otn-pub/java/jdk/7u7-b10/$jre_tar_name.tar.gz"
wget --no-cookies --header "Cookie:gpw_e24=http%3A%2F%2Fwww.oracle.com" "http://download.oracle.com/otn-pub/java/jdk/7u7-b10/$jdk_tar_name.tar.gz"

step_done "Download java jdk and jre"

chmod a+x "$jre_tar_name.tar.gz"
chmod a+x "$jdk_tar_name.tar.gz"

cat "$jre_tar_name.tar.gz" | (
  cd "$java_path"
  tar xvfz -
)

cat "$jdk_tar_name.tar.gz" | (
  cd "$java_path"
  tar xvfz -
)


update-alternatives --install "/usr/bin/java" "java" "$java_path/$jre_bin_path/bin/java" 1
update-alternatives --install "/usr/bin/javac" "javac" "$java_path/$jdk_bin_path/bin/javac" 1
update-alternatives --install "/usr/bin/javaws" "javaws" "$java_path/$jdk_bin_path/bin/javaws" 1
update-alternatives --set java "$java_path/$jre_bin_path/bin/java"
update-alternatives --set javac "$java_path/$jdk_bin_path/bin/javac"
update-alternatives --set javaws "$java_path/$jdk_bin_path/bin/javaws"

step_done "Install Java jdk and jre"


#install play server
cd "$home/dl"
play_name="play-2.0.3"
play_path="/usr/local/share"

wget "http://download.playframework.org/releases/$play_name.zip"
step_done "Download Play server"

unzip -u "$play_name".zip -d "$play_path"
chmod -R a+rw "$play_path"

{
  echo "#!/usr/bin/env bash"
  echo "$play_path/$play_name/play "'"$@"'
  echo "#__EOF__"
} > "$home/bin/play"

chmod a+x "$home/bin/play"
sudo chmod -R a+rw /usr/local/share/play-2.0.3

step_done "Install Play server"

#install mongodb
cd "$home/dl"
mongo_name="mongodb-linux-x86_64-2.2.0"
mongo_path="/usr/local/share"
mongo_data="/data/db"

curl -O "http://fastdl.mongodb.org/linux/$mongo_name.tgz"
step_done "Download mongodb"

cat "$home/dl/$mongo_name.tgz" | (
  cd "$mongo_path"
  tar xvfz -
)

sudo mkdir -p "$mongo_data" #is inside the free 8Go
sudo chmod -R 777 "$mongo_data"

{
  echo "#!/usr/bin/env bash"
  echo "$mongo_path/$mongo_name/bin/mongod "'"$@"'
  echo "#__EOF__"
} > "$home/bin/mongod"

{
  echo "#!/usr/bin/env bash"
  echo "$mongo_path/$mongo_name/bin/mongo "'"$@"'
  echo "#__EOF__"
} > "$home/bin/mongo"


{
  echo "#!/usr/bin/env bash"
  echo "$mongo_path/$mongo_name/bin/mongod --fork --logpath \"$home/mongod.log\" --dbpath \"$mongo_data\""
  echo "#__EOF__"
} > "$home/bin/mongod_fork"

chmod a+x "$home/bin/mongod" "$home/bin/mongo" "$home/bin/mongod_fork"


step_done "Install mongodb"

cd "$home"
git clone git://github.com/stolati/C-h-at.git

step_done "Get C-h-at project "

cd "$home/C-h-at"

"$home/bin/play" clean compile stage

step_done "Compile C-h-at project "

"$home/bin/mongo_fork"
"$home/C-h-at/target/start" -Dhttp.port=80

step_done "Server launched"

#__EOF__
