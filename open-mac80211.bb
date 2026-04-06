SUMMARY = "Linux 802.11 Wireless Networking Stack (mac80211) and Atheros WiFi Drivers"
DESCRIPTION = "Backported wireless drivers from newer kernels including mac80211, cfg80211, ath, ath11k, and ath12k drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

PV = "6.6.15"
PR = "r2"

INHIBIT_PACKAGE_STRIP = "0"
INHIBIT_PACKAGE_DEBUG_SPLIT = "0"

inherit module

MAC80211_PKG_SUBDIR := "backports-${MAC80211_PKG_VERSION}-${LINUX_VERSION}-${MAC80211_PKG_KERNEL_VERSION}"
MAC80211_S := "${KERNEL_BUILD_DIR}"
PKG_BACKPORTS_VERSION := "965f73fc"
PKG_BACKPORTS_BRANCH := "master"
MAC80211_PKG_NAME := "=korg-kvalo/master"
MAC80211_PKG_VERSION := "20250213"
MAC80211_PKG_KERNEL_VERSION := "9a0dddfb3"

SRCPREFIX := "../"
FILESEXTRAPATHS:prepend := "${TOPDIR}/${SRCPREFIX}src/ipq/mac80211/wlan-open/:"

SRC_URI = " \
	git://git.kernel.org/pub/scm/linux/kernel/git/backports/backports.git;protocol=https;branch=${PKG_BACKPORTS_BRANCH};name=backports \
	git://git.codelinaro.org/clo/qsdk/kvalo/ath.git;protocol=https;branch=korg-kvalo/master;name=wlan_open; \
	file://backports-6.1-${MAC80211_PKG_KERNEL_VERSION} \
	file://lib/ \
	file://lib/wifi/ \
	file://lib/netifd-wlan/wireless/ \
	file://ini/ \
	file://ini/internal/ \
"

SRCREV_backports = "965f73fc894d42f7cfa9880bbd6bcc671d295f12"
SRCREV_wlan_open = "9a0dddfb30f120db3851627935851d262e4e7acb"

S = "${WORKDIR}/backports-6.1-${MAC80211_PKG_KERNEL_VERSION}"

DEPENDS = " \
	virtual/kernel \
	wireless-regdb \
	iw \
	qca-nss-ppe \
	qca-nss-ppe-vp \
	qca-nss-ppe-ds \
	qca-nss-wifi-plugin \
"

REQUIRED_HOSTTOOLS += "spatch"

RDEPENDS:${PN} = " \
	wireless-regdb \
	iw \
	hostapd \
"

EXTRA_OEMAKE += " \
	KLIB_BUILD=${STAGING_KERNEL_BUILDDIR} \
	KLIB=${D}/${nonarch_base_libdir}/modules/${KERNEL_VERSION}/ \
	MODPROBE=true \
"

EXTRA_MAKE_CFLAGS=" \
	-I${S}/include \
	-I${STAGING_DIR}/usr/include \
	-I${STAGING_INCDIR}/qca-nss-ppe/qca-nss-ppe/drv/ppe_ds/exports/ \
	-I${TOPDIR}/${SRCPREFIX}src/ipq/qca-wifi/telemetry_agent/inc/ \
	-Wno-error=unused-variable -Wno-unused-variable \
"

EXTRA_CFLAGS += " \
	-I${S}/include \
	-I${S}/include/qca-nss-ppe/qca-nss-ppe/drv/ppe_ds/exports/ \
	-I${STAGING_DIR}/usr/include \
	-I${STAGING_INCDIR}/qca-nss-drv \
	-I${STAGING_INCDIR}/qca-nss-ppe \
	-I${STAGING_INCDIR}/qca-nss-clients \
	-Wall \
	-Wno-unused-function \
	-Wno-error=unused-variable -Wno-unused-variable \
"

KCFLAGS += " \
	-I${S}/include \
	-I${S}/include/qca-nss-ppe/drv/ppe_ds/exports/ \
	-I${STAGING_DIR}/usr/include \
	-I${STAGING_INCDIR}/qca-nss-drv \
	-I${STAGING_INCDIR}/qca-nss-ppe \
	-I${STAGING_INCDIR}/qca-nss-wifi-plugin \
	-I${STAGING_INCDIR}/qca-nss-clients \
	-Wall \
	-Wno-error=unused-variable -Wno-unused-variable \
"

PARALLEL_MAKE = "-j ${@oe.utils.cpu_count()}"

MODULE_EXTRA_SYMBOLS ="${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers \
                        ${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers \
                        ${STAGING_INCDIR}/qca-nss-ppe/Module.symvers \
                        ${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers \
"

do_unpack[postfuncs] += "do_cp_src_wlan_open_extns"

do_cp_src_wlan_open_extns() {
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/subsys/src ${S}/net/mac80211/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/ath12k/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/wifi7/src ${S}/drivers/net/wireless/ath/ath12k/wifi7/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/wifi6/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns/wifi6
}

do_configure:prepend() {
	cat > ${S}/.config << 'EOF'
CPTCFG_CFG80211=m
CPTCFG_CFG80211_DEBUGFS=y
CPTCFG_MAC80211=m
CPTCFG_MAC80211_RC_MINSTREL=y
CPTCFG_MAC80211_RC_MINSTREL_HT=y
CPTCFG_MAC80211_RC_MINSTREL_VHT=y
CPTCFG_MAC80211_RC_DEFAULT_MINSTREL=y
CPTCFG_MAC80211_MESH=y
CPTCFG_MAC80211_DEBUGFS=y
CONFIG_QCOM_RPROC_DISABLE_MPD_SUPPORT=y
CPTCFG_ATHDEBUG=y
CPTCFG_DEBUG_FS=y
CPTCFG_MAC80211_LEDS=y
CPTCFG_ATH_COMMON=m
CPTCFG_ATH11K=m
CPTCFG_ATH11K_AHB=m
CPTCFG_ATH11K_PCI=m
CPTCFG_ATH11K_DEBUG=y
CPTCFG_ATH11K_DEBUGFS=y
CPTCFG_ATH11K_TRACING=y
CPTCFG_ATH11K_PKTLOG=y
CPTCFG_ATH12K=m
CPTCFG_ATH12K_AHB=y
CPTCFG_ATH12K_PCI=m
CPTCFG_ATH12K_DEBUG=y
CPTCFG_ATH12K_DEBUGFS=y
CPTCFG_ATH12K_TRACING=y
CPTCFG_ATH12K_SAWF=y
CPTCFG_WLAN_VENDOR_ATH=y
CPTCFG_NL80211_TESTMODE=y
CPTCFG_CFG80211_CERTIFICATION_ONUS=y
CPTCFG_MAC80211_DS_SUPPORT=y
CPTCFG_ATH12K_PPE_DS_SUPPORT=y
CPTCFG_MAC80211_SFE_SUPPORT=y
CPTCFG_QCN_EXTN=y
CPTCFG_MAC80211_ATHDEBUG=y
EOF

	yes "" | oe_runmake -C ${S} oldconfig || true
	oe_runmake -C ${S} KCONFIG_CONFIG=${S}/.config olddefconfig
}

#do_patch[postfuncs] += "do_refactor_alloc_cocci"

do_refactor_alloc_cocci() {
	set -e
	SPATCH="spatch"
	COCCI="${WORKDIR}/alloc.cocci"
	WLAN_DIR="${S}/drivers/net/wireless/ath/ath12k"

	if [ ! -f "${COCCI}" ]; then
		bbfatal "Missing semantic patch: ${COCCI}"
	fi
	if [ ! -d "${WLAN_DIR}" ]; then
		bbwarn "ath12k directory not found at ${WLAN_DIR}; skipping refactor"
		return
	fi

	for f in \
		ahb.c ce.c core.c coredump.c debugfs_htt_stats.c dp_htt.c dp_mon.c \
		dp_peer.c dbring.c debugfs.c dp_rx.c mac.c peer.c qmi.c reg.c wmi.c wow.c
	do
		if [ -f "${WLAN_DIR}/${f}" ]; then
			bbnote "spatch: ${f}"
			${SPATCH} --sp-file "${COCCI}" --in-place "${WLAN_DIR}/${f}"
		fi
	done

	for d in wifi7 wifi8; do
		if [ -d "${WLAN_DIR}/${d}" ]; then
			bbnote "spatch: dir ${d}"
			${SPATCH} --sp-file "${COCCI}" --in-place -dir "${WLAN_DIR}/${d}"
		fi
	done
}

do_compile:prepend() {
    # Find all Module.symvers from dependencies
    EXTRA_SYMBOLS=""
    for dep in qca-nss-ppe qca-nss-ppe-vp qca-nss-ppe-ds qca-nss-wifi-plugin; do
        symvers=$(find ${STAGING_DIR} -path "*/${dep}/Module.symvers" 2>/dev/null | head -1)
        if [ -f "$symvers" ]; then
            EXTRA_SYMBOLS="${EXTRA_SYMBOLS} $symvers"
        fi
    done

    if [ -n "$EXTRA_SYMBOLS" ]; then
        export KBUILD_EXTRA_SYMBOLS="$EXTRA_SYMBOLS"
    fi
}

MAKE_OPTS:= " \
	EXTRA_CFLAGS='${EXTRA_CFLAGS}' \
	KLIB_BUILD="${STAGING_KERNEL_BUILDDIR}" \
	MODPROBE=true \
	KLIB=${D}/${nonarch_base_libdir}/modules/${KERNEL_VERSION}/ \
	KBUILD_LDFLAGS_MODULE_PREREQ= \
	KBUILD_EXTRA_SYMBOLS='${STAGING_INCDIR}/qca-nss-ppe/Module.symvers ${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers ${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers ${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers' \
"
do_compile() {
	${MAKE} ${MAKE_OPTS} modules
}

do_install() {
	oe_runmake -C ${STAGING_KERNEL_DIR} \
		M=${S} \
		ARCH=${ARCH} \
		CROSS_COMPILE=${TARGET_PREFIX} \
		INSTALL_MOD_PATH=${D} \
		modules_install
}

do_install:append() {
	install -d ${D}/ini
	install -d ${D}/ini/internal/

	rm -f ${D}${includedir}/mac80211-backport/linux/module.h

	install -d ${D}${includedir}/mac80211
	install -d ${D}${includedir}/mac80211-backport
	install -d ${D}${includedir}/mac80211/ath
	install -d ${D}${includedir}/net/mac80211

	cp -r ${WORKDIR}/ini/* ${D}/ini/
	cp -r ${WORKDIR}/ini/internal/* ${D}/ini/internal/

	cp -r ${S}/net/mac80211/*.h ${D}${includedir}/mac80211/
	cp -r ${S}/include/* ${D}${includedir}/mac80211/
	cp -r ${S}/backport-include/* ${D}${includedir}/mac80211-backport/
	cp ${S}/net/mac80211/rate.h ${D}${includedir}/net/mac80211/
	cp ${S}/drivers/net/wireless/ath/*.h ${D}${includedir}/mac80211/ath/

	if [ -f ${S}/drivers/net/wireless/ath/ath12k/vendor.h ]; then
		cp ${S}/drivers/net/wireless/ath/ath12k/vendor.h ${D}${includedir}/mac80211/ath/
	fi

	if [ -f ${S}/Module.symvers ]; then
		install -d ${D}${includedir}/mac80211
		cp ${S}/Module.symvers ${D}${includedir}/mac80211/
	fi

	if [ -f ${S}/include/ath/ath_sawf.h ]; then
		install -d ${D}${includedir}/ath
		cp ${S}/include/ath/ath_sawf.h ${D}${includedir}/ath/
		cp ${S}/include/ath/ath_fse.h ${D}${includedir}/ath/
		cp ${S}/include/ath/ath_dp_accel_cfg.h ${D}${includedir}/ath/
		cp ${S}/include/ath/ppe_public.h ${D}${includedir}/ath/
	fi
}

# Prevent automatic kernel module dependency detection
SKIP_FILEDEPS = "1"

PACKAGES =+ " \
	${PN}-cfg80211 \
	${PN}-mac80211 \
	${PN}-ath \
	${PN}-ath11k \
	${PN}-ath12k \
	${PN}-scripts \
"
FILES:${PN} = ""

FILES:${PN}-cfg80211 = " \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/compat/compat.ko \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/net/wireless/cfg80211.ko \
"

FILES:${PN}-mac80211 = " \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/net/mac80211/mac80211.ko \
"

FILES:${PN}-ath = " \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/drivers/net/wireless/ath/ath.ko \
"

FILES:${PN}-ath11k = " \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/drivers/net/wireless/ath/ath11k/*.ko \
	${nonarch_base_libdir}/firmware/ath11k/* \
"

FILES:${PN}-ath12k = " \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/drivers/net/wireless/ath/ath12k/*.ko \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/drivers/net/wireless/ath/ath12k/ath_debug/ath_debug.ko \
	${nonarch_base_libdir}/modules/${KERNEL_VERSION}/*/drivers/net/wireless/ath/ath12k/wifi7/ath12k_wifi7.ko \
	${nonarch_base_libdir}/firmware/ath12k/* \
	${nonarch_base_libdir}/firmware/qcn9224 \
"

FILES:${PN}-dev = " \
	${includedir}/mac80211/* \
	${includedir}/mac80211-backport/* \
	${includedir}/net/mac80211/* \
	${includedir}/ath/* \
"

# Runtime dependencies
RDEPENDS:${PN}-cfg80211 = "wireless-regdb"
RDEPENDS:${PN}-mac80211 = "${PN}-cfg80211"
RDEPENDS:${PN}-ath = "${PN}-mac80211"
RDEPENDS:${PN}-ath11k = "${PN}-ath"
RDEPENDS:${PN}-ath12k = "${PN}-ath"
RDEPENDS:${PN}-scripts = "bash"

# Provide, replace, and conflict with kernel-provided modules
RPROVIDES:${PN}-cfg80211 += "kernel-module-cfg80211 kernel-module-cfg80211-${KERNEL_VERSION} kernel-module-compat kernel-module-compat-${KERNEL_VERSION}"
RREPLACES:${PN}-cfg80211 += "kernel-module-cfg80211"
RCONFLICTS:${PN}-cfg80211 += "kernel-module-cfg80211"

RPROVIDES:${PN}-mac80211 += "kernel-module-mac80211 kernel-module-mac80211-${KERNEL_VERSION}"
RREPLACES:${PN}-mac80211 += "kernel-module-mac80211"
RCONFLICTS:${PN}-mac80211 += "kernel-module-mac80211"

RPROVIDES:${PN}-ath += "kernel-module-ath kernel-module-ath-${KERNEL_VERSION}"
RREPLACES:${PN}-ath += "kernel-module-ath"
RCONFLICTS:${PN}-ath += "kernel-module-ath"

RPROVIDES:${PN}-ath11k += "kernel-module-ath11k kernel-module-ath11k-${KERNEL_VERSION} kernel-module-ath11k-ahb kernel-module-ath11k-ahb-${KERNEL_VERSION} kernel-module-ath11k-pci kernel-module-ath11k-pci-${KERNEL_VERSION}"
RREPLACES:${PN}-ath11k += "kernel-module-ath11k"
RCONFLICTS:${PN}-ath11k += "kernel-module-ath11k"

RPROVIDES:${PN}-ath12k += "kernel-module-ath12k kernel-module-ath12k-${KERNEL_VERSION} kernel-module-ath-debug kernel-module-ath-debug-${KERNEL_VERSION} kernel-module-ath12k-ahb kernel-module-ath12k-ahb-${KERNEL_VERSION} kernel-module-ath12k-pci kernel-module-ath12k-pci-${KERNEL_VERSION} kernel-module-ath12k-wifi7 kernel-module-ath12k-wifi7-${KERNEL_VERSION}"
RREPLACES:${PN}-ath12k += "kernel-module-ath12k"
RCONFLICTS:${PN}-ath12k += "kernel-module-ath12k"

RCONFLICTS:${PN} = "linux-backports"

# Autoload modules in dependency order
KERNEL_MODULE_AUTOLOAD:${PN}-cfg80211 = "compat cfg80211"
KERNEL_MODULE_AUTOLOAD:${PN}-mac80211 = "mac80211"
KERNEL_MODULE_AUTOLOAD:${PN}-ath = "ath"
KERNEL_MODULE_AUTOLOAD:${PN}-ath11k = "ath11k ath11k_ahb ath11k_pci"
KERNEL_MODULE_AUTOLOAD:${PN}-ath12k = "ath12k ath_debug ath12k_wifi7"

# Set regulatory domain
KERNEL_MODULE_PROBECONF += "cfg80211"
module_conf_cfg80211 = "options cfg80211 ieee80211_regdom=US"

# Make the main package a meta-package that pulls in all sub-packages
ALLOW_EMPTY:${PN} = "1"
ALLOW_EMPTY:${PN}-dev = "1"

RDEPENDS:${PN} += " \
    ${PN}-cfg80211 \
    ${PN}-mac80211 \
    ${PN}-ath \
    ${PN}-ath11k \
    ${PN}-ath12k \
"

FILES:${PN} += "/ini/* /ini/internal/*"

COMPATIBLE_MACHINE = "(ipq807x|ipq60xx|ipq50xx|ipq95xx|ipq53xx|ipq54xx|sdx85)"

EXTRA_OEMAKE += "V=1"
PARALLEL_MAKEINST = ""
PACKAGE_ARCH = "${MACHINE_ARCH}"
