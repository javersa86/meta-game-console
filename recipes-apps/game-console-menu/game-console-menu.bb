SUMMARY = "Game Console Menu Electron App"
DESCRIPTION = "Electron + React game console launcher"
LICENSE = "CLOSED"
PV = "1.0.5"

# Skip QA checks — the Electron binary is pre-built and pre-stripped
INSANE_SKIP:${PN} = "already-stripped file-rdeps ldflags arch"
INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_SYSROOT_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"
EXCLUDE_FROM_SHLIBS = "1"

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
