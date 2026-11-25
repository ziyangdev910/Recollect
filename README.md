# 拾知 / Recollect

拾知是一款面向初高中学生的 Android 原生纸数融合复习工具：手机录入题目与知识点，
按每周到期情况生成可打印的 A4 复习计划；在纸上完成填涂后拍照回读，经人工确认更新
复习状态，再生成下一份计划。调度单元是知识点，题目是复习载体。

## 功能

- 题目与知识点的录入、分类、标签管理与检索
- 按知识点到期情况生成可打印的 A4 周计划（含 PDF 分享）
- 纸质填涂后拍照回读：ArUco 定位、逐题 QR、OMR 填涂识别与 ML Kit 中文 OCR
- 回读结果逐题人工确认后更新复习状态
- 复习进度统计、数据库导入与导出
- 全程离线运行，识别与计划生成均在设备本地完成

## 技术栈

Kotlin + Jetpack Compose，Room，CameraX，OpenCV，ML Kit，ZXing。minSdk 26。

## 构建

```bash
./gradlew assembleDebug
./gradlew assembleRelease
```

Windows 下也可直接运行 `debug_compile_and_install.bat`（构建 debug APK 并安装到
实机）或 `release_compile_and_install.bat`（构建开启 R8 的 release APK 并安装）。
release 签名读取仓库外 `keystore.properties` 指向的正式 keystore，缺失时回退 debug
签名。

## 算法来源

本项目的记忆模型（DHP）公式参考了墨墨团队以 MIT License 开源的
[SSP-MMC](https://github.com/maimemo/SSP-MMC) 实现；算法设计同时受
[SSP-MMC-Plus](https://github.com/maimemo/SSP-MMC-Plus) 与相关论文启发。
排程与工程代码为独立编写。相关版权与 MIT 许可文本见 [NOTICE](NOTICE)。

## 开源许可证

本项目采用 **GNU General Public License v3.0（仅此版本）并附加 Classpath
Exception 2.0** 发布：

```
SPDX-License-Identifier: GPL-3.0-only WITH Classpath-exception-2.0
```

GPL-3.0 全文见 [LICENSE](LICENSE)，Classpath 例外全文见
[LICENSE-EXCEPTION](LICENSE-EXCEPTION)。

你可以自由使用、修改和分发本项目的源代码，但任何分发本应用或其衍生版本的行为，
必须同样以 GPL-3.0 公开对应源代码；**闭源分发（包括将衍生版本闭源上架应用商店）
违反本许可证**。分发二进制（APK）时，请按 GPL-3.0 第 6 节提供完整对应源代码
（即本仓库及构建所需文件）。

Classpath 例外允许将本项目与独立模块（例如 ML Kit 等闭源依赖）链接并一同分发，
不要求这些独立模块开源；它不改变本项目及衍生 App 自身代码的 GPL-3.0 开源义务。

本项目自身代码以 GPL-3.0 + Classpath Exception 发布；使用的第三方组件遵循各自的许可证，详见 [NOTICE](NOTICE)。
