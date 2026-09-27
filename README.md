# Xperia XZ1 Compact / SO-02K — LineageOS 22.2

2026-09-27 项目私钥签名基线，Android 15，设备目标 `lilac_dcm`。

这是面向日本版 Xperia XZ1 Compact（SO-02K，设备代号 `lilac_dcm`）的 Android 15 / LineageOS 22.2 预发布版本，构建类型为 `userdebug`。

## 功能简介

- 集成 KernelSU-Next；SUSFS 未启用。
- 电池充电控制快捷设置及对应状态栏图标使用闪电图形。
- 基础 ROM 与 Sony 应用、GApps 分包；基础 ROM 保留设备运行和 Sony 相机所需底层组件。
- 提供与新签名基线配套的 recovery 和 OTA 证书。

以上列出的是构建内容。换签名后的开机、相机、通信、NFC、采用存储 SD 卡和更新流程尚须实机验证。

实际 Android 安全补丁日期为 **2026-08-01**。KernelSU 已启用，SUSFS 未启用。ADB 仍要求设备授权。

## 下载文件

- `lineage-22.2-20260927-UNOFFICIAL-lilac_dcm-private.zip`：基础 ROM。
- `recovery.img`：从同一份最终签名产物提取的 recovery。
- `SHA256SUMS`：下载完整性校验。

基础 ROM 保留 Aperture、Glimpse、Twelve、Etar；索尼应用和 GApps 单独安装。基础 ROM 仍包含设备运行及索尼相机所需的底层驱动、库和权限配置。

## 已完成的构建检查

整包 OTA 签名、平台应用签名、所有自编译 APEX 的容器及载荷签名均已验证。配套 recovery 已包含项目 OTA 证书。电池控制图标的中间闪电已编入系统资源；OTA 包已启用附加包保留机制。以上为产物检查，实机刷入和后续 OTA 尚未验证。

## 首次换签名安装

仅适用于已解锁 bootloader 的 Xperia XZ1 Compact SO-02K。其他机型和其他 XZ1 Compact 变体未验证。

旧版使用源码公开测试密钥，新版更换了系统应用和 OTA 的签名身份。旧版不能作为可直接保留数据 OTA 升级的基线；首次迁移应在备份确认可恢复后清除数据安装。

**采用存储 SD 卡的加密密钥保存在手机数据分区。清除手机数据可能使原卡上的应用和文件无法读取。必须先独立导出卡内文件、应用安装包及必要应用数据。Seedvault 显示成功或备份目录有文件，并不保证所有应用数据都可恢复。**

1. 在电脑上运行 `shasum -a 256 -c SHA256SUMS` 核对下载文件。
2. 安装本版本配套 recovery，按该设备的安装方式进入 recovery。
3. 在备份已确认可恢复的前提下，完成首次签名迁移所需的数据清除，再安装基础 ROM。
4. 若需要 GApps，安装适配 Android 15 / arm64 的包；若需要索尼应用，使用与本项目新平台密钥配套的索尼附加包。同一次 recovery 会话内依次安装基础 ROM、GApps、索尼包，再启动系统。

旧索尼附加包包含使用旧平台签名的组件，不应与此基础 ROM 混用。

恢复备份时，旧平台签名的系统/索尼 APK 不能直接覆盖新版组件。第三方应用若保留原签名，可按其备份支持情况恢复；此说明不承诺所有应用数据或登录状态恢复一致。

## 签名与后续更新

OTA release certificate SHA-256：

`9162f30f5fe74c6998c6c06b9120dc75a0ac3f140b2234aa0befd2412dfa1159`

本项目会保留同一套签名密钥制作后续版本。系统自编译 APEX 模块使用项目独立密钥；上游预签名组件保留原签名。密钥私有部分不随下载文件发布。

ROM 更新包启用 `addon.d`，供兼容的 GApps 和索尼附加包保留安装内容。此机制仍须在新版基线上完成实际更新验证。

当前已发布的签名基线没有编入自定义 OTA 地址，因此它不会自动发现本项目更新。即将发布的 OTA 自举版会内置 GitHub 更新清单地址。现有设备须先手动安装自举版；之后 Updater 才能检查后续 OTA。

## 发布前应补齐

- 实机验证并记录：开机、移动网络/通话、相机、NFC、采用存储、充电控制，以及新签名版本间的 OTA。
- 提供对应内核源码、KernelSU-Next 及实际内核改动和构建说明，并保留相关许可证。
- 在 Release 中写明实际安全补丁日期、已知问题及设备支持范围。
- 不公开签名私钥、手机备份、恢复码或内部项目记录。

## 上传方式

ROM 大于普通仓库文件限制，应通过 Releases 分发；[GitHub 官方说明](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)规定单个 Release 附件须小于 2 GiB。


## English

# Xperia XZ1 Compact SO-02K — LineageOS 22.2

Android 15 pre-release for the Japanese Xperia XZ1 Compact SO-02K (device target: `lilac_dcm`). This is the first baseline signed with this project's private release keys. Build type: `userdebug`.

### Features

- KernelSU-Next is integrated; SUSFS is disabled.
- The battery charging control tile and its status bar icon use a lightning symbol.
- The base ROM is separate from Sony apps and GApps. Device and Sony camera support libraries remain in the base ROM.
- Matching recovery trusts this project's OTA certificate.

These describe the build contents, not completed device testing. Boot, camera, calls, NFC, adopted SD storage, and OTA installation still need verification on hardware. Android security patch level: **2026-08-01**. ADB requires device authorization.

### Downloads and installation

The [GitHub Releases page](https://github.com/yal3ay/xz1c-lineageos-22/releases) contains the base ROM and matching recovery. Verify downloads with `shasum -a 256 -c SHA256SUMS`.

This release is intended only for an unlocked Japanese Xperia XZ1 Compact SO-02K. Other XZ1 Compact variants are unverified. The previous ROM used public test keys; this release uses a different system and OTA signing identity. Do not expect a data-preserving OTA from the old signing baseline. Install the OTA bootstrap release manually after confirming your backups can be restored.

**The encryption key for adopted storage is kept in the phone's data partition. Wiping data can make apps and files on the adopted SD card unreadable. Export the card's files, app installers, and needed app data separately first. A successful Seedvault status does not guarantee every app's data can be restored.**

If needed, install Android 15 / arm64 GApps and the Sony add-on built for this project's new platform key in the same recovery session, after the base ROM and before the first boot. Do not mix in an older Sony add-on signed with the old platform key.

### Signing and OTA bootstrap

OTA release certificate SHA-256: `9162f30f5fe74c6998c6c06b9120dc75a0ac3f140b2234aa0befd2412dfa1159`. The project will keep these signing keys for future builds; private key material is not distributed. The OTA package enables `addon.d` retention for compatible add-ons, but an actual upgrade has not yet been device-tested.

The currently published baseline does not include this project's OTA feed URL, so it cannot discover project updates automatically. The upcoming OTA bootstrap build will include the GitHub update feed address. Install that build manually once; the Updater can then check for later releases.

Before a stable/public release, verify boot, mobile network and calls, camera, NFC, adopted storage, charging control, and an OTA between builds. Publish the corresponding kernel source and license notices. Never publish signing private keys, phone backups, recovery codes, or internal notes.
