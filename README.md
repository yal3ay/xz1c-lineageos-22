# Xperia XZ1 Compact / SO-02K — LineageOS 22.2

2026-09-27 项目私钥签名基线，Android 15，设备目标 `lilac_dcm`。

这是面向日本版 Xperia XZ1 Compact（SO-02K，设备代号 `lilac_dcm`）的 Android 15 / LineageOS 22.2 预发布版本，构建类型为 `userdebug`。

## Source code / 源码

The source for this ROM is published in these repositories:

- Device tree (`lilac`): https://github.com/yal3ay/android_device_sony_lilac
- Shared Yoshino device tree: https://github.com/yal3ay/android_device_sony_yoshino-common
- Kernel (Linux 4.4.302, GPL-2.0, complete corresponding source): https://github.com/yal3ay/android_kernel_sony_msm8998
- Local manifests and blob-extraction notes: https://github.com/yal3ay/xz1c_local_manifests
- Patches for modified AOSP/LineageOS projects: https://github.com/yal3ay/xz1c_lineageos_22_patches

Sony proprietary files (`vendor/sony/**`) and device-extracted blobs are not
published; extract them from a device or firmware image you legally own. No
signing keys are published.

本 ROM 的源碼公開在以上倉庫；內核是 GPL-2.0，完整對應源碼在 kernel repo。
`vendor/sony/**` 與裝置提取 blob 不公開，請自行從合法持有的裝置或固件提取。
不公開任何簽名私鑰。

## Signing certificate fingerprints (SHA-256)

Only the public certificate fingerprints are published. Certificate files and
private keys are not included in any source repository.

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

The `testkey` entry is a project-generated certificate, not the public AOSP
test key. Do not use these fingerprints to verify a download; verify the
download hashes in `SHA256SUMS`.

## 功能简介

- VoLTE 开关及按 IMS LTE 语音状态显示的状态栏图标。
- 电池充电控制快捷设置及闪电样式的充电状态栏图标。
- KernelSU-Next。
- `modem_switcher` 服务禁用配置。
- 基础 ROM、Sony 应用包和 GApps 分包；基础 ROM 保留设备运行及 Sony 相机所需底层组件。
- 配套 recovery 和项目 OTA 证书；本自举版本内置 GitHub OTA 更新清单地址。

以上为源码/产物功能说明；换签名后的开机、VoLTE 通话、相机、NFC、采用存储 SD 卡及 OTA 安装尚未完成实机验证。

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

**采用存储 SD 卡的加密密钥保存在手机数据分区。清除手机数据可能使原卡上的应用和文件无法读取。必须先独立导出卡内文件、应用安装包及必要应用数据。Seedvault 显示成功或备份目录有文件，并不保证所有应用数据都可恢复。**

1. 在电脑上运行 `shasum -a 256 -c SHA256SUMS` 核对下载文件。
2. 安装本版本配套 recovery，按该设备的安装方式进入 recovery。
3. 在备份已确认可恢复的前提下，完成首次签名迁移所需的数据清除，再安装基础 ROM。
4. 若需要 GApps，安装适配 Android 15 / arm64 的包；若需要索尼应用，使用与本项目新平台密钥配套的索尼附加包。同一次 recovery 会话内依次安装基础 ROM、GApps、索尼包，再启动系统。

旧索尼附加包包含使用旧平台签名的组件，不应与此基础 ROM 混用。

恢复备份时，旧平台签名的系统/索尼 APK 不能直接覆盖新版组件。第三方应用若保留原签名，可按其备份支持情况恢复；此说明不承诺所有应用数据或登录状态恢复一致。



## English

# Xperia XZ1 Compact SO-02K — LineageOS 22.2

Android 15 pre-release for the Japanese Xperia XZ1 Compact SO-02K (device target: `lilac_dcm`). This is the first baseline signed with this project's private release keys. Build type: `userdebug`.

### Features

- VoLTE switch and a status bar icon reflecting IMS LTE voice status.
- Battery charging control tile and a lightning style charging status bar icon.
- KernelSU-Next.
- `modem_switcher` service disabled in the device configuration.
- Base ROM, Sony apps, and GApps are distributed separately; device and Sony camera support libraries remain in the base ROM.
- Matching recovery and project OTA certificate; this bootstrap build includes the GitHub OTA feed URL.

These are source/artifact feature notes; boot, VoLTE calls, camera, NFC, adopted SD storage, and OTA installation on the new signing baseline have not been verified on hardware. Android security patch level: **2026-08-01**. ADB requires device authorization.

### Downloads and installation

The [GitHub Releases page](https://github.com/yal3ay/xz1c-lineageos-22/releases) contains the base ROM and matching recovery. Verify downloads with `shasum -a 256 -c SHA256SUMS`.

This release is intended only for an unlocked Japanese Xperia XZ1 Compact SO-02K. Other XZ1 Compact variants are unverified. The previous ROM used public test keys; this release uses a different system and OTA signing identity. Do not expect a data-preserving OTA from the old signing baseline. Install the OTA bootstrap release manually after confirming your backups can be restored.

**The encryption key for adopted storage is kept in the phone's data partition. Wiping data can make apps and files on the adopted SD card unreadable. Export the card's files, app installers, and needed app data separately first. A successful Seedvault status does not guarantee every app's data can be restored.**

If needed, install Android 15 / arm64 GApps and the Sony add-on built for this project's new platform key in the same recovery session, after the base ROM and before the first boot. Do not mix in an older Sony add-on signed with the old platform key.




