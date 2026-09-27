# Xperia XZ1 Compact / SO-02K — LineageOS 22.2

2026-09-27 项目私钥签名基线，Android 15，设备目标 `lilac_dcm`。

这是首次采用本项目密钥的预发布版本。该版本包含自定义内核、KernelSU-Next 及系统功能改动，构建类型为 `userdebug`。换签名后的开机、相机、通信、NFC、采用存储 SD 卡和更新流程尚须实机验证。

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

自动 OTA 下载地址将在 GitHub 仓库及 Release 地址确定后配置。本版用于建立新的签名基线，尚未提供可用的自动 OTA 服务。

## 发布前应补齐

- 实机验证并记录：开机、移动网络/通话、相机、NFC、采用存储、充电控制，以及新签名版本间的 OTA。
- 提供对应内核源码、KernelSU-Next 及实际内核改动和构建说明，并保留相关许可证。
- 在 Release 中写明实际安全补丁日期、已知问题及设备支持范围。
- 不公开签名私钥、手机备份、恢复码或内部项目记录。

## 上传方式

在 GitHub Releases 中创建 **Pre-release**，把 ROM、recovery 和 SHA256SUMS 作为附件上传，正文使用本说明。ROM 大于普通仓库文件限制，应通过 Releases 分发；[GitHub 官方说明](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases)规定单个 Release 附件须小于 2 GiB。
