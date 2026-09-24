SUMMARY = "udev rule and systemd service to auto-mount PICUBE USB stick"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://picube-mount.rules \
           file://picube-mount.sh \
           file://picube-mount.service"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "picube-mount.service"
SYSTEMD_AUTO_ENABLE:${PN} = "enable"

do_install() {
    install -d ${D}${sysconfdir}/udev/rules.d
    install -m 0644 ${WORKDIR}/picube-mount.rules ${D}${sysconfdir}/udev/rules.d/99-picube-mount.rules

    install -d ${D}${bindir}
    install -m 0755 ${WORKDIR}/picube-mount.sh ${D}${bindir}/picube-mount.sh

    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/picube-mount.service ${D}${systemd_system_unitdir}/picube-mount.service
}

FILES:${PN} = "${sysconfdir}/udev/rules.d/99-picube-mount.rules \
               ${bindir}/picube-mount.sh \
               ${systemd_system_unitdir}/picube-mount.service"