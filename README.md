<h1 align="center">
  <img src="assets/Melox-icon.png" alt="Melox 应用图标" width="96"><br>
  Melox
</h1>

<p align="center">
  <b>一款基于 [Miuix](https://github.com/compose-miuix-ui/miuix) 的 Android 本地音乐播放器</b>
</p>

<p align="center">
  <a href="README.en.md"><img src="https://img.shields.io/badge/Document-English-3478F6?style=flat" alt="English"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84?style=flat&logo=android&logoColor=white" alt="Android 9+">
  <a href="https://github.com/compose-miuix-ui/miuix"><img src="https://img.shields.io/badge/Miuix-0.9.3-4F6BED?style=flat" alt="Miuix 0.9.3"></a>
  <a href="https://developer.android.com/media/media3"><img src="https://img.shields.io/badge/Media3-1.11.0-4F6BED?style=flat" alt="AndroidX Media3 1.11.0"></a>
</p>

---

## 项目简介

Melox 是一款基于 Jetpack Compose、Miuix 和 AndroidX Media3 构建的 Android 本地音乐播放器。
## 功能特性

### 音乐库与首页

- 支持扫描系统媒体库，也支持把扫描范围限制到指定文件夹
- 歌曲、专辑、艺术家和文件夹页面都支持搜索、排序与字母索引
- 专辑和艺术家有独立详情页，点击列表项即可从当前页面队列开始播放
- 首页保留随机推荐和最近添加，适合从本地曲库中快速找到还没想起的音乐
- 可以从 Android 的“打开方式”或分享菜单接收一个 `audio/*` 文件

### 播放、队列与恢复

- 迷你播放器与全屏播放器共享当前曲目、封面和播放状态
- 支持拖动进度、上一首、下一首，以及顺序播放、单曲循环和随机队列
- 队列可以打开、清空、移除单首歌曲，也可以把歌曲加入下一首播放或当前队列
- 应用会保存队列、当前曲目和播放进度；重新打开或进程重启后恢复，但不会擅自自动播放
- 外部音频文件通过系统入口打开后，可以直接交给 Melox 播放

### 本地歌词与曲目信息

- 支持逐字/逐词时间轴、翻译行、字号和字重调整
- 可以选择歌词对齐方式、非当前行模糊，以及是否在歌词页隐藏播放控制
- 歌曲信息页展示标题、艺术家、专辑、格式、码率、采样率、位深、时长和文件位置
- 安装了 Music Tag Editor 或 Lyrico 时，可以从歌曲操作跳转编辑；返回 Melox 后只刷新这首歌的本地信息，不重建播放队列

### 播放页与界面设置

- 跟随系统、浅色和深色主题
- 基于当前封面或系统壁纸的动态配色
- 模糊封面、取色流动、浮动导航和液态玻璃效果
- 预测返回手势、可选默认首页、扫描刷新策略和文件夹范围设置
- 简体中文、English 与跟随系统语言，可在应用内切换

## 运行要求

- Android 9（API 28）或更高版本
- 当前 APK 构建目标为 `arm64-v8a`
- 首次扫描时需要授予本地音乐读取权限

液态玻璃等运行时视觉效果需要 Android 13+
## 下载与安装

从 [Releases](../../releases) 下载适用于设备的 APK。安装后打开 Melox，授予权限并进入“扫描音乐”建立或刷新音乐库。

当前版本刻意保持离线，不提供在线音乐、网络歌词、云同步、投屏、下载、收藏或用户歌单等服务功能。

## 构建

需要 JDK 17 或更高版本，以及 `compileSdk 37` 对应的 Android SDK。

```bash
./gradlew :app:assembleDebug
```

Debug APK 输出到 `app/build/outputs/apk/debug/app-debug.apk`。开发和提交修改前，建议再运行：

```bash
./gradlew :app:testDebugUnitTest :app:lintDebug
```

Release 签名使用本机的 `local.properties`，密钥和密码不属于仓库内容，也不应提交到 Git。

## 致谢

- [Miuix](https://github.com/compose-miuix-ui/miuix) - UI 组件与设计体系
- [AndroidX Media3](https://developer.android.com/media/media3) - 本地媒体播放与系统媒体会话

Melox 仍在持续开发中。