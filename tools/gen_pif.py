#!/usr/bin/env python3
"""Generate pif.json (Play Integrity spoof values) from the latest Pixel Canary build.

Same data sources and output as PlayIntegrityFork's autopif4.sh (MIT): the Pixel Beta
device list on developer.android.com, the canary build from the Android Flash Tool, and
the security patch level from the Pixel Update Bulletin.
"""
import json, random, re, sys, urllib.request, datetime

UA = {"User-Agent": "Mozilla/5.0"}


def get(url, headers=None):
    req = urllib.request.Request(url, headers={**UA, **(headers or {})})
    with urllib.request.urlopen(req, timeout=20) as r:
        return r.read().decode("utf-8", "replace")


def main():
    versions = get("https://developer.android.com/about/versions")
    latest = sorted(set(re.findall(r'href="(/about/versions/\d+)"', versions)),
                    key=lambda s: int(s.rsplit("/", 1)[1]))[-1]
    page = get("https://developer.android.com" + latest)
    links = re.findall(r'href="([^"]*download[^"]*)"', page)
    fi_link = sorted({l for l in links if "ota" not in l}, reverse=True)[0]
    ota_link = sorted({l for l in links if "download-ota" in l}, reverse=True)[0]
    fi = get("https://developer.android.com" + fi_link)
    ota = get("https://developer.android.com" + ota_link)
    src = ota if len(re.findall(r'<tr id="', fi)) < len(re.findall(r'<tr id="', ota)) else fi
    rows = re.findall(r'<tr id="([^"]+)">\s*<td>([^<]+)</td>', src)
    if not rows:
        sys.exit("no Pixel Beta devices found")

    flash = get("https://flash.android.com/")
    key = re.search(r'<body data-client-config=[^;]*;([^&]*)&', flash).group(1)
    random.shuffle(rows)
    for device, model in rows:
        product = device + "_beta"
        try:
            builds = json.loads(get(
                f"https://content-flashstation-pa.googleapis.com/v1/builds?product={product}&key={key}",
                {"Referer": "https://flash.android.com"}))
        except Exception:
            continue
        canary = [b for b in builds.get("flashstationBuild", [])
                  if (b.get("previewMetadata") or {}).get("canary")]
        if canary:
            c = canary[-1]
            break
    else:
        sys.exit("no canary build found")

    build_id, incremental = c["releaseCandidateName"], c["buildId"]
    canary_id = re.sub(r"^(.{4})", r"\1-", c["previewMetadata"]["id"].split("canary-", 1)[1])
    bulletin = get("https://source.android.com/docs/security/bulletin/pixel")
    m = re.search(r"<td>" + re.escape(canary_id) + r"[^<]*</td>\s*<td>([^<]+)</td>", bulletin)
    patch = m.group(1).strip() if m else canary_id + "-05"

    pif = {
        "MANUFACTURER": "Google",
        "MODEL": model.strip(),
        "FINGERPRINT": f"google/{product}/{device}:CANARY/{build_id}/{incremental}:user/release-keys",
        "PRODUCT": product,
        "DEVICE": device,
        "SECURITY_PATCH": patch,
        "DEVICE_INITIAL_SDK_INT": "32",
        "generated": datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%dT%H:%MZ"),
    }
    json.dump(pif, sys.stdout, indent=2)
    print()


if __name__ == "__main__":
    main()
