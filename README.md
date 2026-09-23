# Yocto Build Documentation
## Game Console Menu — Raspberry Pi 4 (armv7l)

---

## Table of Contents
1. [Overview](#overview)
2. [Dependencies](#dependencies)
3. [Licenses](#licenses)
4. [Host Requirements](#host-requirements)
5. [Directory Structure](#directory-structure)
6. [Layer Overview](#layer-overview)
7. [Configuration Files](#configuration-files)
8. [Recipe Files](#recipe-files)
9. [Startup Scripts](#startup-scripts)
10. [Image Recipe](#image-recipe)
11. [Build Instructions](#build-instructions)
12. [Flash Instructions](#flash-instructions)
13. [Updating the App](#updating-the-app)
14. [Planned Changes](#planned-changes)
15. [Lessons Learned](#lessons-learned)
16. [Repository History](#repository-history)

---

## Overview

This Yocto build produces a minimal Linux image for the **Raspberry Pi 4** (32-bit armv7l) that boots directly into an **Electron + React** game console menu application.

| Property | Value |
|---|---|
| Yocto Release | scarthgap (5.0 LTS) |
| Target Machine | raspberrypi4 (32-bit armv7l) |
| Init System | systemd |
| Display Server | X.Org |
| App Framework | Electron 32 + React + Vite |
| App Architecture | armv7l (32-bit) |

> ⚠️ **Architecture Note**: The machine is set to `raspberrypi4` (32-bit armv7l), NOT `raspberrypi4-64`. The Electron app must be built as `armv7l`, not `arm64`. Mixing these will cause `No such file or directory` errors even when the file clearly exists.

---

## Dependencies

### Third-Party Yocto Layers

These layers must be cloned alongside poky before building. They are **not** included in the `meta-game-console` repository.

| Layer | Repository | Branch | License |
|---|---|---|---|
| `poky` | https://git.yoctoproject.org/poky | `scarthgap` | MIT / GPLv2 |
| `meta-raspberrypi` | https://git.yoctoproject.org/meta-raspberrypi | `scarthgap` | MIT |
| `meta-openembedded` | https://github.com/openembedded/meta-openembedded | `scarthgap` | MIT |

```bash
# Clone all required layers
git clone https://git.yoctoproject.org/poky -b scarthgap
cd poky
git clone https://git.yoctoproject.org/meta-raspberrypi -b scarthgap
git clone https://github.com/openembedded/meta-openembedded.git -b scarthgap
```

### Application Dependencies (npm)

The Electron app uses the following packages. These are built on the **dev machine**, not inside Yocto.

| Package | Version | License |
|---|---|---|
| `electron` | ^32.0.0 | MIT |
| `electron-builder` | ^25.0.0 | MIT |
| `vite` | ^5.4.0 | MIT |
| `@vitejs/plugin-react` | ^4.3.0 | MIT |
| `concurrently` | ^9.2.1 | MIT |
| `react` | ^18.3.1 | MIT |
| `react-dom` | ^18.3.1 | MIT |
| `react-router-dom` | ^7.13.1 | MIT |

### Runtime System Libraries (installed in Yocto image)

These libraries are cross-compiled by Yocto and installed into the image. They are required by Electron at runtime.

| Library | Yocto Package | Provided By | Purpose |
|---|---|---|---|
| `libnss3` | `nss` | `meta-openembedded/meta-oe` | Chromium security / SSL |
| `libnspr4` | `nss` | `meta-openembedded/meta-oe` | Netscape Portable Runtime |
| `libatk-1.0` | `atk` | `meta-openembedded/meta-oe` | Accessibility toolkit |
| `libgtk-3` | `gtk+3` | `meta-openembedded/meta-oe` | GTK UI toolkit |
| `libpango` | `pango` | `meta-openembedded/meta-oe` | Text rendering |
| `libcups` | `cups` | `meta-openembedded/meta-oe` | Print system |
| `libX11` | `libx11` | `meta` (OE-Core) | X11 display |
| `libdrm` | `libdrm` | `meta` (OE-Core) | GPU/DRM interface |
| `libasound` | `alsa-lib` | `meta` (OE-Core) | Audio |
| `libz` | `zlib` | `meta` (OE-Core) | Compression |

### Host Build Tools

```bash
sudo apt install gawk wget git diffstat unzip texinfo gcc build-essential \
  chrpath socat cpio python3 python3-pip python3-pexpect xz-utils debianutils \
  iputils-ping python3-git python3-jinja2 libegl1-mesa libsdl1.2-dev \
  xterm python3-subunit mesa-common-dev zstd liblz4-tool file \
  squashfs-tools bmap-tools
```

---

## Licenses

### Custom Layer (`meta-game-console`)
The `meta-game-console` layer (configuration files, recipes, and scripts) is proprietary and **CLOSED** license. The `LICENSE = "CLOSED"` declaration in the recipe tells Yocto not to perform license compliance checks on the app itself.

### Electron Application
The game console menu application is proprietary. However it is built on top of open-source components:

| Component | License | Notes |
|---|---|---|
| Electron | MIT | Bundles Chromium (BSD) and Node.js (MIT) |
| Chromium | BSD 3-Clause | Bundled inside Electron |
| React | MIT | UI framework |
| Vite | MIT | Build tool |
| react-router-dom | MIT | Routing |

### Yocto Layers
| Layer | License |
|---|---|
| poky / OE-Core | MIT and GPLv2 |
| meta-raspberrypi | MIT |
| meta-openembedded | MIT |

### Restricted Licenses
The build accepts the `synaptics-killswitch` restricted license which is required for the Raspberry Pi WiFi/Bluetooth firmware (`linux-firmware-rpidistro`). This is set in `local.conf`:

```bitbake
LICENSE_FLAGS_ACCEPTED = "synaptics-killswitch"
```

This license covers the proprietary Broadcom/Cypress wireless firmware blobs used by the Pi 4's onboard WiFi chip. By accepting this license you agree to the terms set by Synaptics/Broadcom for use of their firmware. The firmware is only used for WiFi/Bluetooth functionality — the image will boot and run without it, but wireless will not work.

> 📋 Full license text: https://github.com/RPi-Distro/firmware-nonfree/blob/master/debian/copyright

### GPL Compliance Note
Because the image includes GPL-licensed software (Linux kernel, busybox, etc.), the corresponding source code is made available by the Yocto Project at https://downloads.yoctoproject.org. Yocto automatically tracks all licenses used in the build — you can find the license manifest at:
```
build/tmp/deploy/images/raspberrypi4/game-console-image-raspberrypi4.rootfs.manifest
```

---

## Host Requirements

- Ubuntu 22.04 x86_64
- At least 50GB free disk space
- At least 8GB RAM (16GB recommended)
- Node.js 18+ (for building the Electron app)
- Required packages listed in the [Dependencies](#dependencies) section above

---

## Directory Structure

```
/mnt/yoctobuild/poky/
├── meta/                          # OE-Core layer
├── meta-poky/                     # Poky distro layer
├── meta-yocto-bsp/                # Yocto BSP layer
├── meta-raspberrypi/              # Raspberry Pi BSP layer (scarthgap branch)
├── meta-openembedded/             # OpenEmbedded layers
│   ├── meta-oe/                   # Base OE packages (nss, atk, gtk+3, etc.)
│   ├── meta-python/               # Python packages
│   └── meta-networking/           # Networking packages
├── meta-game-console/             # Custom layer (our app)
│   ├── conf/
│   │   └── layer.conf
│   ├── recipes-apps/
│   │   └── game-console-menu/
│   │       ├── game-console-menu.bb
│   │       └── files/
│   │           ├── squashfs-root/         # Extracted Electron app
│   │           ├── game-console-menu.service
│   │           ├── start-game-console.sh
│   │           └── start-app.sh
│   └── recipes-core/
│       └── images/
│           └── game-console-image.bb
└── build/
    └── conf/
        ├── local.conf
        └── bblayers.conf
```

---

## Layer Overview

| Layer | Purpose |
|---|---|
| `meta` | OE-Core — base recipes for Linux |
| `meta-poky` | Poky reference distro configuration |
| `meta-yocto-bsp` | Generic BSP configs |
| `meta-raspberrypi` | Pi-specific kernel, firmware, GPU drivers |
| `meta-openembedded/meta-oe` | Provides `nss`, `atk`, `gtk+3`, `pango`, `cups` needed by Electron |
| `meta-openembedded/meta-python` | Python3 and packages |
| `meta-openembedded/meta-networking` | connman, networking tools |
| `meta-game-console` | Our custom layer — app recipe + image recipe |

### Cloning the layers
```bash
cd /mnt/yoctobuild/poky

# Raspberry Pi BSP
git clone https://git.yoctoproject.org/meta-raspberrypi -b scarthgap

# OpenEmbedded (provides Electron dependencies)
git clone https://github.com/openembedded/meta-openembedded.git -b scarthgap
```

---

## Configuration Files

### `build/conf/bblayers.conf`
Tells Yocto which layers to include in the build.

```bitbake
BBLAYERS ?= " \
  /mnt/yoctobuild/poky/meta \
  /mnt/yoctobuild/poky/meta-poky \
  /mnt/yoctobuild/poky/meta-yocto-bsp \
  /mnt/yoctobuild/poky/meta-raspberrypi \
  /mnt/yoctobuild/poky/meta-openembedded/meta-oe \
  /mnt/yoctobuild/poky/meta-openembedded/meta-python \
  /mnt/yoctobuild/poky/meta-openembedded/meta-networking \
  /mnt/yoctobuild/poky/meta-game-console \
"
```

### `build/conf/local.conf`
Main build configuration file.

```bitbake
# Target hardware — 32-bit Pi 4
# IMPORTANT: Do NOT use raspberrypi4-64, the Electron app is armv7l
MACHINE = "raspberrypi4"

DISTRO = "poky"
PACKAGE_CLASSES = "package_rpm"
EXTRA_IMAGE_FEATURES ?= "debug-tweaks"
USER_CLASSES ?= "buildstats"
PATCHRESOLVE = "noop"

BB_DISKMON_DIRS ??= "\
    STOPTASKS,${TMPDIR},1G,100K \
    STOPTASKS,${DL_DIR},1G,100K \
    STOPTASKS,${SSTATE_DIR},1G,100K \
    STOPTASKS,/tmp,100M,100K \
    HALT,${TMPDIR},100M,1K \
    HALT,${DL_DIR},100M,1K \
    HALT,${SSTATE_DIR},100M,1K \
    HALT,/tmp,10M,1K \
"

# systemd as init manager (required for usrmerge)
DISTRO_FEATURES:append = " systemd usrmerge x11"
VIRTUAL-RUNTIME_init_manager = "systemd"
DISTRO_FEATURES_BACKFILL_CONSIDERED = "sysvinit"
VIRTUAL-RUNTIME_initscripts = ""

# GPU memory for Pi
GPU_MEM = "64"

# Accept restricted license for Pi firmware/WiFi
LICENSE_FLAGS_ACCEPTED = "synaptics-killswitch"
```

---

## Recipe Files

### `meta-game-console/conf/layer.conf`

```bitbake
BBPATH .= ":${LAYERDIR}"
BBFILES += "${LAYERDIR}/recipes-*/*/*.bb"

BBFILE_COLLECTIONS += "game-console"
BBFILE_PATTERN_game-console = "^${LAYERDIR}/"
BBFILE_PRIORITY_game-console = "6"

LAYERDEPENDS_game-console = "core"
LAYERSERIES_COMPAT_game-console = "scarthgap"
```

### `meta-game-console/recipes-apps/game-console-menu/game-console-menu.bb`
The main recipe that installs the Electron app.

```bitbake
SUMMARY = "Game Console Menu Electron App"
DESCRIPTION = "Electron + React game console launcher"
LICENSE = "CLOSED"

# Skip QA checks — the Electron binary is pre-built and pre-stripped
INSANE_SKIP:${PN} = "already-stripped file-rdeps ldflags arch"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
EXCLUDE_FROM_SHLIBS = "1"

# Satisfy RPM auto-generated dependency checks for bundled libs
RPROVIDES:${PN} += " \
    libz.so()(64bit) \
    libatk-1.0.so.0()(64bit) \
    libatk-bridge-2.0.so.0()(64bit) \
    libatspi.so.0()(64bit) \
    libcups.so.2()(64bit) \
    libgtk-3.so.0()(64bit) \
    libnspr4.so()(64bit) \
    libnss3.so()(64bit) \
    libnssutil3.so()(64bit) \
    libpango-1.0.so.0()(64bit) \
    libsmime3.so()(64bit) \
    libnss3.so(NSS_3.11)(64bit) \
    libnss3.so(NSS_3.12)(64bit) \
    libnss3.so(NSS_3.12.1)(64bit) \
    libnss3.so(NSS_3.2)(64bit) \
    libnss3.so(NSS_3.22)(64bit) \
    libnss3.so(NSS_3.3)(64bit) \
    libnss3.so(NSS_3.30)(64bit) \
    libnss3.so(NSS_3.4)(64bit) \
    libnss3.so(NSS_3.5)(64bit) \
    libnss3.so(NSS_3.9.2)(64bit) \
    libnssutil3.so(NSSUTIL_3.12.3)(64bit) \
    libsmime3.so(NSS_3.10)(64bit) \
    libsmime3.so(NSS_3.2)(64bit) \
"

SRC_URI = "file://squashfs-root \
           file://game-console-menu.service \
           file://start-game-console.sh \
           file://start-app.sh \
           "

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "game-console-menu.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

# System libraries that Electron needs from the OS
RDEPENDS:${PN} = " \
    libx11 \
    libxcomposite \
    libxdamage \
    libxext \
    libxfixes \
    libxrandr \
    libxrender \
    libxtst \
    libxi \
    libdrm \
    alsa-lib \
    zlib \
    nss \
    atk \
    gtk+3 \
    pango \
    cups \
"

do_install() {
    # Install extracted Electron app
    install -d ${D}/opt/game-console-menu
    cp -r ${WORKDIR}/squashfs-root/. ${D}/opt/game-console-menu/

    # Install launcher scripts
    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/start-game-console.sh \
        ${D}${bindir}/start-game-console.sh
    install -m 0755 ${WORKDIR}/start-app.sh \
        ${D}${bindir}/start-app.sh

    # Install systemd service
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/game-console-menu.service \
        ${D}${systemd_system_unitdir}/game-console-menu.service
}

FILES:${PN} += " \
    /opt/game-console-menu \
    ${bindir}/start-game-console.sh \
    ${bindir}/start-app.sh \
    ${systemd_system_unitdir}/game-console-menu.service \
"
```

---

## Startup Scripts

### `files/start-game-console.sh`
Main entry point called by systemd. Starts X then launches the app.

```bash
#!/bin/sh
# Kill any existing X locks
rm -f /tmp/.X0-lock
rm -f /tmp/.X11-unix/X0

# Start X server in background
X :0 -nolisten tcp &
XPID=$!

# Wait for X to initialize (important — app will fail if X isn't ready)
sleep 3

export DISPLAY=:0
export HOME=/root
export DBUS_SESSION_BUS_ADDRESS="unix:path=/run/dbus/system_bus_socket"

# Launch Electron app
/opt/game-console-menu/game-console-menu \
    --no-sandbox \
    --disable-gpu \
    --disable-software-rasterizer \
    --disable-gpu-compositing \
    --use-gl=swiftshader \
    --display=:0

# Kill X when app exits
kill $XPID
```

> ⚠️ **Important**: The `sleep 3` is required. Without it, Electron launches before X is ready and fails silently.

### `files/start-app.sh`
Direct app launcher (used for manual testing).

```bash
#!/bin/sh
export DISPLAY=:0
export HOME=/root
export DBUS_SESSION_BUS_ADDRESS="unix:path=/run/dbus/system_bus_socket"
exec /opt/game-console-menu/game-console-menu \
    --no-sandbox \
    --disable-gpu \
    --disable-software-rasterizer \
    --disable-gpu-compositing \
    --use-gl=swiftshader \
    --display=:0
```

> ⚠️ **Important**: `--disable-gpu` and `--use-gl=swiftshader` are required. Electron's GPU process crashes on minimal Yocto due to missing EGL configs.

### `files/game-console-menu.service`
Systemd service that runs on boot.

```ini
[Unit]
Description=Game Console Menu
After=local-fs.target systemd-user-sessions.service

[Service]
Type=simple
User=root
Environment=HOME=/root
ExecStart=/usr/bin/start-game-console.sh
Restart=on-failure
RestartSec=5

[Install]
WantedBy=multi-user.target
```

> ⚠️ **Important**: Do NOT add `TTYPath` or `StandardInput=tty` to this service — it causes `SIGHUP` which kills the process immediately.

---

## Image Recipe

### `meta-game-console/recipes-core/images/game-console-image.bb`

```bitbake
require recipes-core/images/core-image-base.bb

DESCRIPTION = "Game Console Image with Electron launcher"

IMAGE_INSTALL:append = " \
    xserver-xorg \
    xserver-xorg-extension-glx \
    xf86-video-fbdev \
    xinit \
    matchbox-wm \
    connman \
    connman-client \
    openssh \
    nss \
    atk \
    gtk+3 \
    pango \
    cups \
    game-console-menu \
"

IMAGE_FEATURES:append = " \
    ssh-server-openssh \
    debug-tweaks \
"

ROOTFS_POSTPROCESS_COMMAND:append = " setup_autologin;"

setup_autologin() {
    mkdir -p ${IMAGE_ROOTFS}/etc/systemd/system/getty@tty1.service.d
    cat > ${IMAGE_ROOTFS}/etc/systemd/system/getty@tty1.service.d/autologin.conf << AUTOLOGIN
[Service]
ExecStart=
ExecStart=-/sbin/agetty --autologin root --noclear %I \$TERM
AUTOLOGIN
}
```

---

## Build Instructions

```bash
# 1. Source the Yocto environment
source /mnt/yoctobuild/poky/oe-init-build-env /mnt/yoctobuild/poky/build

# 2. Build the Electron app AppImage on dev machine
cd ~/ReactProjects/ElectronProjects/game-console-menu
npm run package:pi   # produces dist/Game-Console-Menu-1.0.5-armv7l.AppImage

# 3. Extract the AppImage (cannot run arm binary on x86)
OFFSET=$(grep -boa 'hsqs' dist/Game-Console-Menu-1.0.5-armv7l.AppImage | head -1 | cut -d: -f1)
rm -rf squashfs-root
unsquashfs -offset $OFFSET dist/Game-Console-Menu-1.0.5-armv7l.AppImage

# 4. Copy extracted app to Yocto layer
rm -rf /mnt/yoctobuild/poky/meta-game-console/recipes-apps/game-console-menu/files/squashfs-root
cp -r squashfs-root \
  /mnt/yoctobuild/poky/meta-game-console/recipes-apps/game-console-menu/files/

# 5. Build the Yocto image
cd /mnt/yoctobuild/poky/build
bitbake game-console-image

# Output image location:
# build/tmp/deploy/images/raspberrypi4/game-console-image-raspberrypi4.rootfs.wic.bz2
```

---

## Flash Instructions

```bash
# 1. Identify SD card
lsblk

# 2. Unmount if mounted
sudo umount /media/$USER/boot
sudo umount /media/$USER/root

# 3. Flash (uses .bmap for fast sparse write)
sudo bmaptool copy \
  /mnt/yoctobuild/poky/build/tmp/deploy/images/raspberrypi4/game-console-image-raspberrypi4.rootfs.wic.bz2 \
  /dev/sdX   # replace with your actual device e.g. /dev/sdc

# 4. Eject
sudo eject /dev/sdX
```

---

## Updating the App

When you make changes to the React/Electron app:

```bash
# 1. Rebuild the app
cd ~/ReactProjects/ElectronProjects/game-console-menu
npm run package:pi

# 2. Re-extract
OFFSET=$(grep -boa 'hsqs' dist/Game-Console-Menu-1.0.5-armv7l.AppImage | head -1 | cut -d: -f1)
rm -rf squashfs-root
unsquashfs -offset $OFFSET dist/Game-Console-Menu-1.0.5-armv7l.AppImage

# 3. Update Yocto layer
rm -rf /mnt/yoctobuild/poky/meta-game-console/recipes-apps/game-console-menu/files/squashfs-root
cp -r squashfs-root \
  /mnt/yoctobuild/poky/meta-game-console/recipes-apps/game-console-menu/files/

# 4. Clean and rebuild
cd /mnt/yoctobuild/poky/build
bitbake game-console-menu -c cleanall && bitbake game-console-image
```

---

## Planned Changes

**TODO: shift from a games-only console to a general app/game launcher** — pygame games remain the initial supported format, expanding to general Python applications. See the `pi-cube-game-console` and `windows-cube-game-console` READMEs for the in-progress app-side pivot.

This layer currently bakes the "game console" naming into several places that will need to be updated together once the app itself is renamed (not before — this layer just packages whatever the app is called):
- Recipe: `recipes-apps/game-console-menu/game-console-menu.bb`
- Install path: `/opt/game-console-menu/`
- systemd service + scripts: `game-console-menu.service`, `start-game-console.sh`, `start-app.sh`
- Image recipe + output: `recipes-core/images/game-console-image.bb` → `game-console-image-raspberrypi4.rootfs.wic.bz2`

---

## Lessons Learned

### 1. Architecture must match
The Pi 4 can run both 32-bit (armv7l) and 64-bit (aarch64) but **Raspberry Pi OS uses 32-bit userspace** by default. The Electron app must be built for `armv7l`. Using `arm64` causes a cryptic `No such file or directory` error even when the file is present — this is because the 64-bit ELF binary looks for `/lib/ld-linux-aarch64.so.1` which doesn't exist on a 32-bit userspace.

### 2. AppImage cannot be run on x86 host
AppImages are ELF binaries for the target architecture. Use `unsquashfs` with an offset to extract the contents on an x86 host:
```bash
OFFSET=$(grep -boa 'hsqs' app.AppImage | head -1 | cut -d: -f1)
unsquashfs -offset $OFFSET app.AppImage
```

### 3. Electron needs meta-openembedded
A minimal Yocto image doesn't include `libnss3`, `libgtk3`, `libatk`, `libpango`, or `libcups`. These are required by Electron/Chromium and are provided by `meta-openembedded/meta-oe`.

### 4. GPU must be disabled
Electron's GPU process crashes on minimal Yocto due to missing EGL configuration. Always launch with:
```
--disable-gpu --disable-software-rasterizer --disable-gpu-compositing --use-gl=swiftshader
```

### 5. X must start before the app
The systemd service starts `start-game-console.sh` which launches X, then waits 3 seconds before launching Electron. Without the wait, Electron fails with `Missing X server or $DISPLAY`.

### 6. Avoid TTY flags in systemd service
Adding `TTYPath=/dev/tty1` and `StandardInput=tty` to the service causes `SIGHUP` which kills the process immediately. Keep the service definition minimal.

### 7. usrmerge is required for systemd in scarthgap
Add `usrmerge` to `DISTRO_FEATURES` or systemd/udev will fail to build:
```bitbake
DISTRO_FEATURES:append = " systemd usrmerge x11"
```

### 8. RPM dependency scanning
Yocto's RPM packager scans binaries for shared library dependencies and auto-generates `Requires:` entries. For pre-built binaries like Electron, use `INSANE_SKIP`, `EXCLUDE_FROM_SHLIBS`, and `RPROVIDES` to suppress these.

---

## Repository History
The repository moved on 2026-09-23 from the `Brickhouse4U` GitHub account to [`javersa86`](https://github.com/javersa86), consolidating all portfolio projects under one account. History, issues, and pull requests came with it, and old `github.com/Brickhouse4U/...` links redirect here automatically. To update an existing clone:
```bash
git remote set-url origin https://github.com/javersa86/meta-game-console.git
```
