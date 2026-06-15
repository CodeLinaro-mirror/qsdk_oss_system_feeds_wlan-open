DESCRIPTION = "NSS WiFi Plugins kernel module"
LICENSE = "ISC"
LIC_FILES_CHKSUM = "file://${COREBASE}/meta/files/common-licenses/${LICENSE};md5=f3b90e78ea0cffb20bf5cca7947a896d"

inherit module

CLEANBROKEN = "1"

FILESPATH =+ "${TOPDIR}/../src/ipq/wlan-open-extns/ath/plugin:"

SRC_URI = "file://qca-wifi-nss-plugins/"

DEPENDS = "virtual/kernel qca-nss-ecm qca-nss-ppe qca-nss-ppe-ds open-mac80211"

S = "${WORKDIR}/qca-wifi-nss-plugins"

PACKAGES += "kernel-module-qca-wifi-nss-plugins"

PACKAGECONFIG ??= ""

PACKAGECONFIG[ath] = ""

WIFI_NSS_MAKE_OPTS += " \
    ${@bb.utils.contains('PACKAGECONFIG', 'ath', 'QCA_WIFI_NSS_PLUGINS_OPEN_PROFILE_ENABLE=y', '', d)} \
    QCA_WIFI_NSS_PLUGINS_ECM_EMESH=y \
    QCA_WIFI_NSS_PLUGINS_ECM_FSE=y \
    QCA_WIFI_NSS_PLUGINS_ECM_NL=y \
    QCA_WIFI_NSS_PLUGINS_ECM_WIFI_CLASSIFIER=y \
    QCA_WIFI_NSS_PLUGINS_MSCS=y \
    QCA_WIFI_NSS_PLUGINS_PPE=y \
    QCA_WIFI_NSS_PLUGINS_PPEDS=y \
    "

EXTRA_CFLAGS += " \
    -I${STAGING_INCDIR}/qca-nss-ecm \
    -I${STAGING_INCDIR}/qca-nss-ppe \
    -I${STAGING_INCDIR}/qca-nss-ppe-ds \
    -I${STAGING_INCDIR}/open-mac80211 \
    -I${STAGING_DIR}/usr/include/ \
    "

MODULE_EXTRA_SYMBOLS = " \
    ${STAGING_INCDIR}/qca-nss-ecm/Module.symvers \
    ${STAGING_INCDIR}/qca-nss-ppe/Module.symvers \
    ${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers \
    ${STAGING_INCDIR}/open-mac80211/Module.symvers \
    "

do_compile() {
    unset LDFLAGS
    make -C "${STAGING_KERNEL_BUILDDIR}" ${WIFI_NSS_MAKE_OPTS} \
        CROSS_COMPILE="${TARGET_PREFIX}" \
        ARCH="${KARCH}" \
        M="${S}" \
        EXTRA_CFLAGS="${EXTRA_CFLAGS}" \
        KBUILD_EXTRA_SYMBOLS="${MODULE_EXTRA_SYMBOLS}" \
        modules
}

do_install() {
    install -d ${D}${base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/qca-wifi-nss-plugins
    install -m 0644 qca-wifi-nss-plugins${KERNEL_OBJECT_SUFFIX} ${D}${base_libdir}/modules/${KERNEL_VERSION}/kernel/drivers/qca-wifi-nss-plugins/
}

KERNEL_MODULE_AUTOLOAD += "qca-wifi-nss-plugins"
