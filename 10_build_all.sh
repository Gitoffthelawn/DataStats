#!/bin/bash

################################################################################
# 色定義
################################################################################

# ANSI カラーコード
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
MAGENTA='\033[0;35m'
CYAN='\033[0;36m'
WHITE='\033[0;37m'
BOLD='\033[1m'
NC='\033[0m' # No Color

################################################################################
# ビルドの種類を選択
################################################################################

# ビルドオプションの定義
# | で区切って、1列目はキー、2列目は説明、3列目はgradleタスク
BUILD_OPTIONS=(
    "1|APK のみ|clean publishRelease"
    "2|APK + AAB|clean publishRelease bundlePublishRelease"
)

# メニュー表示関数
show_menu() {
    echo -e "${MAGENTA}${BOLD}----------------------------------------------------------${NC}"
    echo -e "${MAGENTA}${BOLD}ビルドする種類を選択してください:${NC}"
    for option in "${BUILD_OPTIONS[@]}"; do
        IFS='|' read -r key description _ <<< "$option"
        echo -e "${GREEN}${BOLD}$key.${NC} ${WHITE}$description${NC}"
    done
    echo -e "${MAGENTA}${BOLD}----------------------------------------------------------${NC}"
}

# ビルドオプション検索関数
find_build_option() {
    local target_key="$1"
    for option in "${BUILD_OPTIONS[@]}"; do
        IFS='|' read -r key description gradle_tasks <<< "$option"
        if [[ "$key" == "$target_key" ]]; then
            GRADLE_TASKS="$gradle_tasks"
            return 0
        fi
    done
    return 1
}

show_menu

# ビルドタイプの入力
echo -ne "${CYAN}${BOLD}選択 [1-2] (デフォルト: 1): ${NC}"
read build_type
build_type=${build_type:-1}

# ビルドオプションの検索
if ! find_build_option "$build_type"; then
    echo -e "${RED}${BOLD}無効な選択です: $build_type${NC}"
    echo -e "${YELLOW}利用可能なビルドタイプ: 1-2${NC}"
    exit 1
fi

################################################################################
# ビルド
################################################################################

echo -e "${GREEN}${BOLD}ビルドを開始します...${NC}"
echo -e "${CYAN}GRADLE_TASKS: ${WHITE}$GRADLE_TASKS${NC}"
./gradlew $GRADLE_TASKS

if [ $? -eq 0 ]; then
    echo -e "${GREEN}${BOLD}ビルドが成功しました。${NC}"
    afplay /System/Library/Sounds/Glass.aiff &
else
    echo -e "${RED}${BOLD}ビルドが失敗しました。${NC}"
    afplay /System/Library/Sounds/Basso.aiff &
    exit 1
fi