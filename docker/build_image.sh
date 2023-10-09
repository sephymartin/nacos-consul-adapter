#!/bin/bash

# 命令参考 http://www.ruanyifeng.com/blog/2017/11/bash-set.html
# e 只要发生错误，就终止执行, 有一个例外情况，就是不适用于管道命令
# u 执行脚本的时候，如果遇到不存在的变量，Bash 默认忽略它。
# x 用来在运行结果之前，先输出执行的那一行命令
# o pipefail 用来解决这种情况，只要一个子命令失败，整个管道命令就失败，脚本就会终止执行
set -euxo pipefail

PACKAGE_MODE=local

# 处理脚本参数
# -m 打包模式
while getopts "m:" opt_name # 通过循环，使用 getopts，按照指定参数列表进行解析，参数名存入 opt_name
do
    case "$opt_name" in # 根据参数名判断处理分支
        'm') # -m
            PACKAGE_MODE="$OPTARG" # 从 $OPTARG 中获取参数值
            ;;
#        'p') # -p
#            CONN_PASSWORD="$OPTARG"
#            ;;
#        'v') # -v
#            CONN_SHOW_DETAIL=true
#            ;;
#        'n') # -n
#            CONN_PORT="$OPTARG"
#            ;;
        ?) # 其它未指定名称参数
            echo "Unknown argument(s)."
            exit 2
            ;;
    esac
done

CURRENT_DIR=$(
    cd $(dirname $0)
    pwd
)

PROJECT_DIR=$(
    cd $(dirname $0)
    cd ../
    pwd
)

PROJECT_NAME=$(basename "$PWD")

echo "PROJECT_DIR: $PROJECT_DIR"
echo "PROJECT_NAME: $PROJECT_NAME"

MVND_HOME=/usr/local/maven-mvnd-1.0-m6-m40-linux-amd64
JAVA_HOME=/usr/lib/jvm/java-11-openjdk
USER_HOME=/app
M2_HOME=$USER_HOME/.m2
M2_SETTINGS=$M2_HOME/settings.xml

if [[ -f "$M2_SETTINGS" ]]; then
    echo "$M2_SETTINGS exists."
else
    mkdir -p $M2_HOME
    cp $CURRENT_DIR/settings.xml $M2_SETTINGS
fi

if [[ "$PACKAGE_MODE" == "local" ]]; then
    echo "package mode: local"
    mvn clean package -Dmaven.test.skip=true
elif [[ "$PACKAGE_MODE" == "mvnd" ]]; then
    echo "package mode: mvnd"
    $MVND_HOME/bin/mvnd clean package -am -U -Dmaven.test.skip=true -s $M2_SETTINGS -f $PROJECT_DIR/pom.xml -Djava.home=$JAVA_HOME -Duser.home=$USER_HOME
elif [[ "$PACKAGE_MODE" == "docker" ]]; then
    echo "package mode: docker"
    docker run --rm \
      -v $PROJECT_DIR:/app/$PROJECT_NAME \
      -v $M2_HOME:/root/.m2 \
      -w /app/$PROJECT_NAME maven:3.9.3-eclipse-temurin-8 mvn \
      -Duser.home=$HOME clean package -am -U  -Dmaven.test.skip=true

else
    echo "package mode: unknown"
    exit 1
fi

cd $PROJECT_DIR

MVN_VERSION=1.0.0-SNAPSHOT

JAR_FILE=$PROJECT_NAME.jar

cp -f $PROJECT_DIR/target/$JAR_FILE $CURRENT_DIR/app.jar

cd $CURRENT_DIR

docker build --build-arg JAR_FILE="app.jar" -t ${PROJECT_NAME}:${MVN_VERSION} .
