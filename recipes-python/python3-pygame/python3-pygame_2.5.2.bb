SUMMARY = "Python game development library"
HOMEPAGE = "https://www.pygame.org"
LICENSE = "LGPL-2.1-only"
LIC_FILES_CHKSUM = "file://docs/LGPL.txt;md5=7fbc338309ac38fefcd64b04bb903e34"

SRC_URI = "https://files.pythonhosted.org/packages/source/p/pygame/pygame-2.5.2.tar.gz \
           file://Setup"
SRC_URI[sha256sum] = "c1b89eb5d539e7ac5cf75513125fb5f2f0a2d918b1fd6e981f23bf0ac1b1c24a"
SRC_URI[md5sum] = "bf45bc5288fa244a0dde60095bf4afb8"

S = "${WORKDIR}/pygame-2.5.2"

DEPENDS = " \
    libsdl2 \
    libsdl2-native \
    libsdl2-image \
    libsdl2-mixer \
    libsdl2-ttf \
    libsdl2-net \
    freetype \
    freetype-native \
    python3 \
    python3-native \
    pkgconfig-native \
"

inherit python3-dir setuptools3 pkgconfig

do_compile:prepend() {
    cp ${WORKDIR}/Setup ${S}/Setup
    export SDL_CONFIG="${STAGING_BINDIR_NATIVE}/sdl2-config"
    export PYGAME_DETECT_AVX2=1
    export CFLAGS="${CFLAGS} -mfpu=neon -mfloat-abi=hard"
}

RDEPENDS:${PN} = "python3"