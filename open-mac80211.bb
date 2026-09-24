SUMMARY = "Linux 802.11 Wireless Networking Stack (mac80211) and Atheros WiFi Drivers"
DESCRIPTION = "Backported wireless drivers from newer kernels including mac80211, cfg80211, ath, ath11k, and ath12k drivers"
LICENSE = "GPL-2.0-only"
LIC_FILES_CHKSUM = "file://COPYING;md5=6bc538ed5bd9a7fc9398086aedcd7e46"

PV = "6.6.15"
PR = "r2"

INHIBIT_PACKAGE_STRIP = "0"
INHIBIT_PACKAGE_DEBUG_SPLIT = "0"

inherit module deploy

MAC80211_PKG_SUBDIR := "backports-${MAC80211_PKG_VERSION}-${LINUX_VERSION}-${MAC80211_PKG_KERNEL_VERSION}"
MAC80211_S := "${KERNEL_BUILD_DIR}"
MAC80211_PKG_VERSION := "20250213"
MAC80211_PKG_KERNEL_VERSION := "9a0dddfb3"

SRCPREFIX := "../"
python () {
    import os
    topdir = d.getVar("TOPDIR")
    srcprefix = d.getVar("SRCPREFIX") or ""
    if os.path.isdir(os.path.join(topdir, srcprefix, "src/ipq/mac80211/wlan-open")):
        d.setVar("WLAN_SRCPREFIX", "src/ipq/")
    else:
        d.setVar("WLAN_SRCPREFIX", "qca/src/")
}
FILESEXTRAPATHS:prepend := "${THISDIR}/wifi-scripts/files/lib/functions/:${THISDIR}/:${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}mac80211/wlan-open/:"

SRC_URI = " \
	file://backports-6.1-${MAC80211_PKG_KERNEL_VERSION} \
	file://lib/ \
	file://lib/wifi/ \
	file://lib/netifd-wlan/wireless/ \
	file://ini/ \
	file://ini/internal/ \
	file://etc/modprobe.d/ \
	file://rdk_init_helper.sh \
"

SRC_URI:append:echo = " file://etc/udev/rules.d/60-ath12k-no-autoload.rules"
SRC_URI:append:echo = " file://lib/systemd/system/ath12k-wifi8-load.service"

S = "${WORKDIR}/backports-6.1-${MAC80211_PKG_KERNEL_VERSION}"

DEPENDS = " \
	virtual/kernel \
	wireless-regdb \
	iw \
	qca-nss-ppe \
	qca-nss-ppe-vp \
	qca-nss-ppe-ds \
	qca-nss-wifi-plugin \
	qca-debug-uio \
"

DEPENDS:remove:echo = "qca-nss-ppe qca-nss-ppe-vp qca-nss-ppe-ds qca-nss-wifi-plugin"
DEPENDS:append:echo = " dataipa"

REQUIRED_HOSTTOOLS += "spatch"

RDEPENDS:${PN} = " \
	wireless-regdb-static \
	iw \
"

TARGET_CFLAGS:append:echo = " -DPLATFORM_SDX"
TARGET_CFLAGS:append:echo = " -DATH12K_CMA_SUPPORT"
TARGET_CFLAGS:append = "${@' -DATH12K_CMA_SUPPORT' if d.getVar('MACHINE').startswith(('ipq96xx', 'ipq52xx')) else ''}"

EXTRA_MAKE_CFLAGS=" \
	-I${S}/include \
	-I${STAGING_DIR}/usr/include \
	-I${STAGING_INCDIR}/qca-nss-ppe/qca-nss-ppe/drv/ppe_ds/exports/ \
	-I${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}qca-wifi/telemetry_agent/inc/ \
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
	-I${STAGING_INCDIR}/ \
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

KCFLAGS:append:echo = " \
	-I${STAGING_INCDIR}/ \
	-I${TOPDIR}/${SRCPREFIX}src/dataipa/drivers/platform/msm/include/ \
	-I${TOPDIR}/${SRCPREFIX}src/dataipa/drivers/platform/msm/include/uapi \
"

MODULE_EXTRA_SYMBOLS ="${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers \
						${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers \
						${STAGING_INCDIR}/qca-nss-ppe/Module.symvers \
						${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers \
						${STAGING_INCDIR}/qca-debug-uio/Module.symvers \
"

do_unpack[postfuncs] += "do_cp_src_wlan_open_extns do_cp_headers"

do_cp_src_wlan_open_extns() {
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/subsys/src ${S}/net/mac80211/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/ath/ath12k/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/ath/wifi7/src ${S}/drivers/net/wireless/ath/ath12k/wifi7/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/ath/wifi6/src ${S}/drivers/net/wireless/ath/ath12k/qcn_extns/wifi6
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/ath/wifi8/src ${S}/drivers/net/wireless/ath/ath12k/wifi8/qcn_extns
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/subsys/src/cfg80211_dfs_extn.c ${S}/net/wireless/cfg80211_dfs_extn.c
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/subsys/src/cfg80211_dfs_extn.h ${S}/net/wireless/cfg80211_dfs_extn.h
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/subsys/src/cfg80211_scan_radio_extn.c ${S}/net/wireless/cfg80211_scan_radio_extn.c
	cp -af ${TOPDIR}/${SRCPREFIX}${WLAN_SRCPREFIX}wlan-open-extns/subsys/src/cfg80211_scan_radio_extn.h ${S}/net/wireless/cfg80211_scan_radio_extn.h
}

LINUX_SRC_DIR = "${TOPDIR}/${SRCPREFIX}files-6.6"
LINUX_SRC_DIR:echo = "${TOPDIR}/${SRCPREFIX}src/kernel-6.18/kernel_platform/kernel"
QCA_HOSTAP_SRC = "${TOPDIR}/${SRCPREFIX}qca/src/network/services/hostapd"
QCA_VENDOR_HEADER = "${QCA_HOSTAP_SRC}/src/common/qca-vendor.h"

do_cp_headers() {
	install -d ${STAGING_DIR}/include/linux/
	install -m 0644 ${LINUX_SRC_DIR}/include/linux/debug_mem_usage.h ${STAGING_DIR}/include/linux/debug_mem_usage.h
	install -m 0644 ${QCA_VENDOR_HEADER} ${S}/drivers/net/wireless/ath/ath11k/qca-vendor.h
	install -m 0644 ${QCA_VENDOR_HEADER} ${S}/drivers/net/wireless/ath/ath12k/qca-vendor.h
}


# Strip _64/_32 suffix from MACHINE so it matches CONFIG_TARGET_* names in ath.mk
SOC_TARGET = "${@d.getVar('MACHINE').split('_64')[0].split('_32')[0] if d.getVar('MACHINE') else d.getVar('BASEMACHINE')}"
POKY_DIR = ""
POKY_DIR:echo = "poky/"
do_configure[vardeps] += "CONFIG_KERNEL_IPQ_MEM_PROFILE SOC_TARGET"

do_configure:prepend:echo() {
	install -d ${S}/include/qca-debug-uio
	install -m 0644 ${STAGING_INCDIR}/qca-debug-uio/debug_uio_public.h \
		${S}/include/qca-debug-uio/debug_uio_public.h
}

def get_mem_cfg_flags(d):
    import re, os
    profile = d.getVar('CONFIG_KERNEL_IPQ_MEM_PROFILE')
    topdir  = d.getVar('TOPDIR')
    srcprefix = d.getVar('SRCPREFIX') or ''
    pokyprefix = d.getVar('POKY_DIR') or ''
    base_machine = d.getVar('BASEMACHINE') or ''
    base = os.path.join(topdir, srcprefix, pokyprefix,
                        'meta-ipq/recipes-kernel/linux/linux-ipq')
    if base_machine == 'echo':
        mem_file = 'ipq_mem_sdx'
    elif profile == '512':
        mem_file = 'ipq_mem_512'
    else:
        mem_file = 'ipq_mem_open'
    mem_cfg = os.path.join(base, mem_file)
    try:
        with open(mem_cfg) as f:
            lines = f.readlines()
    except FileNotFoundError:
        return ""
    result = []
    for line in lines:
        line = line.strip()
        if not line or line.startswith('#'):
            continue
        line = re.sub(r'=m$', '=y', line)
        result.append(line)
    return ' '.join(result)

do_configure:prepend() {
	mem_cfg_dir="${TOPDIR}/${SRCPREFIX}${POKY_DIR}meta-ipq/recipes-kernel/linux/linux-ipq"
	mem_cfg_open="${mem_cfg_dir}/ipq_mem_open"
	mem_cfg_512="${mem_cfg_dir}/ipq_mem_512"
	mem_cfg_sdx="${mem_cfg_dir}/ipq_mem_sdx"

	if [ ! -f "${mem_cfg_open}" ] || [ ! -f "${mem_cfg_512}" ] || [ ! -f "${mem_cfg_sdx}" ]; then
		bberror "Required memory profile config files are missing."
		bberror "Expected file: ${mem_cfg_open}"
		bberror "Expected file: ${mem_cfg_512}"
		bberror "Expected file: ${mem_cfg_sdx}"
		if [ -d "${mem_cfg_dir}" ]; then
			bberror "Directory exists, current contents:" 
			ls -la "${mem_cfg_dir}" || true
		else
			bberror "Directory does not exist: ${mem_cfg_dir}"
		fi
		bbfatal "Missing ipq_mem_open/ipq_mem_512; ensure the meta-ipq config-file change is present in this build environment."
	fi

	bbnote "Found required memory profile config files: ${mem_cfg_open} and ${mem_cfg_512}"

	# Run rdk.mk (which includes ath.mk) to generate CPTCFG_* flags.
	# Pass all CONFIG_PACKAGE_* / CONFIG_* vars from the profile fragment
	# so ath.mk evaluates the same way as in QSDK.
	${MAKE} -C ${THISDIR} -f ${THISDIR}/rdk.mk print-config \
		OUTPUT_CONFIG="${S}/.config" \
		CONFIG_KERNEL_IPQ_MEM_PROFILE="${CONFIG_KERNEL_IPQ_MEM_PROFILE}" \
		CONFIG_TARGET_${SOC_TARGET}=y \
		${@get_mem_cfg_flags(d)}

	# Set all unrecognised flags to disabled (n), same as QSDK Build/Compile
	${MAKE} ${MAKE_OPTS} KLIB_BUILD="${STAGING_KERNEL_BUILDDIR}" allnoconfig
}

do_patch[postfuncs] += "do_refactor_alloc_cocci"

do_refactor_alloc_cocci() {
	set -e
	if [ "${DEBUG_PROFILE}" = "true" ] || [ "${BASEMACHINE}" = "echo" ]; then
		bbwarn "Skipping do_refactor_alloc_cocci: DEBUG_PROFILE=true enables CONFIG_DEBUG_MEM_USAGE=y which redefines kzalloc to a 2-arg wrapper, incompatible with the 3-arg cocci transformation"
		return
	fi
	SPATCH="spatch"
	COCCI="${WORKDIR}/alloc.cocci"
	WLAN_DIR="${S}/drivers/net/wireless/ath/ath12k"

	cp -af ${THISDIR}/alloc.cocci ${COCCI}

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

OPEN_MAC80211_KBUILD_EXTRA_SYMBOLS = "${STAGING_INCDIR}/qca-nss-ppe/Module.symvers \
	${STAGING_INCDIR}/qca-nss-ppe-ds/Module.symvers \
	${STAGING_INCDIR}/qca-nss-ppe-vp/Module.symvers \
	${STAGING_INCDIR}/qca-nss-wifi-plugin/Module.symvers \
	${STAGING_INCDIR}/qca-debug-uio/Module.symvers \
"

OPEN_MAC80211_KBUILD_EXTRA_SYMBOLS:echo ="\
	${STAGING_INCDIR}/dataipa/Module.symvers \
	${STAGING_INCDIR}/qca-debug-uio/Module.symvers \
"

MAKE_OPTS = " \
	EXTRA_CFLAGS='${EXTRA_CFLAGS}' \
	KCFLAGS="${KCFLAGS}" \
	KLIB_BUILD="${STAGING_KERNEL_BUILDDIR}" \
	MODPROBE=true \
	KLIB=${D}/${nonarch_base_libdir}/modules/${KERNEL_VERSION}/ \
	KBUILD_LDFLAGS_MODULE_PREREQ= \
	KBUILD_EXTRA_SYMBOLS='${OPEN_MAC80211_KBUILD_EXTRA_SYMBOLS}' \
"

MODULE_EXTRA_SYMBOLS:echo = "${STAGING_INCDIR}/qca-debug-uio/Module.symvers"
ATH12K_FW_FILES:echo = "Data.msc amss.bin aux.bin board-2.bin fw_ini_cfg.bin m3.bin \
			mcss.bin qdss_trace_config.bin"

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

do_install:append:echo() {
	install -d ${D}${includedir}/ipa/wifi8/
	if [ -f ${S}/drivers/net/wireless/ath/ath12k/wifi8/qcn_extns/ipa/dp_ipa_fse.h ]; then
		cp ${S}/drivers/net/wireless/ath/ath12k/wifi8/qcn_extns/ipa/dp_ipa_fse.h ${D}${includedir}/ipa/wifi8/
	fi
	install -d ${D}/lib/firmware/ath12k/QCN9625/hw1.0
	install -d ${D}/lib/firmware/ath12k/QCN9589/hw1.0
	install -d ${D}/lib/firmware/qcn9625
	install -d ${D}/lib/firmware/qcn9589
	for f in ${ATH12K_FW_FILES}; do
		ln -snf /firmware/image/qcn9625/$f ${D}/lib/firmware/ath12k/QCN9625/hw1.0/$f
		ln -snf /firmware/image/qcn9589/$f ${D}/lib/firmware/ath12k/QCN9589/hw1.0/$f
	done

	install -d ${D}${sysconfdir}/udev/rules.d
	install -m 0644 ${WORKDIR}/etc/udev/rules.d/60-ath12k-no-autoload.rules \
		${D}${sysconfdir}/udev/rules.d/60-ath12k-no-autoload.rules

	install -d ${D}${systemd_unitdir}/system
	install -m 0644 ${WORKDIR}/lib/systemd/system/ath12k-wifi8-load.service \
		${D}${systemd_unitdir}/system/ath12k-wifi8-load.service
	install -d ${D}${systemd_unitdir}/system/multi-user.target.wants
	ln -sf ../ath12k-wifi8-load.service \
		${D}${systemd_unitdir}/system/multi-user.target.wants/ath12k-wifi8-load.service
}

do_deploy() {
}

do_deploy:append:echo() {
	install -d ${DEPLOYDIR}/kernel_modules/${PN}
	for kmod in $(find ${S} -name '*.ko' ! -name 'ath12k_wifi7.ko'); do
		install -m 0644 $kmod ${DEPLOYDIR}/kernel_modules/${PN}
	done
}

addtask deploy before do_build after do_install

do_install:append() {
	install -d ${D}/ini
	install -d ${D}/ini/internal/

	install -d ${D}${includedir}/open-mac80211
	install -d ${D}${includedir}/mac80211
	install -d ${D}${includedir}/mac80211-backport
	install -d ${D}${includedir}/mac80211/ath
	install -d ${D}${includedir}/net/mac80211
	install -d ${D}${sysconfdir}/modprobe.d
	install -d ${STAGING_DIR}/usr/
	install -d ${STAGING_DIR}/usr/include
	install -d ${STAGING_DIR}/usr/include/mac80211
	install -d ${STAGING_DIR}/usr/include/mac80211/ath

	cp -r ${WORKDIR}/ini/* ${D}/ini/
	cp -r ${WORKDIR}/ini/internal/* ${D}/ini/internal/

	if [ "${BASEMACHINE}" = "echo" ]; then
		install -d ${D}${nonarch_base_libdir}/firmware/internal
		for f in ${D}/ini/*.ini; do
			[ -f "$f" ] || continue
			b="$(basename "$f")"
			ln -sfn "/ini/$b" "${D}${nonarch_base_libdir}/firmware/$b"
		done

		for f in ${D}/ini/internal/*.ini; do
			[ -f "$f" ] || continue
			b="$(basename "$f")"
			ln -sfn "/ini/internal/$b" "${D}${nonarch_base_libdir}/firmware/internal/$b"
		done
	fi
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

	if [ -f ${S}/drivers/net/wireless/ath/ath12k/qca-vendor.h ]; then
		cp ${S}/drivers/net/wireless/ath/ath12k/qca-vendor.h ${D}${includedir}/mac80211/ath/
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

	# Remove ath12k_wifi7.ko from the rootfs for echo
	if [ "${BASEMACHINE}" = "echo" ]; then
		rm -f ${D}/lib/modules/${KERNEL_VERSION}/ath12k_wifi7.ko
	fi

	install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k.conf ${D}${sysconfdir}/modprobe.d/ath12k.conf
	install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k_wifi6.conf ${D}${sysconfdir}/modprobe.d/ath12k_wifi6.conf
	if [ "${BASEMACHINE}" != "echo" ]; then
		install -m 0644 ${WORKDIR}/etc/modprobe.d/ath12k_wifi8.conf ${D}${sysconfdir}/modprobe.d/ath12k_wifi8.conf
	fi
	install -m 0755 ${WORKDIR}/lib/boost_performance.sh ${D}${nonarch_base_libdir}/boost_performance.sh

    install -d ${D}/lib/functions
    install -m 0755 ${WORKDIR}/rdk_init_helper.sh ${D}/lib/functions/rdk_init_helper.sh
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
FILES:${PN}-firmware-ath12k:append:echo = " \
	${nonarch_base_libdir}/firmware/qcn9625 \
	${nonarch_base_libdir}/firmware/qcn9589 \
"

FILES:${PN}-dev = " \
	${includedir}/mac80211/* \
	${includedir}/mac80211-backport/* \
	${includedir}/net/mac80211/* \
	${includedir}/ath/* \
"

FILES:${PN}-dev:append:echo = " ${includedir}/ipa/*"

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

RDEPENDS:${PN}:remove:echo = " \
	kernel-module-ath11k \
	kernel-module-ath11k-ahb \
	kernel-module-ath11k-pci \
	${PN}-firmware-ath11k \
	kernel-module-ath12k-wifi7 \
"

FILES:${PN} += "/ini/* /ini/internal/*"
FILES:${PN} += "/lib/functions/rdk_init_helper.sh"

FILES:${PN} += "${sysconfdir}/modprobe.d/ath12k.conf"
FILES:${PN} += "${sysconfdir}/modprobe.d/ath12k_wifi8.conf"
FILES:${PN} += "${sysconfdir}/modprobe.d/ath12k_wifi6.conf"
FILES:${PN}:append:echo = " ${sysconfdir}/udev/rules.d/60-ath12k-no-autoload.rules"
FILES:${PN}:append:echo = " ${systemd_unitdir}/system/ath12k-wifi8-load.service"
FILES:${PN}:append:echo = " ${systemd_unitdir}/system/multi-user.target.wants/ath12k-wifi8-load.service"
FILES:${PN} += "${nonarch_base_libdir}/boost_performance.sh"
FILES:${PN}:append:echo = " \
      ${nonarch_base_libdir}/firmware \
      ${nonarch_base_libdir}/firmware/* \
      ${nonarch_base_libdir}/firmware/internal \
      ${nonarch_base_libdir}/firmware/internal/* \
  "

FILES:${PN}-dev += "${includedir}/open-mac80211/*"
FILES:kernel-module-ath12k-wifi8 += "${sysconfdir}/modprobe.d/ath12k_wifi8.conf"
FILES:kernel-module-ath12k-wifi6 += "${sysconfdir}/modprobe.d/ath12k_wifi6.conf"

COMPATIBLE_MACHINE = "(ipq807x|ipq60xx|ipq50xx|ipq95xx|ipq53xx|ipq54xx|ipq52xx|ipq96xx|sdx85|echo)"

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
KERNEL_MODULE_AUTOLOAD:remove:echo = "ath12k_wifi7"
KERNEL_MODULE_AUTOLOAD:append = " ath12k_wifi8"
KERNEL_MODULE_AUTOLOAD:remove:echo = "ath12k_wifi8"

# Configure modprobe options using module_conf (same pattern as reference)
module_conf_cfg80211 = "options cfg80211 ieee80211_regdom=US"
module_conf_ath12k = "options ath12k dyndbg=+p debug_mask=0x60"

# Register modules that have configuration
KERNEL_MODULE_PROBECONF += "cfg80211 ath12k"

# Conflict with linux-backports
RCONFLICTS:${PN} = "linux-backports"
