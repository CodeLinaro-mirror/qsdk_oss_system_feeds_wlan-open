#!/bin/sh /etc/rc.common

#Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
#SPDX-License-Identifier: ISC

START=98
STOP=02

regenerate_wireless_defaults()
{
	# ath12k_wifi8 loads long after the one-shot early-boot
	# networklayout.json/ODL-defaults generation (uci-defaults 98/99, which
	# self-delete after their first run) already ran and captured zero
	# radios. Re-derive both using the same library/renderer those scripts
	# used, now that the phy actually exists, and nudge pwhm to reload.
	. /usr/share/libubox/jshn.sh
	. /lib/functions.sh
	. /lib/functions/netlayout_from_uci.sh

	NETLAYOUT=/etc/networklayout.json
	# json_dump -i pretty-prints, so an empty array spans multiple lines
	# ("Radios": [\n\n]) -- flatten whitespace first or a single-line grep
	# never matches and this check silently always no-ops.
	case "$(tr -d ' \t\n' < "$NETLAYOUT" 2>/dev/null)" in
	*'"Radios":[]'*) ;;
	*) return 0 ;;
	esac

	brLan="br-lan"
	brGuest="br-guest"
	brLcm="br-lcm"

	netlayout_init
	netlayout_add_wan_itf
	netlayout_add_wwan_itf
	netlayout_add_mxl_updk_pon_itf
	netlayout_add_lan_bridged_itfs
	netlayout_add_lan_dsa_bridged_itfs

	# /etc/config/wireless gets its wifi-device/wifi-iface sections from the
	# async ieee80211 hotplug handler (01-wifi-detect/10-wifi-detect -> wifi
	# config), which races independently of this script. Wait for UCI to
	# actually catch up to the detected phy count before parsing it, or we
	# just recreate the same empty-radios race one level down.
	wait_for_uci_wireless
	netlayout_add_wireless_itfs
	netlayout_discover_all_sfpcages
	json_dump -i > "$NETLAYOUT"

	for ODLTMPL in $(find /etc/amx /usr/lib -name "*.odl.uc"); do
		TARGETODL=$(echo "$ODLTMPL" | sed "s|\.odl\.uc|.odl|")
		ucode -E BD="$NETLAYOUT" \
			-e '{"ODL_UC_PATH":"'"$ODLTMPL"'"}' \
			-i /etc/amx/process_odl_templates.uc > "$TARGETODL"
	done

	[ -x /etc/init.d/prplmesh_whm ] && /etc/init.d/prplmesh_whm restart
}

boot()
{
	[ -e /sys/class/remoteproc/remoteproc0/state ] || return 0

	# Blocks boot (pwhm's init is ordered to START after this script) so
	# pwhm's radio detection runs only once the phy actually exists.
	count=0
	while [ "$(cat /sys/class/remoteproc/remoteproc0/state 2>/dev/null)" != "running" ]; do
		count=$((count + 1))
		if [ $count -ge 120 ]; then
			echo "ath12k_wifi8_deferred_load: remoteproc0 not running after 120s, aborting" > /dev/console
			return 1
		fi
		sleep 1
	done
	/sbin/modprobe ath12k_wifi8
	/sbin/modprobe qca-wifi-nss-plugins

	count=0
	while [ -z "$(ls /sys/class/ieee80211 2>/dev/null)" ]; do
		count=$((count + 1))
		[ $count -ge 30 ] && break
		sleep 1
	done

	regenerate_wireless_defaults
}
