
#!/bin/bash

# 设置项目根目录
PROJECT_ROOT=$(cd "$(dirname "$0")" && pwd)

# 进入项目根目录
cd "$PROJECT_ROOT"

# 检查Java是否安装
if ! command -v java &> /dev/null
then
    echo "错误: 未找到Java，请确保Java已安装并配置了JAVA_HOME"
    exit 1
fi

# 使用Maven Wrapper编译并运行
echo "正在编译项目..."
./mvnw clean compile

if [ $? -ne 0 ]; then
    echo "编译失败，请检查错误信息"
    exit 1
fi

echo "编译成功，正在运行M3模拟客户端..."
./mvnw exec:java -Dexec.mainClass="com.example.demo.M3StandaloneApplication"
