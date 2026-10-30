# Xperia XZ1 Compact / SO-02K · LineageOS 22.2

**为小屏 Xperia 延续日常使用、音乐播放与跨设备连接体验。**

[中文](#项目概况) · [English](#english)

这是由 Yal3AY 维护、面向日版 Sony Xperia XZ1 Compact SO-02K 的非官方 Android 15 定制系统。基于 LineageOS 22.2，在系统管家、索尼音频、Apple 设备联动、翻译与文件阅读等方面持续完善，让这台小尺寸手机拥有更实用的现代系统体验。

[下载系统](https://github.com/yal3ay/xz1c-lineageos-22/releases) · [查看更新记录](https://github.com/yal3ay/xz1c-lineageos-22/releases) · [反馈问题](https://github.com/yal3ay/xz1c-lineageos-22/issues) · [安全问题](SECURITY.md)

> 适配与验证以 **SO-02K** 为目标；其他 XZ1 Compact 变体未验证。本项目不是 Sony 或 LineageOS 官方发行版。以下介绍核对于 **2026-10-30**，具体可用功能以所选版本的发行说明为准。

## 功能一览

> 以下汇总开发版中的功能；默认更新通道版本较旧，实际功能请以所安装版本的发行说明为准。

| 想做什么 | 系统提供的功能 |
| --- | --- |
| 管理手机 | **系统管家**查看内存、处理器和电池历史，管理后台、自启动、应用和缓存，批量预编译应用。 |
| 听音乐 | **高保真音频**查看实际输出设备、采样率和位深；兼容路径支持直通、索尼音效、耳机输出增强和蓝牙音质设置。 |
| 听音箱、看电视 | **AirPlay**把媒体声音发送到兼容音箱；向兼容的 Apple TV 镜像画面和声音，并用手机遥控、调音量和输入文字。 |
| 分享文件 | 系统分享页可在受支持的同一无线网络环境下向 Mac 发送文件，通过 LocalSend 与手机收发，或转交 Google 快速分享。 |
| 阅读与学习 | 选中文字即可打开翻译卡；英文单词可离线查词，收藏后用单词卡复习，并导出到 Anki。 |
| 查看文件 | **文档**应用在本机打开常见办公文档、图片和媒体，支持搜索、夜间模式和打印。 |
| 查看天气 | 锁屏显示农历、节气和 Xperia 天气，并可选择湿度、风力、紫外线和空气质量等信息。 |
| 日常操作 | 支持双击或音量键唤醒、短信验证码提示、存储卡作为内部存储，以及针对通话和触摸的设备适配。 |
| 更新与反馈 | 系统内检查更新、管理附加包；开发版可选择启用问题反馈，并在查看报告后决定是否提交。 |

**兼容范围：**音频效果受播放器和输出设备影响；AirPlay 和文件分享取决于接收设备及网络，尚不支持所有苹果设备的原生隔空投送。部分功能仅见于开发版。

## 项目概况

| 项目 | 内容 |
| --- | --- |
| 目标设备 | Sony Xperia XZ1 Compact / SO-02K |
| 设备代号 / 构建目标 | `lilac` / `lineage_lilac_dcm` |
| 系统基础 | Android 15 / LineageOS 22.2 |
| 内核基础 | Linux 4.4.302 |
| 当前系统安全补丁日期 | **2026-08-01**，不等同于安装包的发布日期 |
| 默认更新通道 | [22.2-20261010-r6](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261010-r6) |
| 开发者更新通道 | [22.2-20261030-r2](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r2)，内置近连（Nearbo）应用与最新修复，仍属预发布 |

默认通道和开发者通道更新节奏不同。想体验最新功能，可在系统更新设置中选择开发者更新；请先阅读对应版本的说明和验证范围。

## 主要功能

### 系统管家：常用管理集中在一处

统一的卡片界面提供内存与运行状态、加速、自启动管理、后台应用、垃圾清理、应用管理和应用预编译。预编译会提前优化应用代码，支持记住选择、批量操作，以及应用或系统更新后的重新处理。

电池页面保存跨充电周期的本机历史，提供24小时 / 7天曲线、亮屏时间与应用耗电估算。应用管理可查看大小和最近使用情况，并进行强制停止、清缓存或卸载。垃圾清理中的照片、视频、文档与大文件需要用户自行确认，避免把个人文件当作普通缓存处理。

“加速”可能停止正在运行或播放内容的应用，使用前请留意当前任务。

### 高保真音频：看清实际输出，按设备能力播放

内置音频控制页，集中显示输出设备、采样率、精度和实际音频路径，提供兼容路径下的无损直通、索尼音频增强、耳机输出增强与蓝牙音质选项。

- 有线耳机和兼容的外接音频设备可使用直通输出；是否按音源原格式输出，取决于播放器、音源和设备共同支持的能力。
- 提供 **DSEE HX（Digital Sound Enhancement Engine HX／索尼数字音质增强技术／对兼容有线输出路径中的压缩音乐进行增强处理）**。
- 可切换耳机输出增强；切换前先降低音量，出现失真时恢复原厂模式。
- 蓝牙页面显示当前编码与质量选项；蓝牙播放的编码、距离和稳定性存在取舍，不等同于有线无损直通。

不同播放器的音频路径可能不同，页面会尽量显示实际输出，不承诺所有应用都走同一种高规格路径。

### 近连（Nearbo）：投屏、音乐、遥控与局域网互传应用

开发者版本 **[22.2-20261030-r2](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r2)** 起，系统内置独立的 **近连（Nearbo）** 应用（`com.xz1c.appleconnect`，版本 **1.0.53**），把大屏投送、电视遥控与局域网文件分享整合在同一处：

- **屏幕镜像与原画投屏**：将手机画面与允许捕获的媒体／游戏声音镜像到同一无线网络下的兼容 Apple TV、Nearbo TV 及支持 AirPlay 的电视或投影仪；支持全屏或单应用投屏、画质与方向设置，以及将正在播放的视频原画推送到受支持的接收端。
- **音乐投送**：向兼容音箱或电视串流音乐，提供实时歌词、播放控制、独立音量与睡眠定时。
- **电视遥控器**：与 Nearbo TV 或兼容 Apple TV 配对后，可使用大尺寸触控板、播放与音量键、电视应用快捷启动及手机键盘文字输入。
- **近连分享（Nearbo Share）**：在同一无线网络下向附近的 Android 设备、Nearbo TV 或开放接收的兼容 Mac 点对点发送照片、视频与文件，文件在局域网内直传，不经过外部中转服务器。

应用以普通非特权系统应用预置，保留原有独立签名，支持直接覆盖升级与后续应用内更新；每次屏幕捕获均需经过 Android 系统授权确认。

### Apple 设备联动：音频投送、屏幕镜像与遥控

**AirPlay（Apple AirPlay／苹果的无线媒体投送技术／把手机媒体声音或画面发送到兼容设备）** 已整合到系统输出面板与快捷设置。

- 向兼容的 HomePod、Apple TV 等设备投送媒体声音，可从系统媒体输出面板选择设备、切换输出和调节音量。
- 向兼容的 Apple TV 投送手机画面与声音，提供配对、连接状态与停止镜像入口。
- 内置 Apple TV 遥控器，支持触控板、播放控制、音量与文字输入。
- 遥控器的锁屏媒体卡可展示设备提供的进度、时长与控制；没有时长的信息源不显示虚构进度。

音频投送与屏幕镜像是独立功能；接收设备、网络、访问设置及内容保护会影响可用性。单独的音频投送存在缓冲延迟，不等同于低延迟视频镜像。

### 分享：从系统分享菜单选择接收设备

系统分享菜单中的“AirDrop”入口提供邻近设备选择页面：

- 对在同一无线网络上开放接收的兼容 Mac，可使用项目实现的文件发送路径；Mac 需要允许来自所有人的接收。
- 手机之间可使用 LocalSend 方式收发，接收时显示确认与核对码。
- 已安装 Google 服务时，可交给 Google 快速分享继续发送。
- 设备搜索与接收仅在分享页打开期间运行；已经开始的传输可在切到后台后继续。

这是有明确设备和网络条件的分享实现，**不代表已经具备与所有 iPhone 或新版 Mac 的原生 AirDrop 互传能力**。请以发行说明和实际设备测试为准。

### 翻译与单词卡：从选中文字到复习

选中文字后可进入浮动翻译卡，查看译文与原文、翻面、复制、朗读，并收藏为单词卡。

- 常用英文单词可使用离线词典。
- 在线翻译可选择 Google 或自行配置 DeepSeek 服务；支持多种目标语言。
- 提供系统朗读及可选的在线语音配置。
- 单词卡支持复习提醒、记忆程度选择与导出到 Anki。

离线查词在本机完成；在线翻译和在线语音会把所选文字发送给相应服务。第三方服务的网络、凭据和可用性由用户按需要配置。当前主要入口是“翻译”；早期“大爆炸”入口已暂时停用。

### 文档阅读：本机打开常见文件

内置基于开源项目 Gander 的“文档”阅读器，可打开常见办公文档、电子文档、图片和媒体文件，并支持查找、夜间模式、打印或另存副本等操作。

该阅读器无需网络权限，文件在本机打开。具体格式的排版和兼容性受阅读组件限制，不能替代所有专业编辑软件。原作者与相关许可证可在应用“关于”中查看。

### Xperia 日常体验与系统工具

- 锁屏农历、节气、节日与 Xperia 天气信息，可选择显示天气、体感温度、湿度、风力、紫外线和空气质量等项目。
- 双击亮屏与音量键唤醒；已发布亮屏解锁后偶发触摸无响应的驱动修复。
- 短信验证码提示与自动填写。
- 通话与高清语音的相关开关、状态显示和设备适配。
- 支持将存储卡配置为内部存储，实际体验取决于卡片性能与可靠性。
- 内置 WireGuard、Shizuku 集成、KernelSU-Next 支持，以及 Google 服务状态检查和相关兼容设置。

Shizuku 向用户授权的兼容工具提供部分系统能力；KernelSU-Next 可提供 **Root（root access／超级用户权限／允许经授权的应用执行系统级操作）** 支持。两者的作用与授权方式不同。Google 认证与第三方应用兼容结果可能随服务端策略变化，不承诺所有应用始终可用。

### Bug 反馈：先查看，再决定是否提交

开发者版本 **22.2-20261026-r1** 已加入系统管家“Bug 反馈”：

- 自动收集与提醒默认关闭；用户开启后，仅在本机保存有限的错误类型、代码位置及版本等技术信息。
- 本机最多20份、有效期7天，过期文件在下次使用时清理；最多每天一次静默提醒。
- 可手动创建报告；每份报告先显示接收方和技术内容，再由用户确认提交给维护者。
- 不自动上传完整日志，不附带账号、聊天、照片或设备唯一标识；后台按30天期限清理报告。

本功能用于帮助维护者分析问题，不提供远程控制用户手机的能力。自动收集的首次启动行为等验证范围，请参阅该版本发行说明。

从 [22.2-20261030-r1](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r1) 起，手动反馈可选择应用并填写问题描述，报告附带所选应用的版本。“重现 Bug”可主动记录最多3分钟，附上可选的无声录屏，并尝试保存有限的播放诊断；无法取得的信息会明确标注。应用进程与共享媒体服务的记录分别标记，共享服务错误不能直接归因于所选应用。这项更新改进问题取证，**不代表已修复抖音花屏**。

## 正在测试与已知限制

| 功能或问题 | 当前状态 |
| --- | --- |
| 主动记录复现过程＋可选录屏 | 已包含在 [22.2-20261030-r1](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r1)；本轮通过编译与包内容检查，尚未进行新整包安装和录屏的真机验证 |
| 抖音“日常”图片配音乐转成的视频偶发花屏 | 等待同一条故障内容复现，**尚未确认根因或发布修复** |
| 跨设备分享 | 已有系统入口与受限环境的发送路径；完整原生互传仍在研发 |
| 部分旧功能 | 网络存储挂载、独立主题色入口、小羊助手及 AirPods 弹窗目前不作为已发布版本的可用功能介绍 |

主动记录功能中的视频可能包含个人信息；设计为单独授权、本机预览、可删除视频后仅提交技术信息，确认前不发送。它不是已经默认开启的后台录屏能力。

本项目持续维护，但仍有开发测试中的功能和设备组合。已编译、单项手机测试与整包更新后验证是不同的完成状态，不将它们混为一谈。

## 下载、安装与更新

下载请前往 [GitHub Releases](https://github.com/yal3ay/xz1c-lineageos-22/releases)。基础系统、配套恢复环境镜像和文件校验清单随对应版本提供；Sony 应用包和 **GApps（Google Apps／Google应用套件／按需要另装的Google服务与应用）** 分开管理，可从系统更新的附加包入口按版本选择。

系统内支持 **OTA（Over-the-Air／空中更新／在手机上下载并安装系统更新）**。当前采用完整更新包；“增量编译”不等于“增量更新包”。

- **已有本项目相同签名版本**：通常按系统更新流程保留数据升级；普通更新不要主动清除数据。
- **首次安装、跨项目或跨签名迁移**：先确认设备已解锁引导加载程序，再按对应发行说明使用匹配的恢复环境；可能需要清除数据，不保证能保留原有应用和登录状态。
- **使用内部存储模式的存储卡**：其加密密钥保存在手机数据分区，清除手机数据可能使卡内应用与文件无法读取。操作前应独立导出重要内容，并确认备份可恢复。
- **附加包**：使用与本项目版本和签名匹配的 Sony / Google 包，不混用旧平台签名组件。
- **下载核对**：使用该版本的 `SHA256SUMS`，例如运行 `shasum -a 256 -c SHA256SUMS`；不要用证书指纹代替下载文件摘要。

不同版本的安装与验证范围可能不同，**对应发行说明优先**。发布流程另见 [维护说明](OTA_RELEASE_WORKFLOW.md)。

## 源码与许可

这是一个基于开源系统与设备适配项目持续修改的非官方项目，源码入口包括：

- [设备配置：lilac](https://github.com/yal3ay/android_device_sony_lilac)
- [Yoshino 共用设备配置](https://github.com/yal3ay/android_device_sony_yoshino-common)
- [msm8998 内核源码](https://github.com/yal3ay/android_kernel_sony_msm8998)（GPL-2.0）
- [源码清单与专有文件提取说明](https://github.com/yal3ay/xz1c_local_manifests)
- [系统修改补丁](https://github.com/yal3ay/xz1c_lineageos_22_patches)
- [本仓库中的补丁](patches/)

Sony 专有文件和设备提取的二进制组件不随这些源码仓库公开分发，请从合法持有的设备或固件提取。各组件沿用自身许可证；本仓库许可见 [LICENSE](LICENSE)。不公开签名私钥或用户私密配置。

<details>
<summary>项目公开签名证书指纹</summary>

以下保留项目公开证书摘要，仅供签名身份核对。证书文件和私钥不包含在源码仓库中。

```
bluetooth     4E:E2:4D:89:CE:2D:60:E5:90:E4:A5:31:01:62:E5:2C:17:3F:E1:64:4F:06:BE:D8:DA:6E:C6:8D:38:FA:3B:CB
media         5B:B1:76:43:F9:D6:AC:78:67:0D:12:04:BC:38:94:7E:11:88:5D:72:69:B6:4C:8C:BB:86:15:6B:52:9E:B0:20
networkstack  24:D3:9D:61:27:AA:76:ED:E9:75:E4:59:93:EF:D5:5E:69:5F:C8:FD:52:78:83:AD:D9:26:A4:F3:B5:32:2A:5C
nfc           D9:76:2B:EC:5E:D2:76:65:99:D2:0A:FA:FF:D3:A3:BA:91:17:A2:C9:58:63:B1:3C:99:7F:DD:7A:D2:88:48:0B
platform      C0:4D:ED:72:8A:A8:4B:38:87:7A:DA:2E:A0:6F:A7:63:2E:82:FC:83:C1:AD:DF:20:6A:2B:39:2C:E5:4E:FD:E6
releasekey    91:62:F3:0F:5F:E7:4C:69:98:C6:C0:6B:91:20:DC:75:A0:AC:3F:14:0B:22:34:AA:0B:EF:D2:41:2D:FA:11:59
sdk_sandbox   7B:CE:7F:2B:4E:3D:D8:A3:38:E6:5C:DD:BD:DA:A1:98:E6:68:EA:B8:6B:36:98:BD:18:D0:33:65:15:04:05:C9
shared        B2:1E:0B:1F:DD:67:BA:3E:D0:03:2D:0A:4A:F3:5C:CA:3E:4C:A2:65:51:21:56:D1:58:79:7B:FD:E8:37:C2:07
testkey       34:8C:61:3A:CB:3C:31:85:55:93:9F:00:EA:8F:EE:77:27:CF:87:6D:0B:01:84:A8:10:05:2B:2A:0B:2E:10:68
```

`testkey` 是本项目生成的证书，不是 Android 开源项目的公共测试密钥。下载文件请核对对应版本的文件校验清单。

</details>

## 反馈与参与

普通问题请在 [Issues](https://github.com/yal3ay/xz1c-lineageos-22/issues) 中说明机型、系统版本、复现步骤和预期表现；日志或截图请先检查并移除个人资料。

安全漏洞请按 [安全政策](SECURITY.md) 使用私密报告入口，不在公开问题中发布未修复漏洞、凭据或签名材料。

感谢 LineageOS、Android 开源项目、Sony 开源设备社区，以及项目中使用的开源组件作者和测试用户。

## English

### A modern Android experience for a compact Xperia

This unofficial project brings **Android 15 and LineageOS 22.2** to the Japanese **Sony Xperia XZ1 Compact SO-02K**. It combines device-specific fixes with custom tools for audio, everyday system management, translation, document reading and connecting to Apple devices.

[Downloads and release notes](https://github.com/yal3ay/xz1c-lineageos-22/releases) · [Report an issue](https://github.com/yal3ay/xz1c-lineageos-22/issues) · [Security policy](SECURITY.md) · [中文](#xperia-xz1-compact--so-02k--lineageos-222)

> This is an independent project, not an official Sony or LineageOS release. Only SO-02K is the target of the documented device testing; other XZ1 Compact variants have not been verified. Features below describe the development line as documented on 2026-10-30. Read the notes for your specific build: the older default update channel does not include every feature below.

| | |
| --- | --- |
| Device | Xperia XZ1 Compact SO-02K (`lilac`; build target `lineage_lilac_dcm`) |
| System | Android 15 / LineageOS 22.2 |
| Kernel | Linux 4.4.302 |
| Android security patch level | 2026-08-01; this is not the release date |
| Default update channel, as documented | [22.2-20261010-r6](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261010-r6) |
| Developer update channel, as documented | [22.2-20261030-r2](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r2), a prerelease with the built-in Nearbo app and latest fixes |

### Features at a glance

> These highlights describe the development line. The older default-channel build may not include them; check the release notes for your installed version.

| What you want to do | What the system offers |
| --- | --- |
| Manage the phone | **System Manager** shows memory, processor and battery history; manage background activity, autostart, apps and caches, and precompile apps in batches. |
| Listen to music | **Hi-Fi Audio** shows the actual output device, sample rate and bit depth, with direct output, Sony effects, headphone boost and Bluetooth quality options on compatible paths. |
| Use speakers and TV | Send audio to compatible speakers with **AirPlay**, mirror screen and sound to a compatible Apple TV, and control playback, volume and text entry from the phone. |
| Share files | Send to supported Macs on the same Wi-Fi, exchange files with phones using LocalSend, or hand off to Google Quick Share. |
| Read and learn | Translate selected text in a floating card, look up common English words offline, review saved flashcards and export them to Anki. |
| Open documents | View common office files, images and media locally, with search, night mode and printing. |
| Check the weather | See the lunar calendar and Xperia weather on the lock screen, with optional humidity, wind, UV and air-quality details. |
| Everyday use | Wake with a double tap or volume key, use SMS code prompts, configure an SD card as internal storage, and benefit from device-specific call and touch fixes. |
| Update and report | Check for updates and manage add-ons; developer builds offer opt-in problem reports that you review before submitting. |

Audio behavior depends on the player and output device. AirPlay and sharing depend on the receiver and network; native AirDrop interoperability with every Apple device is not supported.

### What you can do

#### Manage the phone in one place

The custom **System Manager** brings memory and running status, background and autostart controls, cleanup, app management and app precompilation together. It can remember compilation choices and run batch operations or schedule work after app and system updates.

The battery page keeps local history across charging cycles, with 24-hour and seven-day charts, screen-on time and estimated per-app use. App management shows size and recent use and offers force stop, cache clearing and uninstalling. Cleanup requires the user to choose personal or large files. The acceleration action can stop applications that are currently running, including music players.

#### See and control audio output

The custom **Hi-Fi Audio** page shows the output device, sample rate, bit depth and available playback-path information. It provides compatible direct-output options for wired headphones and external audio devices, Sony sound enhancement, optional headphone output boost and Bluetooth quality settings.

**DSEE HX** is Sony's processing for compatible compressed audio paths. Headphone boost is optional; lower the volume before switching it on and return to the original setting if distortion occurs. Bluetooth quality depends on the connected device and codec and is not the same as lossless wired playback. Direct playback and source information depend on the player, media format and output device; the page does not guarantee that every app uses the same path.

#### Built-in Nearbo app: casting, music, TV remote and local sharing

Starting with **[22.2-20261030-r2](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r2)**, the developer build includes the standalone **Nearbo** app (`com.xz1c.appleconnect`, version **1.0.53**) in `/system/app/Nearbo`, combining big-screen casting, remote control and local file sharing in one app:

- **Screen mirroring and video casting**: mirror the phone screen and permitted media/game audio to compatible Apple TVs, Nearbo TV receivers and AirPlay-compatible TVs or speakers on the same Wi-Fi, with full-screen or single-app sharing, picture quality and orientation options, plus original-quality video casting to supported receivers.
- **Music streaming**: stream audio to compatible speakers or TVs with live lyrics, playback controls, per-device volume and a sleep timer.
- **TV remote**: pair with Nearbo TV or compatible receivers for a large touchpad, playback and hardware volume controls, app launching and phone keyboard text input.
- **Nearbo Share**: transfer photos, videos and files directly over local Wi-Fi to nearby Android devices, Nearbo TV or compatible Macs without uploading files to external servers.

Nearbo is preloaded as an ordinary non-privileged app with its independent signing identity preserved byte-for-byte, so user data is retained across updates and standalone APK or in-app updates continue to work. Every screen-capture session requires Android system consent.

#### Send audio and control Apple TV

**AirPlay** is integrated into the system media output panel and quick settings. Select a compatible HomePod or Apple TV for audio, adjust the receiver's volume, or mirror the screen and sound to a compatible Apple TV. Initial pairing may require a code displayed by the receiver.

The built-in **Apple TV remote** includes a touchpad, playback and volume controls, device switching and text entry from the phone. Its lock-screen media card uses progress and duration supplied by the receiver; it does not invent a duration for live content. Receiver model, network, access settings and content protection affect compatibility. Audio-only streaming has buffering delay and should not be assumed to provide low-latency video synchronization.

#### Share files nearby

The system share menu's **AirDrop** entry opens a nearby-device picker. It supports sending to compatible Macs that accept the project's same-network transfer path, phone-to-phone transfers using LocalSend with an acceptance screen and verification code, and handoff to Google Quick Share when available. Discovery and receiving run while the share page is open; an active transfer can continue in the background.

Mac receiving settings and network conditions matter. This implementation does **not** claim universal native AirDrop interoperability with iPhones or all newer Macs; consult the release notes for the tested scope.

#### Translate, read and review

Select text in a supported app to open a floating **translation card** with original and translated text, copy and speech controls, and an option to save a flashcard. The offline English dictionary handles common words locally. Google or user-configured DeepSeek translation and optional online speech require network access and send the selected text to the chosen service. Flashcards support reminders, review ratings and Anki-compatible export. The earlier separate BigBang entry is currently disabled.

The built-in **Documents** viewer is adapted from the open-source Gander project. It opens common office documents, images and media locally and offers search, night mode, printing and saving a copy. It does not require network permission; rendering support depends on the file and viewing components. Credits and licenses are available in the app.

#### Make the most of the hardware

- Lunar calendar, seasonal information and Xperia weather on the lock screen, with selectable details such as humidity, wind, ultraviolet index and air quality.
- Double-tap and volume-button wake options, plus a published fix for intermittent touch failure after waking and unlocking.
- SMS verification-code prompts and autofill; device-specific call audio and VoLTE configuration.
- Adoptable storage for using a compatible SD card as internal storage; performance and reliability depend on the card.
- Integrated WireGuard and Shizuku, KernelSU-Next support, and Google service status and compatibility settings. Shizuku and root access grant different capabilities and require appropriate user authorization. Google certification, payment and third-party app compatibility cannot be guaranteed.

#### Report a problem with consent

The **20261026 developer release** adds an opt-in Bug Feedback page to System Manager. Automatic collection and reminders are off by default. When enabled, limited technical information is saved locally; users review each report and choose whether to submit it. Reports do not automatically upload full logs or attach accounts, chats, photos or unique device identifiers. Local reports expire after seven days, with a maximum of 20; the backend is configured to delete reports after 30 days. See the release notes for the remaining validation scope.

Starting with [22.2-20261030-r1](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r1), manual reports include a chosen app and problem description, with the app version attached. User-started reproduction sessions last up to three minutes and can include silent video and bounded playback diagnostics. Missing information is explicitly marked; selected-app and shared media-service records are labeled separately. These are diagnostic clues, not proof of a root cause. **This update does not claim to fix Douyin display corruption.**

### Known limitations and work in progress

- An intermittent display corruption issue affecting some Douyin daily posts made from photos and music is still being investigated. A reproducible sample is needed; no cause or fix has been confirmed.
- User-started reproduction recording and optional silent screen video are included in [22.2-20261030-r1](https://github.com/yal3ay/xz1c-lineageos-22/releases/tag/22.2-20261030-r1). Build and artifact checks passed; a new complete installation and recording test on XZ1C were not performed in this task. Recording and sending require separate consent.
- Full native sharing interoperability with all Apple devices remains under development. A successful discovery or handshake alone does not prove a completed transfer.
- Network storage, the separate theme-color entry, the Sheep AI assistant and the AirPods popup are not presented as available features in current releases. Function availability and testing differ between builds.

### Download and installation

Download the matching system package, recovery image and checksum list from [GitHub Releases](https://github.com/yal3ay/xz1c-lineageos-22/releases). Sony applications and Google Apps are managed separately. Follow the **specific release notes** for installation and validation details.

- Updates from an existing build signed with the same project keys normally retain data; do not wipe data during a routine update.
- First installation or migration from a differently signed system may require an unlocked bootloader, a matching recovery environment and a data wipe. Back up and verify important data first.
- If an SD card is configured as internal storage, its encryption key is stored in the phone's data partition. Wiping that partition can make the card's data unreadable. Export important files and app data separately first.
- Use add-ons compatible with the project's version and signing identity. Verify downloads against the release's `SHA256SUMS`, for example with `shasum -a 256 -c SHA256SUMS`.

For maintainers, see the [OTA release workflow](OTA_RELEASE_WORKFLOW.md). Full update packages are currently used; an incremental build is not the same thing as an incremental update package.

### Source, licenses and feedback

Device configuration: [lilac](https://github.com/yal3ay/android_device_sony_lilac) and [Yoshino common](https://github.com/yal3ay/android_device_sony_yoshino-common). Other sources: [msm8998 kernel](https://github.com/yal3ay/android_kernel_sony_msm8998), [local manifests and extraction notes](https://github.com/yal3ay/xz1c_local_manifests), [system patches](https://github.com/yal3ay/xz1c_lineageos_22_patches), and [patches in this repository](patches/). The kernel is GPL-2.0; other components retain their own licenses, and this repository's license is in [LICENSE](LICENSE).

Sony proprietary files and device-extracted binaries are not distributed in those source repositories; extract them from a device or firmware you legally possess. Public signing certificate fingerprints are listed in the Chinese section above; private keys are not published. Use file checksums, not certificate fingerprints, to verify downloads.

For ordinary problems, open an [issue](https://github.com/yal3ay/xz1c-lineageos-22/issues) with the device, build, steps and expected behavior, removing personal information from attachments. Report unpatched security vulnerabilities privately as described in the [security policy](SECURITY.md).

Thanks to LineageOS, the Android Open Source Project, the Sony open-device community, open-source component authors and project testers.
