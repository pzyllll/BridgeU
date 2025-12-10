#!/bin/bash

echo "正在创建 MySQL 数据库 bridgeu..."
echo ""
read -sp "请输入 MySQL root 密码（默认：123456）: " password
password=${password:-123456}
echo ""

mysql -u root -p"$password" -e "CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"

if [ $? -eq 0 ]; then
    echo ""
    echo "数据库 bridgeu 创建成功！"
    echo ""
    echo "验证数据库："
    mysql -u root -p"$password" -e "SHOW DATABASES LIKE 'bridgeu';"
else
    echo ""
    echo "创建数据库失败，请检查："
    echo "1. MySQL 服务是否正在运行"
    echo "2. root 密码是否正确"
    echo ""
    echo "手动创建数据库的 SQL："
    echo "CREATE DATABASE IF NOT EXISTS bridgeu CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
fi

