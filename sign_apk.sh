#!/bin/bash

# APK签名脚本 / APK Signing Script  
# 用于为 fcitx5-android 生成的 APK 文件签名

set -e

# 颜色输出
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}fcitx5-android APK 签名工具${NC}"
echo -e "${BLUE}APK Signing Tool for fcitx5-android${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# 配置参数
KEYSTORE_FILE="fcitx5-android.keystore"
KEY_ALIAS="fcitx5-android-key"
KEYSTORE_PASSWORD="fcitx5android"
KEY_PASSWORD="fcitx5android"
VALIDITY_DAYS=10000

# 查找 build-tools 路径
BUILD_TOOLS_DIR=""
if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/build-tools" ]; then
    BUILD_TOOLS_DIR=$(ls -d $ANDROID_HOME/build-tools/* | sort -V | tail -1)
elif [ -d "/home/aitest/android-studio/build-tools" ]; then
    BUILD_TOOLS_DIR=$(ls -d /home/aitest/android-studio/build-tools/* | sort -V | tail -1)
fi

if [ -z "$BUILD_TOOLS_DIR" ]; then
    echo -e "${RED}错误: 找不到 Android build-tools${NC}"
    echo -e "${RED}Error: Android build-tools not found${NC}"
    exit 1
fi

APKSIGNER="$BUILD_TOOLS_DIR/apksigner"
ZIPALIGN="$BUILD_TOOLS_DIR/zipalign"

if [ ! -f "$APKSIGNER" ] || [ ! -f "$ZIPALIGN" ]; then
    echo -e "${RED}错误: 找不到 apksigner 或 zipalign${NC}"
    echo -e "${RED}Error: apksigner or zipalign not found${NC}"
    exit 1
fi

echo -e "${GREEN}找到 build-tools: $BUILD_TOOLS_DIR${NC}"
echo ""

# 创建 keystore（如果不存在）
if [ ! -f "$KEYSTORE_FILE" ]; then
    echo -e "${YELLOW}创建新的密钥库...${NC}"
    echo -e "${YELLOW}Creating new keystore...${NC}"
    
    keytool -genkey -v \
        -keystore "$KEYSTORE_FILE" \
        -alias "$KEY_ALIAS" \
        -keyalg RSA \
        -keysize 2048 \
        -validity $VALIDITY_DAYS \
        -storepass "$KEYSTORE_PASSWORD" \
        -keypass "$KEY_PASSWORD" \
        -dname "CN=fcitx5-android, OU=Development, O=fcitx5, L=Unknown, S=Unknown, C=CN"
    
    echo -e "${GREEN}✓ 密钥库创建成功: $KEYSTORE_FILE${NC}"
    echo -e "${GREEN}✓ Keystore created: $KEYSTORE_FILE${NC}"
    echo ""
else
    echo -e "${GREEN}✓ 使用现有密钥库: $KEYSTORE_FILE${NC}"
    echo -e "${GREEN}✓ Using existing keystore: $KEYSTORE_FILE${NC}"
    echo ""
fi

# 查找所有未签名的 APK
APK_DIR="app/build/outputs/apk/release"
if [ ! -d "$APK_DIR" ]; then
    echo -e "${RED}错误: 找不到 APK 输出目录${NC}"
    echo -e "${RED}Error: APK output directory not found${NC}"
    exit 1
fi

UNSIGNED_APKS=$(find "$APK_DIR" -name "*-unsigned.apk" 2>/dev/null)

if [ -z "$UNSIGNED_APKS" ]; then
    echo -e "${YELLOW}未找到未签名的 APK 文件${NC}"
    echo -e "${YELLOW}No unsigned APK files found${NC}"
    echo ""
    echo -e "${BLUE}请先构建 APK:${NC}"
    echo -e "${BLUE}Please build APK first:${NC}"
    echo "  ./gradlew assembleRelease"
    exit 1
fi

# 签名每个 APK
echo -e "${BLUE}开始签名 APK 文件...${NC}"
echo -e "${BLUE}Starting APK signing...${NC}"
echo ""

SIGNED_COUNT=0
for UNSIGNED_APK in $UNSIGNED_APKS; do
    # 生成签名后的文件名
    SIGNED_APK="${UNSIGNED_APK%-unsigned.apk}-signed.apk"
    ALIGNED_APK="${UNSIGNED_APK%-unsigned.apk}-aligned.apk"
    
    echo -e "${YELLOW}处理: $(basename $UNSIGNED_APK)${NC}"
    echo -e "${YELLOW}Processing: $(basename $UNSIGNED_APK)${NC}"
    
    # 对齐 APK
    echo "  [1/3] 对齐 APK / Aligning APK..."
    if ! "$ZIPALIGN" -v -p 4 "$UNSIGNED_APK" "$ALIGNED_APK" > /dev/null 2>&1; then
        echo -e "${RED}  ✗ 对齐失败，跳过 / Alignment failed, skipping${NC}"
        continue
    fi
    
    # 签名 APK
    echo "  [2/3] 签名 APK / Signing APK..."
    if ! "$APKSIGNER" sign \
        --ks "$KEYSTORE_FILE" \
        --ks-key-alias "$KEY_ALIAS" \
        --ks-pass pass:"$KEYSTORE_PASSWORD" \
        --key-pass pass:"$KEY_PASSWORD" \
        --out "$SIGNED_APK" \
        "$ALIGNED_APK" 2>&1; then
        echo -e "${RED}  ✗ 签名失败 / Signing failed${NC}"
        rm -f "$ALIGNED_APK"
        continue
    fi
    
    # 验证签名（允许警告）
    echo "  [3/3] 验证签名 / Verifying signature..."
    VERIFY_OUTPUT=$("$APKSIGNER" verify "$SIGNED_APK" 2>&1)
    VERIFY_STATUS=$?
    
    # 检查是否有错误（不是警告）
    if [ $VERIFY_STATUS -ne 0 ] || echo "$VERIFY_OUTPUT" | grep -q "^ERROR:"; then
        echo -e "${RED}  ✗ 签名验证失败 / Signature verification failed${NC}"
        echo "$VERIFY_OUTPUT" | head -5
        rm -f "$ALIGNED_APK" "$SIGNED_APK"
        continue
    fi
    
    # 清理临时文件
    rm -f "$ALIGNED_APK"
    
    # 显示文件信息
    SIZE=$(du -h "$SIGNED_APK" | cut -f1)
    echo -e "${GREEN}  ✓ 签名成功: $(basename $SIGNED_APK) ($SIZE)${NC}"
    echo -e "${GREEN}  ✓ Signed successfully: $(basename $SIGNED_APK) ($SIZE)${NC}"
    
    # 如果有警告，显示警告数量
    WARNING_COUNT=$(echo "$VERIFY_OUTPUT" | grep -c "^WARNING:" || true)
    if [ $WARNING_COUNT -gt 0 ]; then
        echo -e "${YELLOW}  ⚠  $WARNING_COUNT 个警告 (可忽略) / $WARNING_COUNT warnings (safe to ignore)${NC}"
    fi
    echo ""
    
    SIGNED_COUNT=$((SIGNED_COUNT + 1))
done

# 完成总结
echo -e "${BLUE}========================================${NC}"
if [ $SIGNED_COUNT -gt 0 ]; then
    echo -e "${GREEN}✓ 签名完成! 成功签名 $SIGNED_COUNT 个 APK${NC}"
    echo -e "${GREEN}✓ Signing complete! Successfully signed $SIGNED_COUNT APK(s)${NC}"
    echo ""
    echo -e "${BLUE}签名的 APK 位置:${NC}"
    echo -e "${BLUE}Signed APK location:${NC}"
    find "$APK_DIR" -name "*-signed.apk" -exec ls -lh {} \; | awk '{printf "  %s  %s\n", $5, $9}'
    echo ""
    echo -e "${BLUE}密钥库信息:${NC}"
    echo -e "${BLUE}Keystore information:${NC}"
    echo "  文件 / File: $KEYSTORE_FILE"
    echo "  别名 / Alias: $KEY_ALIAS"
    echo "  密码 / Password: $KEYSTORE_PASSWORD"
    echo ""
    echo -e "${YELLOW}⚠️  请妥善保管密钥库文件!${NC}"
    echo -e "${YELLOW}⚠️  Please keep the keystore file safe!${NC}"
    echo ""
    echo -e "${GREEN}现在可以安装 APK:${NC}"
    echo -e "${GREEN}Now you can install the APK:${NC}"
    echo "  adb install app/build/outputs/apk/release/*-signed.apk"
else
    echo -e "${RED}✗ 未签名任何 APK${NC}"
    echo -e "${RED}✗ No APKs were signed${NC}"
fi
echo -e "${BLUE}========================================${NC}"
