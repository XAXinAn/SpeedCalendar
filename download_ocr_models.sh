#!/bin/bash

# PaddleOCR模型下载脚本
# 用途：下载PP-OCRv3中文模型并转换为Paddle Lite格式

set -e

echo "========================================="
echo " PaddleOCR模型下载与转换工具"
echo "========================================="

# 创建模型目录
MODELS_DIR="ocr_models"
mkdir -p $MODELS_DIR
cd $MODELS_DIR

echo ""
echo "📥 步骤1：下载PP-OCRv3中文模型..."
echo ""

# 检测模型
if [ ! -f "ch_PP-OCRv3_det_infer.tar" ]; then
    echo "下载检测模型..."
    wget -c https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_det_infer.tar
else
    echo "检测模型已存在，跳过下载"
fi

# 识别模型
if [ ! -f "ch_PP-OCRv3_rec_infer.tar" ]; then
    echo "下载识别模型..."
    wget -c https://paddleocr.bj.bcebos.com/PP-OCRv3/chinese/ch_PP-OCRv3_rec_infer.tar
else
    echo "识别模型已存在，跳过下载"
fi

# 方向分类模型
if [ ! -f "ch_ppocr_mobile_v2.0_cls_infer.tar" ]; then
    echo "下载方向分类模型..."
    wget -c https://paddleocr.bj.bcebos.com/dygraph_v2.0/ch/ch_ppocr_mobile_v2.0_cls_infer.tar
else
    echo "方向分类模型已存在，跳过下载"
fi

# 字典文件
if [ ! -f "ppocr_keys_v1.txt" ]; then
    echo "下载字典文件..."
    wget -c https://raw.githubusercontent.com/PaddlePaddle/PaddleOCR/release/2.6/ppocr/utils/ppocr_keys_v1.txt
else
    echo "字典文件已存在，跳过下载"
fi

echo ""
echo "📦 步骤2：解压模型文件..."
echo ""

tar -xf ch_PP-OCRv3_det_infer.tar
tar -xf ch_PP-OCRv3_rec_infer.tar
tar -xf ch_ppocr_mobile_v2.0_cls_infer.tar

echo ""
echo "🔧 步骤3：跳过模型转换（使用原始模型）..."
echo ""
echo "注意：.nb格式需要Paddle Lite工具转换，但paddleocr4android库可以直接使用原始模型"
echo "如果使用方案一（paddleocr4android），只需要原始.pdmodel和.pdiparams文件"
echo "如果需要.nb文件，请从官方预转换模型下载或使用Linux环境转换"

echo ""
echo "📁 步骤4：整理文件结构..."
echo ""

# 创建Android assets目录结构
mkdir -p android_assets/models/{det,rec,cls}

# 复制原始模型文件（paddleocr4android可以直接使用）
cp ch_PP-OCRv3_det_infer/inference.pdmodel android_assets/models/det/
cp ch_PP-OCRv3_det_infer/inference.pdiparams android_assets/models/det/
cp ch_PP-OCRv3_rec_infer/inference.pdmodel android_assets/models/rec/
cp ch_PP-OCRv3_rec_infer/inference.pdiparams android_assets/models/rec/
cp ch_ppocr_mobile_v2.0_cls_infer/inference.pdmodel android_assets/models/cls/
cp ch_ppocr_mobile_v2.0_cls_infer/inference.pdiparams android_assets/models/cls/

# 复制字典文件
cp ppocr_keys_v1.txt android_assets/

echo ""
echo "✅ 完成！文件已整理到 android_assets 目录"
echo ""
echo "========================================="
echo " 文件清单"
echo "========================================="
echo ""
echo "📂 原始模型格式（paddleocr4android库使用）："
echo "   - android_assets/models/det/inference.pdmodel"
echo "   - android_assets/models/det/inference.pdiparams"
echo "   - android_assets/models/rec/inference.pdmodel"
echo "   - android_assets/models/rec/inference.pdiparams"
echo "   - android_assets/models/cls/inference.pdmodel"
echo "   - android_assets/models/cls/inference.pdiparams"
echo ""
echo "📂 字典文件："
echo "   - android_assets/ppocr_keys_v1.txt"
echo ""
echo "========================================="
echo " 下一步操作"
echo "========================================="
echo ""
echo "1. 将 android_assets 目录中的文件复制到你的Android项目："
echo "   cp -r android_assets/* ../app/src/main/assets/"
echo ""
echo "2. 或者手动复制到："
echo "   SpeedCalendar/app/src/main/assets/"
echo ""
echo "3. 在 app/build.gradle 中添加依赖："
echo "   implementation 'io.github.mymonstercat:paddleocr4android:2.0.0'"
echo ""
echo "4. 查看详细集成步骤："
echo "   cat ../PaddleOCR_Android集成指南.md"
echo ""
echo "========================================="
echo " 模型信息"
echo "========================================="
echo ""
echo "模型版本：PP-OCRv3"
echo "语言：中文+英文+数字"
echo "总大小：$(du -sh android_assets | cut -f1)"
echo ""

# 显示各个文件大小
echo "详细大小："
du -h android_assets/models/det/inference.pdmodel 2>/dev/null | sed 's/^/  - /'
du -h android_assets/models/rec/inference.pdmodel 2>/dev/null | sed 's/^/  - /'
du -h android_assets/models/cls/inference.pdmodel 2>/dev/null | sed 's/^/  - /'

echo ""
echo "✨ 下载完成！"
