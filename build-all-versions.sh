#!/bin/bash

# 各版本配置信息
# 格式: 版本号|yarn_mappings|fabric_api|loom_version|loader_version

VERSIONS=(
    "1.21.2|1.21.2+build.1|0.106.1+1.21.2|1.10|0.16.7"
    "1.21.3|1.21.3+build.2|0.114.1+1.21.3|1.10|0.16.7"
    "1.21.4|1.21.4+build.4|0.111.0+1.21.4|1.10|0.16.7"
    "1.21.5|1.21.5+build.1|0.115.0+1.21.5|1.10|0.16.7"
    "1.21.6|1.21.6+build.1|0.127.0+1.21.6|1.10|0.16.14"
    "1.21.7|1.21.7+build.1|0.129.0+1.21.7|1.10|0.16.14"
    "1.21.8|1.21.8+build.1|0.129.0+1.21.8|1.10|0.16.14"
    "1.21.9|1.21.9+build.1|0.130.0+1.21.9|1.11|0.16.14"
    "1.21.10|1.21.10+build.2|0.131.0+1.21.10|1.11|0.16.14"
    "1.21.11|1.21.11+build.1|0.132.0+1.21.11|1.11|0.16.14"
)

# 创建输出目录
OUTPUT_DIR="build/multi-version"
mkdir -p "$OUTPUT_DIR"

# 原始 gradle.properties 内容备份
ORIGINAL_PROPS="gradle.properties.original"
cp gradle.properties "$ORIGINAL_PROPS"

# Loom 版本对应的 build.gradle 插件版本
declare -A LOOM_PLUGIN_VERSIONS
LOOM_PLUGIN_VERSIONS["1.8"]="1.8.13"
LOOM_PLUGIN_VERSIONS["1.10"]="1.10.5"
LOOM_PLUGIN_VERSIONS["1.11"]="1.11.8"

# 清理函数
cleanup_locks() {
    ./gradlew --stop 2>/dev/null || true
    rm -rf ~/.gradle/caches/fabric-loom/*.lock 2>/dev/null || true
    rm -rf ~/.gradle/caches/journal-1/file-access.lock 2>/dev/null || true
    rm -rf .gradle/loom-cache/*.lock 2>/dev/null || true
}

# 编译每个版本
PREV_LOOM_VERSION=""
for VERSION_CONFIG in "${VERSIONS[@]}"; do
    IFS='|' read -r MC_VERSION YARN_MAPPINGS FABRIC_API LOOM_VERSION LOADER_VERSION <<< "$VERSION_CONFIG"
    
    echo "========================================"
    echo "编译版本: $MC_VERSION"
    echo "Yarn: $YARN_MAPPINGS"
    echo "Fabric API: $FABRIC_API"
    echo "Loom: $LOOM_VERSION"
    echo "Loader: $LOADER_VERSION"
    echo "========================================"
    
    # 如果 Loom 版本发生变化，清理缓存
    if [ "$PREV_LOOM_VERSION" != "" ] && [ "$PREV_LOOM_VERSION" != "$LOOM_VERSION" ]; then
        echo "Loom 版本变化: $PREV_LOOM_VERSION -> $LOOM_VERSION，清理缓存..."
        ./gradlew --stop 2>/dev/null || true
        rm -rf ~/.gradle/caches/fabric-loom 2>/dev/null || true
        rm -rf .gradle/loom-cache 2>/dev/null || true
    fi
    PREV_LOOM_VERSION=$LOOM_VERSION
    
    # 清理锁文件
    cleanup_locks
    
    # 确保 gradle.properties 包含所有必要属性
    if ! grep -q "mod_version=" gradle.properties; then
        echo "mod_version=1.2.0" >> gradle.properties
    fi
    if ! grep -q "maven_group=" gradle.properties; then
        echo "maven_group=betterbundle" >> gradle.properties
    fi
    if ! grep -q "archives_base_name=" gradle.properties; then
        echo "archives_base_name=better-bundle" >> gradle.properties
    fi
    
    # 更新 gradle.properties
    sed -i \
        -e "s/minecraft_version=.*/minecraft_version=$MC_VERSION/" \
        -e "s/yarn_mappings=.*/yarn_mappings=$YARN_MAPPINGS/" \
        -e "s/loader_version=.*/loader_version=$LOADER_VERSION/" \
        -e "s/loom_version=.*/loom_version=$LOOM_VERSION/" \
        -e "s/fabric_api_version=.*/fabric_api_version=$FABRIC_API/" \
        gradle.properties
    
    # 更新 build.gradle 中的 loom 版本
    LOOM_PLUGIN="${LOOM_PLUGIN_VERSIONS[$LOOM_VERSION]}"
    sed -i "s/id 'fabric-loom' version '[^']*'/id 'fabric-loom' version '$LOOM_PLUGIN'/g" build.gradle
    
    # 清理 build 目录
    rm -rf build/libs build/devlibs
    
    # 编译
    ./gradlew build --no-build-cache
    
    # 检查编译结果
    if [ -f "build/libs/better-bundle-1.2.0.jar" ]; then
        JAR_SIZE=$(stat -c%s "build/libs/better-bundle-1.2.0.jar")
        if [ "$JAR_SIZE" -gt 1000 ]; then
            # 复制并重命名 JAR 文件
            cp "build/libs/better-bundle-1.2.0.jar" "$OUTPUT_DIR/better-bundle-1.2.0+$MC_VERSION.jar"
            echo "✅ 成功: $OUTPUT_DIR/better-bundle-1.2.0+$MC_VERSION.jar ($JAR_SIZE bytes)"
        else
            echo "❌ 失败: 版本 $MC_VERSION JAR 文件太小 ($JAR_SIZE bytes)"
        fi
    else
        echo "❌ 失败: 版本 $MC_VERSION 编译失败"
    fi
    
    # 再次清理锁文件
    cleanup_locks
done

# 恢复原始配置
cp "$ORIGINAL_PROPS" gradle.properties
rm "$ORIGINAL_PROPS"

echo "========================================"
echo "所有版本编译完成"
echo "输出目录: $OUTPUT_DIR"
ls -la "$OUTPUT_DIR"
echo "========================================"