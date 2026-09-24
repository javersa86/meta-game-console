DESCRIPTION = "Initialize ALSA analog audio output"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SRC_URI = "file://audio-init.service"

S = "${WORKDIR}"

inherit systemd

SYSTEMD_SERVICE:${PN} = "audio-init.service"
SYSTEMD_AUTO_ENABLE = "enable"

FILES:${PN} += "${systemd_system_unitdir}/audio-init.service"

do_install() {
    install -d ${D}${systemd_system_unitdir}
    install -m 0644 ${WORKDIR}/audio-init.service ${D}${systemd_system_unitdir}/audio-init.service
}