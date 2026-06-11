from __future__ import annotations

import argparse
import os
import subprocess
import sys
import time
from pathlib import Path


PROJECT_ROOT = Path(__file__).resolve().parent
DEFAULT_PACKAGE = "com.example.gym_app"
DEFAULT_ACTIVITY = ".MainActivity"


def step(message: str) -> None:
    print(f"==> {message}")


def read_local_properties() -> dict[str, str]:
    path = PROJECT_ROOT / "local.properties"
    if not path.exists():
        return {}
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8").splitlines():
        if "=" in line and not line.startswith("#"):
            key, value = line.split("=", 1)
            values[key] = value
    return values


def android_sdk_path() -> Path | None:
    for key in ("ANDROID_SDK_ROOT", "ANDROID_HOME"):
        value = os.environ.get(key)
        if value:
            return Path(value)

    raw = read_local_properties().get("sdk.dir")
    if raw:
        return Path(raw.replace("\\:", ":").replace("\\\\", "\\"))
    return None


def resolve_tool(command_name: str, *sdk_candidates: str, required: bool = True) -> Path | None:
    path_env = os.environ.get("PATH", "")
    for entry in path_env.split(os.pathsep):
        if not entry:
            continue
        base = Path(entry)
        for name in (command_name, f"{command_name}.exe"):
            candidate = base / name
            if candidate.exists():
                return candidate

    sdk = android_sdk_path()
    if sdk:
        for candidate in sdk_candidates:
            full = sdk / candidate
            if full.exists():
                return full

    if required:
        raise SystemExit(f"Tool not found: {command_name}")
    return None


ADB_PATH = resolve_tool("adb", "platform-tools/adb.exe", "platform-tools/adb")
EMULATOR_PATH = resolve_tool("emulator", "emulator/emulator.exe", "emulator/emulator", required=False)


def adb_prefix(device_id: str | None) -> list[str]:
    return ["-s", device_id] if device_id else []


def run(cmd: list[str], cwd: Path | None = None, capture: bool = False) -> subprocess.CompletedProcess[str]:
    return subprocess.run(
        cmd,
        cwd=cwd or PROJECT_ROOT,
        check=True,
        text=True,
        capture_output=capture,
    )


def adb(args: list[str], device_id: str | None = None, capture: bool = False) -> str:
    result = run([str(ADB_PATH), *adb_prefix(device_id), *args], capture=capture)
    return result.stdout if capture else ""


def connected_devices() -> list[str]:
    output = adb(["devices"], capture=True)
    devices: list[str] = []
    for line in output.splitlines():
        line = line.strip()
        if not line or line.startswith("List of devices attached"):
            continue
        parts = line.split()
        if len(parts) >= 2 and parts[1] == "device":
            devices.append(parts[0])
    return devices


def device_boot_completed(device_id: str) -> bool:
    try:
        output = adb(["shell", "getprop", "sys.boot_completed"], device_id, capture=True).strip()
    except subprocess.CalledProcessError:
        return False
    return output == "1"


def list_avds() -> list[str]:
    if not EMULATOR_PATH:
        raise SystemExit("Android emulator tool not found.")
    output = run([str(EMULATOR_PATH), "-list-avds"], capture=True).stdout
    return [line.strip() for line in output.splitlines() if line.strip()]


def choose_default_avd(avds: list[str]) -> str:
    if "Pixel_8" in avds:
        return "Pixel_8"
    return avds[0]


def start_emulator(avd_name: str | None) -> None:
    avds = list_avds()
    if not avds:
        raise SystemExit("No AVD found. Create one in Android Studio Device Manager first.")
    target = avd_name or choose_default_avd(avds)
    if target not in avds:
        raise SystemExit(f"AVD not found: {target}")
    step(f"Starting emulator {target}")
    subprocess.Popen([str(EMULATOR_PATH), "-avd", target], cwd=PROJECT_ROOT)


def wait_for_device(device_id: str | None, timeout_seconds: int = 180) -> str:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        devices = connected_devices()
        if device_id and device_id in devices:
            return device_id
        if not device_id and len(devices) == 1:
            return devices[0]
        if not device_id and len(devices) > 1:
            raise SystemExit("Multiple devices detected. Use --device-id.")
        time.sleep(3)
    if device_id:
        raise SystemExit(f"Timed out waiting for device: {device_id}")
    raise SystemExit("Timed out waiting for an Android device or emulator.")


def wait_for_boot_completed(device_id: str, timeout_seconds: int = 180) -> None:
    deadline = time.time() + timeout_seconds
    while time.time() < deadline:
        if device_boot_completed(device_id):
            return
        time.sleep(3)
    raise SystemExit(f"Timed out waiting for Android boot completion: {device_id}")


def ensure_device(device_id: str | None, avd_name: str | None) -> str:
    devices = connected_devices()
    if device_id:
        if device_id in devices:
            if not device_boot_completed(device_id):
                step(f"Waiting for device boot completion: {device_id}")
                wait_for_boot_completed(device_id)
            return device_id
        raise SystemExit(f"Device not found: {device_id}")
    if len(devices) == 1:
        if not device_boot_completed(devices[0]):
            step(f"Waiting for device boot completion: {devices[0]}")
            wait_for_boot_completed(devices[0])
        return devices[0]
    if len(devices) > 1:
        raise SystemExit("Multiple devices detected. Use --device-id.")
    start_emulator(avd_name)
    step("Waiting for emulator to connect to adb")
    device = wait_for_device(None)
    step("Waiting for Android boot completion")
    wait_for_boot_completed(device)
    step(f"Device ready: {device}")
    return device


def apk_path(variant: str) -> Path:
    if variant == "release":
        signed = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release.apk"
        unsigned = PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "release" / "app-release-unsigned.apk"
        return signed if signed.exists() else unsigned
    return PROJECT_ROOT / "app" / "build" / "outputs" / "apk" / "debug" / "app-debug.apk"


def build(variant: str) -> None:
    task = "assembleRelease" if variant == "release" else "assembleDebug"
    step(f"Building {variant} APK")
    run([str(PROJECT_ROOT / "gradlew.bat"), task])
    output = apk_path(variant)
    if not output.exists():
        raise SystemExit(f"APK not found: {output}")
    step(f"Done: {output.relative_to(PROJECT_ROOT)}")


def install(variant: str, device_id: str | None, avd_name: str | None) -> None:
    selected_device = ensure_device(device_id, avd_name)
    output = apk_path(variant)
    if not output.exists():
        raise SystemExit(f"APK not found: {output}. Run build first.")
    step(f"Installing {output.relative_to(PROJECT_ROOT)}")
    adb(["install", "-r", str(output)], selected_device)


def start_app(package_name: str, activity_name: str, device_id: str | None, avd_name: str | None) -> None:
    selected_device = ensure_device(device_id, avd_name)
    step(f"Starting {package_name}/{activity_name}")
    adb(["shell", "am", "start", "-n", f"{package_name}/{activity_name}"], selected_device)


def app_pid(package_name: str, device_id: str) -> str | None:
    output = adb(["shell", "pidof", package_name], device_id, capture=True).strip()
    if not output:
        return None
    return output.split()[0]


def stream_logcat(package_name: str, device_id: str | None, avd_name: str | None) -> None:
    selected_device = ensure_device(device_id, avd_name)
    pid = app_pid(package_name, selected_device)
    if pid:
        step(f"Streaming logcat for PID {pid}")
        subprocess.run([str(ADB_PATH), "-s", selected_device, "logcat", "--pid", pid], check=True)
        return
    step("PID not available yet, falling back to full logcat")
    subprocess.run([str(ADB_PATH), "-s", selected_device, "logcat"], check=True)


def startup_log(package_name: str, activity_name: str, device_id: str | None, avd_name: str | None) -> None:
    selected_device = ensure_device(device_id, avd_name)
    step("Clearing old logcat buffer")
    adb(["logcat", "-c"], selected_device)
    step(f"Starting {package_name}/{activity_name}")
    adb(["shell", "am", "start", "-n", f"{package_name}/{activity_name}"], selected_device)
    time.sleep(2)
    stream_logcat(package_name, selected_device, avd_name)


def doctor() -> None:
    step("Environment check")
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Android SDK: {android_sdk_path()}")
    print(f"adb path: {ADB_PATH}")
    print(f"emulator path: {EMULATOR_PATH}")
    print(f"Gradle wrapper: {PROJECT_ROOT / 'gradlew.bat'}")
    step("Connected devices")
    subprocess.run([str(ADB_PATH), "devices"], check=True)


def show_emulators() -> None:
    step("Available emulators")
    avds = list_avds()
    if not avds:
        print("No AVD found.")
        return
    for avd in avds:
        print(avd)


def run_flow(variant: str, package_name: str, activity_name: str, device_id: str | None, avd_name: str | None) -> None:
    build(variant)
    install(variant, device_id, avd_name)
    start_app(package_name, activity_name, device_id, avd_name)


def create_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Android preview helper")
    parser.add_argument("--device-id", help="adb device id")
    parser.add_argument("--avd-name", help="AVD name to auto-start when no device is connected")
    parser.add_argument("--variant", choices=("debug", "release"), default="debug")
    parser.add_argument("--package-name", default=DEFAULT_PACKAGE)
    parser.add_argument("--activity-name", default=DEFAULT_ACTIVITY)

    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("doctor")
    subparsers.add_parser("devices")
    subparsers.add_parser("emulators")
    subparsers.add_parser("start-emulator")
    subparsers.add_parser("build")
    subparsers.add_parser("install")
    subparsers.add_parser("start")
    subparsers.add_parser("run")
    subparsers.add_parser("logcat")
    subparsers.add_parser("startup-log")
    return parser


def main() -> None:
    parser = create_parser()
    args = parser.parse_args()

    if args.command == "doctor":
        doctor()
    elif args.command == "devices":
        step("Connected devices")
        subprocess.run([str(ADB_PATH), "devices"], check=True)
    elif args.command == "emulators":
        show_emulators()
    elif args.command == "start-emulator":
        start_emulator(args.avd_name)
    elif args.command == "build":
        build(args.variant)
    elif args.command == "install":
        install(args.variant, args.device_id, args.avd_name)
    elif args.command == "start":
        start_app(args.package_name, args.activity_name, args.device_id, args.avd_name)
    elif args.command == "run":
        run_flow(args.variant, args.package_name, args.activity_name, args.device_id, args.avd_name)
    elif args.command == "logcat":
        stream_logcat(args.package_name, args.device_id, args.avd_name)
    elif args.command == "startup-log":
        startup_log(args.package_name, args.activity_name, args.device_id, args.avd_name)


if __name__ == "__main__":
    try:
        main()
    except subprocess.CalledProcessError as exc:
        raise SystemExit(exc.returncode) from exc
