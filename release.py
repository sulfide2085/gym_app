from __future__ import annotations

import argparse
import os
import shutil
import subprocess
import sys
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent
APP_BUILD_FILE = PROJECT_ROOT / "app" / "build.gradle.kts"
WORKER_INDEX_FILE = PROJECT_ROOT / "cloudflare" / "worker" / "src" / "index.ts"
KEYSTORE_PROPERTIES_FILE = PROJECT_ROOT / "keystore.properties"
DEFAULT_KEYSTORE_REL = Path("keystore") / "gym-app-release.jks"


def step(message: str) -> None:
    print(f"==> {message}")


def run(cmd: list[str], cwd: Path | None = None) -> None:
    subprocess.run(cmd, cwd=cwd or PROJECT_ROOT, check=True)


def resolve_executable(name: str) -> str:
    candidates = [name]
    if os.name == "nt" and not name.lower().endswith(".cmd"):
        candidates.insert(0, f"{name}.cmd")
        candidates.insert(1, f"{name}.exe")

    for candidate in candidates:
        resolved = shutil.which(candidate)
        if resolved:
            return resolved

    return name


def read_text(path: Path) -> str:
    return path.read_text(encoding="utf-8")


def write_text(path: Path, content: str) -> None:
    path.write_text(content, encoding="utf-8", newline="\n")


def ensure_java_keytool() -> Path:
    java_home = os.environ.get("JAVA_HOME")
    if java_home:
        candidate = Path(java_home) / "bin" / "keytool.exe"
        if candidate.exists():
            return candidate
        candidate = Path(java_home) / "bin" / "keytool"
        if candidate.exists():
            return candidate

    for entry in os.environ.get("PATH", "").split(os.pathsep):
        if not entry:
            continue
        base = Path(entry)
        for name in ("keytool.exe", "keytool"):
            candidate = base / name
            if candidate.exists():
                return candidate

    raise SystemExit("keytool was not found. Install a JDK or set JAVA_HOME.")


def parse_version(version: str) -> tuple[str, int]:
    version_name = version.removeprefix("v")
    parts = version_name.split(".")
    major = int(parts[0])
    minor = int(parts[1]) if len(parts) > 1 else 0
    version_code = major * 100 + minor
    return version_name, version_code


def replace_once(content: str, old_fragment: str, new_fragment: str) -> str:
    if old_fragment not in content:
        raise SystemExit(f"Expected fragment not found: {old_fragment}")
    return content.replace(old_fragment, new_fragment, 1)


def update_android_version(version_name: str, version_code: int) -> None:
    content = read_text(APP_BUILD_FILE)
    import re

    content, count_code = re.subn(r'versionCode = \d+', f"versionCode = {version_code}", content, count=1)
    content, count_name = re.subn(r'versionName = "[^"]*"', f'versionName = "{version_name}"', content, count=1)
    if count_code != 1 or count_name != 1:
        raise SystemExit("Failed to update Android version fields.")
    write_text(APP_BUILD_FILE, content)


def update_worker_release_info(version_name: str, version_code: int, notes: list[str]) -> None:
    content = read_text(WORKER_INDEX_FILE)
    import re

    notes_json = ", ".join(f'"{note}"' for note in notes)
    content, count_code = re.subn(
        r"const LATEST_ANDROID_VERSION_CODE = \d+",
        f"const LATEST_ANDROID_VERSION_CODE = {version_code}",
        content,
        count=1,
    )
    content, count_name = re.subn(
        r'const LATEST_ANDROID_VERSION_NAME = "[^"]*"',
        f'const LATEST_ANDROID_VERSION_NAME = "{version_name}"',
        content,
        count=1,
    )
    content, count_notes = re.subn(
        r"const RELEASE_NOTES = \[.*?\];",
        f"const RELEASE_NOTES = [{notes_json}];",
        content,
        count=1,
        flags=re.DOTALL,
    )
    if count_code != 1 or count_name != 1 or count_notes != 1:
        raise SystemExit("Failed to update worker release info.")
    write_text(WORKER_INDEX_FILE, content)


def release_output_path() -> Path:
    signed = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
    unsigned = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
    return signed if signed.exists() else unsigned


def deploy_release_apk_to_pages() -> None:
    output = release_output_path()
    if not output.exists():
        raise SystemExit(f"Release APK not found: {output}")

    releases_dir = PROJECT_ROOT / "cloudflare" / "releases"
    releases_dir.mkdir(parents=True, exist_ok=True)
    target = releases_dir / "app-debug.apk"
    shutil.copyfile(output, target)

    step("Deploying signed APK to Cloudflare Pages")
    run(
        [
            resolve_executable("wrangler"),
            "pages",
            "deploy",
            str(releases_dir),
            "--project-name=gym-app-releases",
        ]
    )


def build(variant: str) -> None:
    task = "assembleRelease" if variant == "release" else "assembleDebug"
    step(f"Building {variant} APK")
    run([str(PROJECT_ROOT / "gradlew.bat"), task])
    if variant == "debug":
        output = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"
    else:
        output = release_output_path()
    if output.exists():
        step(f"Done: {output.relative_to(PROJECT_ROOT)}")
    else:
        raise SystemExit(f"Build finished but APK was not found: {output}")


def write_keystore_properties(store_file: Path, store_password: str, key_alias: str, key_password: str) -> None:
    content = "\n".join(
        [
            f"storeFile={store_file.as_posix()}",
            f"storePassword={store_password}",
            f"keyAlias={key_alias}",
            f"keyPassword={key_password}",
            "",
        ]
    )
    write_text(KEYSTORE_PROPERTIES_FILE, content)


def init_signing(args: argparse.Namespace) -> None:
    keystore_rel = Path(args.keystore_file or DEFAULT_KEYSTORE_REL)
    keystore_path = PROJECT_ROOT / keystore_rel
    keystore_path.parent.mkdir(parents=True, exist_ok=True)

    store_password = args.store_password or input("Store password: ").strip()
    if not store_password:
        raise SystemExit("Store password is required.")

    key_password = args.key_password or input("Key password [same as store password]: ").strip() or store_password
    key_alias = args.key_alias or input("Key alias [gym-app]: ").strip() or "gym-app"

    keytool = ensure_java_keytool()
    if not keystore_path.exists():
        step("Generating keystore")
        run(
            [
                str(keytool),
                "-genkeypair",
                "-v",
                "-keystore",
                str(keystore_path),
                "-alias",
                key_alias,
                "-keyalg",
                "RSA",
                "-keysize",
                "2048",
                "-validity",
                "10000",
                "-storepass",
                store_password,
                "-keypass",
                key_password,
                "-dname",
                "CN=Gym App, OU=Dev, O=Gym App, L=Shanghai, ST=Shanghai, C=CN",
            ]
        )
    else:
        step(f"Keystore already exists: {keystore_rel.as_posix()}")

    write_keystore_properties(keystore_rel, store_password, key_alias, key_password)
    step(f"Done: {KEYSTORE_PROPERTIES_FILE.name}")
    step(f"Done: {keystore_rel.as_posix()}")
    print("Keep this keystore and its passwords safe. You need the same signature for future updates.")


def publish(version: str, notes: list[str]) -> None:
    if not notes:
        raise SystemExit("Publish requires at least one release note.")

    version_name, version_code = parse_version(version)
    step(f"Publishing v{version_name} (versionCode={version_code})")
    update_android_version(version_name, version_code)
    update_worker_release_info(version_name, version_code, notes)

    step("Committing release files")
    run(["git", "add", str(APP_BUILD_FILE), str(WORKER_INDEX_FILE)])
    run(["git", "commit", "-m", f"Release v{version_name}"])

    step("Pushing to GitHub")
    run(["git", "push", "origin", "main"])

    step("Deploying Cloudflare Worker")
    run([resolve_executable("wrangler"), "deploy"], cwd=PROJECT_ROOT / "cloudflare" / "worker")

    build("release")
    deploy_release_apk_to_pages()

    print(f"\nRelease complete: v{version_name}")
    print("CI will build the APK and deploy Cloudflare Pages.")


def create_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Android release helper")
    subparsers = parser.add_subparsers(dest="command", required=True)

    init_parser = subparsers.add_parser("init-signing", help="Create or register a release keystore")
    init_parser.add_argument("keystore_file", nargs="?", help="Relative keystore path, default keystore/gym-app-release.jks")
    init_parser.add_argument("key_alias", nargs="?", help="Key alias, default gym-app")
    init_parser.add_argument("store_password", nargs="?", help="Store password")
    init_parser.add_argument("key_password", nargs="?", help="Key password")
    init_parser.set_defaults(func=init_signing)

    build_debug = subparsers.add_parser("build-debug", help="Build debug APK")
    build_debug.set_defaults(func=lambda _args: build("debug"))

    build_release = subparsers.add_parser("build-release", help="Build release APK")
    build_release.set_defaults(func=lambda _args: build("release"))

    publish_parser = subparsers.add_parser("publish", help="Update version and publish")
    publish_parser.add_argument("version", help="Version like 1.4")
    publish_parser.add_argument("notes", nargs="+", help="Release notes")
    publish_parser.set_defaults(func=lambda args: publish(args.version, args.notes))

    return parser


def main() -> None:
    parser = create_parser()
    args = parser.parse_args()
    args.func(args)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
