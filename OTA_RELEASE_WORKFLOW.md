# XZ1C OTA Release Workflow

Runbook for future maintainers and agents of `yal3ay/xz1c-lineageos-22`.

## Release format

- Publish a **full OTA ZIP** for each release. The current bootstrap package is about 1.08 GB. Do not upload `target_files.zip` as the end-user OTA.
- The build pipeline currently creates a full package by running `ota_from_target_files` without `--incremental_from`. A full OTA contains the system update payload; it normally preserves `/data` when applied over the project's existing signing baseline.
- Keep the same project signing keys for every update. Never upload private keys. A signing-certificate migration is a separate clean-install operation and can require wiping `/data`.
- Incremental OTAs are technically possible by building against the exact previous target-files package with `--incremental_from`. They apply only to their specified source build, so full OTAs are the default for this project.
- Current GitHub Release convention: attach the base ROM OTA ZIP and its matching `recovery.img` only. Sony apps and GApps remain separate packages. Do not attach `target_files.zip`, signing keys, or phone backups.
- Keep the Release marked as a pre-release until real-device testing supports a stable release.

## Per-release steps

1. Build `lineage_lilac_dcm` using the build host's `BUILD-RULES.md` and `/home/joey/build.sh`; do not run raw `m`/`mka` commands.
2. Confirm `lineage.updater.uri` is present in `system/build.prop` and points to `https://raw.githubusercontent.com/yal3ay/xz1c-lineageos-22/main/ota/lilac.json`.
3. Create the full OTA ZIP from the target-files using the existing project private keys and the established signing script. Preserve `addon.d` support (`--backup=true`). Extract the matching recovery from the same signed target-files.
4. Verify the OTA certificate/signature, platform APK and source-built APEX signatures, recovery OTA trust, target device, package contents, and SHA-256 hashes. Confirm the OTA's `post-timestamp` is newer than the previous release.
5. Create a GitHub pre-release with the full OTA ZIP and matching recovery. Include both SHA-256 hashes and bilingual installation notes. Sony and GApps packages remain separate.
6. Only after the Release assets are uploaded, replace `ota/lilac.json` with the new package entry. Verify both the public raw JSON URL and the Release download URL.
7. Keep the prior Release available for rollback unless the repository owner explicitly requests deletion.

## OTA feed entry

The LineageOS 22.2 Updater in this build reads the legacy `response` array. Keep the field names and types below; use the exact OTA ZIP filename and byte size:

```json
{
  "response": [
    {
      "datetime": 1790497558,
      "filename": "lineage-22.2-YYYYMMDD-UNOFFICIAL-lilac_dcm-ota.zip",
      "id": "<OTA ZIP SHA-256>",
      "romtype": "UNOFFICIAL",
      "size": 1079842107,
      "url": "https://github.com/yal3ay/xz1c-lineageos-22/releases/download/<tag>/<filename>",
      "version": "22.2"
    }
  ]
}
```

For each release, replace the example timestamp with `post-timestamp` from `META-INF/com/android/metadata`, the hash and size with values from the final signed OTA ZIP, and the URL with the uploaded asset URL. The current updater expects the `response` list; do not copy a newer LineageOS schema unless the Updater app in this ROM is also upgraded and verified.

## Bootstrap and device behavior

- The original baseline did not contain the custom feed URL. Existing devices must install the OTA bootstrap build manually once. After that, Updater can discover entries added to `ota/lilac.json`.
- The bootstrap migration from public Lineage test signatures to this project's private signatures is not a data-preserving OTA. Back up first; adopted-storage keys are held in `/data`, so wiping data can make that SD card unreadable.
- After a device has the bootstrap build, later packages signed with the same project keys should use the normal OTA flow and normally preserve user data. OTA installation has not yet been verified on physical hardware; do not claim it has been tested until it is.
- `recovery.img` is primarily for clean installs or recovery repair/update. Ordinary OTA users should use the Updater flow; provide the matching recovery asset with each public full release per current project convention.
