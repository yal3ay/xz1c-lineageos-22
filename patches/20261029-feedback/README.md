# Diagnostic feedback update — 2026-10-29

Source overlays for the XZ1C System Manager feedback update. This directory contains project-authored Java/XML changes, receiving-service code and tests; it contains no Sony application binaries or upload/admin credentials.

## Apply

Start with the project's existing System Manager sources and the `20261026-bug-feedback.patch` in the parent directory. Copy `client/src/` into `vendor/sony/lilac/apps/ZramStatus/src/`, and `client/README-bug-feedback.md` into that app directory. The new reproduction service, recording permissions and `PlaybackDiagnostics` source are included. Keep the private `FeedbackProvider.java` generation step from the existing project; do not publish that file.

The system app keeps the same package and signing identity. Its version code is increased to `20261029` so older test updates cannot mask the new system version after an OTA.

The server accepts category-only legacy reports as well as app descriptions, package/version information and the extended reproduction-session payload. It does not require a database migration. Raise the technical request limit to 65536 bytes at the reverse proxy; the existing video envelope keeps its separate 21 MiB request limit. Back up the existing service and configuration before deployment and retain its existing private environment and database.

## Diagnostic scope

The recording window is at most 180 seconds. Media logs are read once at the end, from the requested start time, with a fixed media-tag list, a 3-second command timeout and a 256 KiB read cap. Only matching lines inside the recording window are retained, up to 48 lines / 8192 UTF-8 bytes; recent lines replace older ones within that bounded output. If the source buffer was overwritten, the service was inaccessible or no logs were produced, this cannot reconstruct the missing information.

Each retained line labels `selected_app` when its process ID was observed for the chosen app, otherwise `shared_media`. A shared-media error can come from another player or the recorder. These records are clues, not proof of the selected app's root cause. Network addresses in diagnostic lines are replaced. Optional silent video uses Android's screen-capture permission and is reviewed before submission. This is not a fix for Douyin display corruption.

## Tests

`python3 server/test_server.py` tests both payload generations, rejected fields and sizes, authenticated submission, idempotency and escaped administration output in a temporary local inbox. It does not use production credentials or records.

`tests/host-test/` contains minimal Android/JSON stand-ins for testing the production media-line filter on a host JVM. Compile those sources together with `client/src/main/java/org/lineageos/zramstatus/PlaybackDiagnostics.java`, excluding any Mac `._*` metadata files, and run `org.lineageos.zramstatus.PlaybackDiagnosticsTest`. The tests cover time bounds, media tags, process labels, deduplication, address replacement, UTF-8 limits and an unavailable tool. These host tests do not verify Android's runtime log permissions or actual device video recording.

The release notes state the package checks and the remaining physical-device validation scope. Components retain their existing project licenses; see the repository's LICENSE and upstream component notices.
