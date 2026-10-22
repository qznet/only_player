#!/usr/bin/env python3
"""准备构建环境：Gradle 启动器与便携 JDK。"""
import os
import sys

os.environ["PYTHONDONTWRITEBYTECODE"] = "1"
sys.dont_write_bytecode = True

import argparse
import hashlib
import platform
import re
import shutil
import subprocess
import tarfile
import tempfile
import urllib.request
import zipfile
from dataclasses import dataclass
from pathlib import Path

MIN_JDK = 26
JDK_DIR = Path("build/jdk")
TMP_DIR = Path("build/tmp")
GRADLE_WRAPPER_FILES = ("gradlew", "gradlew.bat", "gradle/wrapper/gradle-wrapper.jar")
ADOPTIUM_URL = "https://api.adoptium.net/v3/binary/latest/{major}/ga/{os_name}/{arch}/jdk/hotspot/normal/eclipse"


@dataclass(frozen=True)
class Jdk:
    home: Path
    major: int


def fail(message: str) -> None:
    raise SystemExit(f"error: {message}")


def resolve_project_root() -> Path:
    current = Path(__file__).resolve().parent
    for candidate in (current, *current.parents):
        if (candidate / "settings.gradle.kts").is_file() and (candidate / "app" / "build.gradle.kts").is_file():
            return candidate
    fail("project root not found next to this script")


def read_java_major(java_home: Path) -> int | None:
    release = java_home / "release"
    version = None
    if release.is_file():
        match = re.search(r'^JAVA_VERSION="([^"]+)"', release.read_text(encoding="utf-8", errors="replace"), re.M)
        version = match.group(1) if match else None
    if version is None:
        launcher = java_home / "bin" / ("java.exe" if os.name == "nt" else "java")
        if not launcher.is_file():
            return None
        result = subprocess.run([str(launcher), "-version"], capture_output=True, text=True, errors="replace")
        match = re.search(r'version "([^"]+)"', result.stderr + result.stdout)
        version = match.group(1) if match else None
    if version is None:
        return None
    parts = version.split(".")
    # 1.8.0 这类旧写法的主版本在第二段
    major = parts[1] if parts[0] == "1" and len(parts) > 1 else parts[0]
    parsed = int(re.sub(r"\D.*$", "", major) or 0)
    return parsed or None


def _is_jdk_home(path: Path) -> bool:
    launcher = path / "bin" / ("java.exe" if os.name == "nt" else "java")
    return launcher.is_file() and (path / "release").is_file()


def find_jdk_home(root: Path) -> Path | None:
    if not root.is_dir():
        return None
    if _is_jdk_home(root):
        return root
    for child in sorted(p for p in root.iterdir() if p.is_dir()):
        if _is_jdk_home(child):
            return child
        # macOS 归档多一层 Contents/Home
        mac_home = child / "Contents" / "Home"
        if _is_jdk_home(mac_home):
            return mac_home
    return None


def current_portable_jdk(project_root: Path) -> Jdk | None:
    home = find_jdk_home(project_root / JDK_DIR)
    if home is None:
        return None
    major = read_java_major(home)
    if major is None or major < MIN_JDK:
        return None
    return Jdk(home, major)


def host_target() -> tuple[str, str]:
    machine = platform.machine().lower()
    if machine in ("amd64", "x86_64"):
        arch = "amd64"
    elif machine in ("arm64", "aarch64"):
        arch = "arm64"
    else:
        fail(f"unsupported CPU for portable JDK: {machine}")
    if os.name == "nt":
        return "windows", arch
    if sys.platform == "darwin":
        return "darwin", arch
    return "linux", arch


def adoptium_target() -> tuple[str, str]:
    os_name, arch = host_target()
    return ("mac" if os_name == "darwin" else os_name), {"amd64": "x64", "arm64": "aarch64"}[arch]


def _archive_kind(path: Path) -> str:
    with path.open("rb") as handle:
        magic = handle.read(4)
    if magic[:2] == b"PK":
        return "zip"
    if magic[:2] == b"\x1f\x8b":
        return "tar.gz"
    fail("downloaded JDK archive is not zip or tar.gz")


def _extract(archive: Path, dest: Path) -> None:
    if _archive_kind(archive) == "zip":
        with zipfile.ZipFile(archive) as zf:
            zf.extractall(dest)
        return
    with tarfile.open(archive) as tf:
        try:
            tf.extractall(dest, filter="data")
        except TypeError:
            tf.extractall(dest)


def _download(url: str, dest: Path) -> None:
    dest.parent.mkdir(parents=True, exist_ok=True)
    request = urllib.request.Request(url, headers={"User-Agent": "only-player-prebuild"})
    with urllib.request.urlopen(request) as response, dest.open("wb") as out:
        shutil.copyfileobj(response, out)


def install_portable_jdk(project_root: Path, *, verbose: bool = False) -> Jdk:
    os_name, arch = adoptium_target()
    url = ADOPTIUM_URL.format(major=MIN_JDK, os_name=os_name, arch=arch)
    dest = project_root / JDK_DIR
    print(f"[info] downloading Temurin {MIN_JDK} {os_name}/{arch}")
    if verbose:
        print(f"[info] url: {url}")
    if dest.exists():
        shutil.rmtree(dest)
    dest.mkdir(parents=True)
    tmp_root = project_root / TMP_DIR
    tmp_root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="only-player-jdk-", dir=tmp_root) as tmp:
        archive = Path(tmp) / "jdk.bin"
        _download(url, archive)
        _extract(archive, dest)
    home = find_jdk_home(dest)
    if home is None:
        fail(f"extracted archive has no JDK under {JDK_DIR.as_posix()}")
    major = read_java_major(home)
    if major is None or major < MIN_JDK:
        fail(f"portable JDK is {major}, need {MIN_JDK}+")
    print(f"[ ok ] portable jdk={home.relative_to(project_root).as_posix()} major={major}")
    return Jdk(home, major)


def ensure_portable_jdk(project_root: Path, *, force: bool = False, verbose: bool = False) -> Jdk:
    if not force:
        found = current_portable_jdk(project_root)
        if found is not None:
            if verbose:
                print(f"[info] portable JDK ready: {found.home} (JDK {found.major})")
            return found
    print(f"[info] portable JDK {MIN_JDK}+ missing; downloading")
    return install_portable_jdk(project_root, verbose=verbose)


def read_properties(path: Path) -> dict[str, str]:
    if not path.is_file():
        return {}
    values: dict[str, str] = {}
    for line in path.read_text(encoding="utf-8", errors="replace").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith(("#", "!")) or "=" not in stripped:
            continue
        key, _, value = stripped.partition("=")
        # Java properties 会转义分隔符，还原成真实路径
        values[key.strip()] = value.strip().replace("\\:", ":").replace("\\\\", "\\")
    return values


def ensure_gradle_wrapper(project_root: Path) -> None:
    missing = [name for name in GRADLE_WRAPPER_FILES if not (project_root / name).is_file()]
    if not missing:
        return
    properties = read_properties(project_root / "gradle/wrapper/gradle-wrapper.properties")
    match = re.fullmatch(
        r"https://services\.gradle\.org/distributions/gradle-(\d+\.\d+(?:\.\d+)?)-(?:bin|all)\.zip",
        properties.get("distributionUrl", ""),
    )
    if not match:
        fail("unsupported Gradle distributionUrl; expected an official numbered release")
    version = match.group(1)
    tmp_root = project_root / TMP_DIR
    tmp_root.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(prefix="gradle-wrapper-", dir=tmp_root) as tmp:
        for name in missing:
            downloaded = Path(tmp) / Path(name).name
            _download(f"https://raw.githubusercontent.com/gradle/gradle/v{version}/{name}", downloaded)
            if name.endswith(".jar"):
                checksum = Path(tmp) / "wrapper.sha256"
                _download(
                    f"https://services.gradle.org/distributions/gradle-{version}-wrapper.jar.sha256",
                    checksum,
                )
                expected = checksum.read_text(encoding="utf-8").strip()
                if hashlib.sha256(downloaded.read_bytes()).hexdigest() != expected:
                    fail("Gradle wrapper checksum mismatch")
            target = project_root / name
            target.parent.mkdir(parents=True, exist_ok=True)
            downloaded.replace(target)
            if name == "gradlew":
                target.chmod(0o755)
    print(f"[ ok ] Gradle {version} wrapper ready")


def sdk_dir_candidates(project_root: Path) -> list[tuple[str, Path]]:
    candidates: list[tuple[str, Path]] = []
    configured = read_properties(project_root / "local.properties").get("sdk.dir")
    if configured:
        candidates.append(("local.properties", Path(configured)))
    for key in ("ANDROID_HOME", "ANDROID_SDK_ROOT"):
        value = os.environ.get(key)
        if value:
            candidates.append((key, Path(value)))
    home = Path.home()
    if os.name == "nt":
        local_app_data = os.environ.get("LOCALAPPDATA", str(home / "AppData" / "Local"))
        candidates.append(("default", Path(local_app_data) / "Android" / "Sdk"))
    elif sys.platform == "darwin":
        candidates.append(("default", home / "Library" / "Android" / "sdk"))
    else:
        candidates.append(("default", home / "Android" / "Sdk"))
    return candidates


def detect_sdk(project_root: Path, *, verbose: bool = False) -> Path:
    for source, path in sdk_dir_candidates(project_root):
        expanded = path.expanduser()
        if (expanded / "platforms").is_dir() or (expanded / "platform-tools").is_dir():
            if verbose:
                print(f"[info] sdk found from {source}: {expanded}")
            return expanded
    fail("Android SDK not found; set ANDROID_HOME or install it through Android Studio")


def missing_resources(project_root: Path) -> list[str]:
    missing = [name for name in GRADLE_WRAPPER_FILES if not (project_root / name).is_file()]
    if current_portable_jdk(project_root) is None:
        missing.append(JDK_DIR.as_posix())
    return missing


def main() -> int:
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(encoding="utf-8")
        sys.stderr.reconfigure(encoding="utf-8")
    parser = argparse.ArgumentParser(
        prog="only-player-prebuild",
        description="Prepare the Gradle launcher and portable JDK used by the build scripts.",
    )
    parser.add_argument("--force", action="store_true", help="Replace the existing portable JDK")
    parser.add_argument("-v", "--verbose", action="store_true", help="Print extra detail")
    args = parser.parse_args()

    root = resolve_project_root()
    ensure_gradle_wrapper(root)
    jdk = ensure_portable_jdk(root, force=args.force, verbose=args.verbose)
    print(f"[ ok ] JAVA_HOME={jdk.home.relative_to(root).as_posix()}")
    detect_sdk(root, verbose=args.verbose)

    missing = missing_resources(root)
    if missing:
        fail(f"build resources missing: {', '.join(missing)}")
    print("[ ok ] build resources ready")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
