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
MAC80211_PKG_VERSION := "20250213"
MAC80211_PKG_KERNEL_VERSION := "9a0dddfb3"

SRCPREFIX := "../"
FILESEXTRAPATHS:prepend := "${TOPDIR}/${SRCPREFIX}src/ipq/mac80211/wlan-open/:"

SRC_URI = " \
	file://backports-6.1-${MAC80211_PKG_KERNEL_VERSION} \
	file://lib/ \
	file://lib/wifi/ \
	file://lib/netifd-wlan/wireless/ \
	file://ini/ \
	file://ini/internal/ \
	file://etc/modprobe.d/ \
"

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
	wireless-regdb-static \
	iw \
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
	-I${STAGING_DIR}/ \
	-I${STAGING_DIR}/usr/include \
	-I${STAGING_INCDIR}/qca-nss-drv \
	-I${STAGING_INCDIR}/qca-nss-ppe \
	-I${STAGING_INCDIR}/qca-nss-clients \
	-I${STAGING_INCDIR}/qca-nss-ppe-ds/ \
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

MODULE_EXTRA_SYMBOLS ="${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers \
						${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers \
						${STAGING_INCDIR}/qca-nss-ppe/Module.symvers \
						${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers \
"

do_unpack[postfuncs] += "do_cp_src_wlan_open_extns do_cp_headers"

do_cp_src_wlan_open_extns() {
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/subsys/src ${S}/net/mac80211/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/ath12k/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/wifi7/src ${S}/drivers/net/wireless/ath/ath12k/wifi7/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/wifi6/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns/wifi6
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/ath/wifi8/src ${S}/drivers/net/wireless/ath/ath12k/wifi8/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/subsys/src/cfg80211_dfs_extn.c ${S}/net/wireless/cfg80211_dfs_extn.c
	cp -af ${TOPDIR}/${SRCPREFIX}src/ipq/wlan-open-extns/subsys/src/cfg80211_dfs_extn.h ${S}/net/wireless/cfg80211_dfs_extn.h
}

do_cp_headers() {
	install -d ${STAGING_DIR}/include/linux/
	install -m 0644 ${TOPDIR}/${SRCPREFIX}files-6.6/include/linux/debug_mem_usage.h ${STAGING_DIR}/include/linux/debug_mem_usage.h
}

# The following compilation flags are enabled for 1G profile
# When RDK platform introduces new profiles, configure accordingly in do_configure()

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
CPTCFG_ATH_DEBUG=y
CPTCFG_ATH11K=m
CPTCFG_ATH11K_AHB=m
CPTCFG_ATH11K_PCI=m
CPTCFG_ATH11K_DEBUG=y
CPTCFG_ATH11K_DEBUGFS=y
CPTCFG_ATH11K_PKTLOG=y
CPTCFG_ATH12K=m
CPTCFG_ATH12K_PCI=m
CPTCFG_ATH12K_DEBUG=y
CPTCFG_ATH12K_DEBUGFS=y
CPTCFG_ATH12K_SAWF=y
CPTCFG_WLAN_VENDOR_ATH=y
CPTCFG_NL80211_TESTMODE=y
CPTCFG_CFG80211_CERTIFICATION_ONUS=y
CPTCFG_MAC80211_DS_SUPPORT=y
CPTCFG_ATH12K_PPE_DS_SUPPORT=y
CPTCFG_MAC80211_SFE_SUPPORT=y
CPTCFG_QCN_EXTN=y
CPTCFG_MAC80211_ATHDEBUG=y
CPTCFG_ATH_REG_DYNAMIC_USER_REG_HINTS=y
CPTCFG_ATH_USER_REGD=y
CPTCFG_ATH11K_CFR=y
CPTCFG_ATH11K_SMART_ANT_ALG=y
CPTCFG_ATH11K_SPECTRAL=y
CPTCFG_ATH12K_CFR=y
CPTCFG_ATH12K_PKTLOG=y
CPTCFG_ATH12K_POWER_BOOST=y
CPTCFG_ATH12K_SPECTRAL=y
CPTCFG_ATH12K_TX_MONITOR=y
CPTCFG_MAC80211_DEBUG_MENU=y
CPTCFG_MAC80211_HWSIM=m
CPTCFG_MAC80211_MLME_DEBUG=y
CPTCFG_MAC80211_PPE_SUPPORT=y
CPTCFG_MAC80211_PS_DEBUG=y
CPTCFG_MAC80211_STA_DEBUG=y
CPTCFG_MAC80211_VERBOSE_DEBUG=y
CPTCFG_QCN_EXTN_MESH_SUPPORT=y
CPTCFG_WILINK_PLATFORM_DATA=y
CPTCFG_WLAN=y
CPTCFG_WLAN_VENDOR_ADMTEK=y
CPTCFG_WLAN_VENDOR_RSI=y
CPTCFG_WL_TI=y
EOF

	if [[ "${MACHINE}" == ipq53xx* ]]; then
		echo "CPTCFG_ATH12K_POWER_OPTIMIZATION=y" >> ${S}/.config
	fi

	if [[ "${MACHINE}" == ipq53xx* || "${MACHINE}" == ipq54xx* ]]; then
		echo "CPTCFG_ATH12K_AHB=y" >> ${S}/.config
	fi
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

MAKE_OPTS = " \
	EXTRA_CFLAGS='${EXTRA_CFLAGS}' \
	KLIB_BUILD="${STAGING_KERNEL_BUILDDIR}" \
	MODPROBE=true \
	KLIB=${D}/${nonarch_base_libdir}/modules/${KERNEL_VERSION}/ \
	KBUILD_LDFLAGS_MODULE_PREREQ= \
	KBUILD_EXTRA_SYMBOLS='${STAGING_INCDIR}/qca-nss-ppe/Module.symvers ${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers ${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers ${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers' \
"

do_compile[vardepsexclude] += "MODULE_EXTRA_SYMBOLS"

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

	install -d ${D}${includedir}/open-mac80211
	install -d ${D}${includedir}/mac80211
	install -d ${D}${includedir}/mac80211-backport
	install -d ${D}${includedir}/mac80211/ath
	install -d ${D}${includedir}/net/mac80211
	install -d ${D}${sysconfdir}/modprobe.d
	install -d  ${STAGING_DIR}/usr/
	install -d  ${STAGING_DIR}/usr/include
	install -d  ${STAGING_DIR}/usr/include/mac80211
	install -d  ${STAGING_DIR}/usr/include/mac80211/ath

	cp -r ${WORKDIR}/ini/* ${D}/ini/
	cp -r ${WORKDIR}/ini/internal/* ${D}/ini/internal/

	cp -r ${S}/net/mac80211/*.h ${D}${includedir}/mac80211/
	cp -r ${S}/include/* ${D}${includedir}/mac80211/
	cp -r ${S}/backport-include/* ${D}${includedir}/mac80211-backport/
	cp ${S}/net/mac80211/rate.h ${D}${includedir}/net/mac80211/
	cp ${S}/drivers/net/wireless/ath/*.h ${D}${includedir}/mac80211/ath/

	cp -r ${S}/include/ath/*.h ${STAGING_DIR}/usr/include
	cp -r ${S}/include/uapi/linux/*.h ${STAGING_DIR}/usr/include
	install -d ${D}${includedir}/open-mac80211/linux
	install -m 0644 ${S}/include/uapi/linux/nl80211.h ${D}${includedir}/open-mac80211/linux/nl80211.h
	cp ${S}/drivers/net/wireless/ath/ath12k/*.h ${STAGING_DIR}/usr/include/mac80211/ath/
	cp ${S}/Module.symvers ${D}${includedir}/open-mac80211/

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

	# Move all .ko files from updates/ subdirs to KERNEL_VERSION/
	find "${D}/lib/modules/${KERNEL_VERSION}/updates/" -name '*.ko' \
		-exec mv -t "${D}/lib/modules/${KERNEL_VERSION}/" {} +

	# Clean up empty updates/ dir
	rm -rf ${D}/lib/modules/${KERNEL_VERSION}/updates

	install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k.conf ${D}${sysconfdir}/modprobe.d/ath12k.conf
	install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k_wifi8.conf ${D}${sysconfdir}/modprobe.d/ath12k_wifi8.conf
	install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k_wifi6.conf ${D}${sysconfdir}/modprobe.d/ath12k_wifi6.conf

}


# Firmware files need to be in separate packages since they're not .ko files
PACKAGES =+ "${PN}-firmware-ath11k ${PN}-firmware-ath12k ${PN}-scripts"

# Allow firmware packages to be empty if firmware is provided elsewhere
ALLOW_EMPTY:${PN}-firmware-ath11k = "1"
ALLOW_EMPTY:${PN}-firmware-ath12k = "1"

FILES:${PN}-firmware-ath11k = " \
	${nonarch_base_libdir}/firmware/ath11k/* \
"

FILES:${PN}-firmware-ath12k = " \
	${nonarch_base_libdir}/firmware/ath12k/* \
	${nonarch_base_libdir}/firmware/qcn9224 \
"

FILES:${PN}-dev = " \
	${includedir}/mac80211/* \
	${includedir}/mac80211-backport/* \
	${includedir}/net/mac80211/* \
	${includedir}/ath/* \
"

# Runtime dependencies for auto-generated kernel-module-* packages
RDEPENDS:kernel-module-cfg80211 = "wireless-regdb-static"
RDEPENDS:kernel-module-mac80211 = "kernel-module-cfg80211 kernel-module-compat"
RDEPENDS:kernel-module-ath = "kernel-module-mac80211"
RDEPENDS:kernel-module-ath11k = "kernel-module-ath"
RDEPENDS:kernel-module-ath11k-ahb = "kernel-module-ath11k"
RDEPENDS:kernel-module-ath11k-pci = "kernel-module-ath11k"
RDEPENDS:kernel-module-ath12k = "kernel-module-ath"
RDEPENDS:kernel-module-ath-debug = "kernel-module-ath12k"
RDEPENDS:kernel-module-ath12k-wifi7 = "kernel-module-ath12k"
RDEPENDS:kernel-module-ath12k-wifi8 = "kernel-module-ath12k"
RDEPENDS:${PN}-scripts = "bash"


ALLOW_EMPTY:${PN} = "1"
ALLOW_EMPTY:${PN}-dev = "1"

RDEPENDS:${PN} += " \
	kernel-module-compat \
	kernel-module-cfg80211 \
	kernel-module-mac80211 \
	kernel-module-ath \
	kernel-module-ath11k \
	kernel-module-ath11k-ahb \
	kernel-module-ath11k-pci \
	kernel-module-ath12k \
	kernel-module-ath-debug \
	kernel-module-ath12k-wifi7 \
	kernel-module-ath12k-wifi8 \
	${PN}-firmware-ath11k \
	${PN}-firmware-ath12k \
"

FILES:${PN} += "/ini/* /ini/internal/*"

FILES:${PN} += "${sysconfdir}/modprobe.d/ath12k.conf"
FILES:kernel-module-ath12k-wifi8 += "${sysconfdir}/modprobe.d/ath12k_wifi8.conf"
FILES:kernel-module-ath12k-wifi6 += "${sysconfdir}/modprobe.d/ath12k_wifi6.conf"

FILES:${PN}-dev += "${includedir}/open-mac80211/*"

COMPATIBLE_MACHINE = "(ipq807x|ipq60xx|ipq50xx|ipq95xx|ipq53xx|ipq54xx|ipq52xx|ipq96xx|sdx85)"

PARALLEL_MAKEINST = ""
PACKAGE_ARCH = "${MACHINE_ARCH}"

# Use KERNEL_MODULE_AUTOLOAD:append for each module
KERNEL_MODULE_AUTOLOAD:append = " compat"
KERNEL_MODULE_AUTOLOAD:append = " cfg80211"
KERNEL_MODULE_AUTOLOAD:append = " mac80211"
KERNEL_MODULE_AUTOLOAD:append = " ath"
KERNEL_MODULE_AUTOLOAD:append = " ath11k"
KERNEL_MODULE_AUTOLOAD:append = " ath11k_ahb"
KERNEL_MODULE_AUTOLOAD:append = " ath11k_pci"
KERNEL_MODULE_AUTOLOAD:append = " ath12k"
KERNEL_MODULE_AUTOLOAD:append = " ath_debug"
KERNEL_MODULE_AUTOLOAD:append = " ath12k_wifi7"
KERNEL_MODULE_AUTOLOAD:append = " ath12k_wifi8"

# Configure modprobe options using module_conf (same pattern as reference)
module_conf_cfg80211 = "options cfg80211 ieee80211_regdom=US"
module_conf_ath12k = "options ath12k dyndbg=+p debug_mask=0xffffffff"

# Register modules that have configuration
KERNEL_MODULE_PROBECONF += "cfg80211 ath12k"

# Conflict with linux-backports
RCONFLICTS:${PN} = "linux-backports"
