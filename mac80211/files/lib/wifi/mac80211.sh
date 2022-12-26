#!/bin/sh
[ -e /lib/functions.sh ] && . /lib/functions.sh
append DRIVERS "mac80211"

MLD_VAP_DETAILS="/tmp/wifi_mld_cfg.txt"

update_mld_vap_details() {
	local _mlds
	local _devices_up
	local _ifaces
	config_load wireless
	mld_vaps_count=0
	radio_up_count=0

	mac80211_get_wifi_mlds() {
		append _mlds $1
	}
	config_foreach mac80211_get_wifi_mlds wifi-mld

	if [ -z "$_mlds" ]; then
		return
	fi

	mac80211_get_wifi_ifaces() {
		append _ifaces $1
	}
	config_foreach mac80211_get_wifi_ifaces wifi-iface

	mac80211_get_active_wifi_devices()  {
		config_get disabled "$1" disabled
		if [ -z "$disabled" ] || [ "$disabled" -eq 0 ]; then
			radio_up_count=$((radio_up_count+1))
		fi
	}
	config_foreach mac80211_get_active_wifi_devices wifi-device

	for _mld in $_mlds
	do
		for _ifname in $_ifaces
		do
			config_get mld_name $_ifname mld

			if [ -n "$mld_name" ] &&  [ "$_mld" = "$mld_name" ]; then
				mld_vaps_count=$((mld_vaps_count+1))
			fi
		done
	done
	echo "radio_up_count=$radio_up_count mld_vaps_count=$mld_vaps_count" > $MLD_VAP_DETAILS
}

pre_wifi_updown() {
	has_updated_cfg=$(ls /var/run/hostapd-*-updated-cfg 2>/dev/null | wc -l)
	if [ "$has_updated_cfg" -gt 1 ]; then
		rm -rf /var/run/hostapd-*updated-cfg
		rm -rf /var/run/hostapd*.conf
	fi
	if [ -f "$MLD_VAP_DETAILS" ]; then
		rm -rf $MLD_VAP_DETAILS
	fi

	update_mld_vap_details
}

post_wifi_updown() {
	:
}

pre_wifi_reload_legacy() {
	:
}

post_wifi_reload_legacy() {
	:
}

pre_wifi_config() {
	:
}

post_wifi_config() {
	:
}

lookup_phy() {
	[ -n "$phy" ] && {
		[ -d /sys/class/ieee80211/$phy ] && return
	}

	# Incase of multiple radios belonging to the same soc, the device path
	# of these radio's would be same. To find the correct phy, we can
	# get the phy index of the device in soc and use it during searching
	# the global phy list
	local radio_idx=${device:5:1}
	local first_phy_idx=0
	local delta=0
	local devpath
	config_get devpath "$device" path
	while :; do
	if [ ${#device} -eq 12 ]; then
		config_get devicepath "radio$radio_idx\_band$first_phy_idx" path
	else
		config_get devicepath "radio$first_phy_idx" path
	fi
	[ -n "$devicepath" -a -n "$devpath" ] || break
	[ "$devpath" == "$devicepath" ] && break
	first_phy_idx=$(($first_phy_idx + 1))
	done

	delta=$(($radio_idx - $first_phy_idx))

	[ -n "$devpath" ] && {
		for phy in $(ls /sys/class/ieee80211 2>/dev/null); do
			case "$(readlink -f /sys/class/ieee80211/$phy/device)" in
			*$devpath)
				if [ $delta -gt 0 ]; then
					delta=$(($delta - 1))
					continue;
				fi
				return;;
			esac
		done
	}

	local macaddr="$(config_get "$device" macaddr | tr 'A-Z' 'a-z')"
	[ -n "$macaddr" ] && {
		for _phy in /sys/class/ieee80211/*; do
			[ -e "$_phy" ] || continue
			[ "$macaddr" = "$(cat ${_phy}/macaddress)" ] || continue
			phy="${_phy##*/}"
			return
		done
	}
	phy=
	return
}

find_mac80211_phy() {
	local device="$1"

	config_get phy "$device" phy
	lookup_phy
	[ -n "$phy" -a -d "/sys/class/ieee80211/$phy" ] || {
		echo "PHY for wifi device $1 not found"
		return 1
	}
	config_set "$device" phy "$phy"

	config_get macaddr "$device" macaddr
	[ -z "$macaddr" ] && {
		config_set "$device" macaddr "$(cat /sys/class/ieee80211/${phy}/macaddress)"
	}

	[ -z "$macaddr" ] && {
		config_set "$device" macaddr "$(cat /sys/class/ieee80211/${phy}/device/net/wlan${phy#phy}/address)"
	}
	return 0
}

check_mac80211_device() {
	config_get phy "$1" phy
	[ -z "$phy" ] && {
		find_mac80211_phy "$1" >/dev/null || return 0
		config_get phy "$1" phy
	}
	[ "$phy" = "$dev" ] && found=1
}

detect_mac80211() {
	if [ $(cat /sys/bus/coresight/devices/coresight-stm/enable) -eq 0 ]
	then
		chipset=$(grep -o "IPQ.*" /proc/device-tree/model | awk -F/ '{print $1}')
		board=$(grep -o "IPQ.*" /proc/device-tree/model | awk -F/ '{print $2}')
		if [ "$chipset" == "IPQ9574" ] && [ "$board" != "AP-AL02-C4" ]; then
			echo 0 > /sys/bus/coresight/devices/coresight-stm/enable
			echo "q6mem" > /sys/bus/coresight/devices/coresight-tmc-etr/out_mode
			echo 1 > /sys/bus/coresight/devices/coresight-tmc-etr/curr_sink
			echo 1 > /sys/bus/coresight/devices/coresight-stm/enable
		fi
	fi
	devidx=0

	config_load wireless

	if [ ! -f "/etc/config/wireless" ] || ! grep -q "enable_smp_affinity" "/etc/config/wireless"; then
		cat <<EOF
config smp_affinity  mac80211
	option enable_smp_affinity	1
	option enable_color		1

EOF
	fi

	while :; do
		config_get type "radio$devidx" type
		[ -n "$type" ] || break
		devidx=$(($devidx + 1))
	done

	#add this delay for empty wifi script issue
	count=0
	while [ $count -le 10 ]
	do
		sleep  1
		if ([ $(ls /sys/class/ieee80211 | wc -l  | grep -w "0") ])
		then
			count=$(( count+1 ))
		else
			sleep 1
			break
		fi
	done

	for _dev in `ls -dv /sys/class/ieee80211/*`; do
		[ -e "$_dev" ] || continue
		dev="${_dev##*/}"
		found=0
		config_foreach check_mac80211_device wifi-device
		[ "$found" -gt 0 ] && continue

		no_sbands=$(iw phy ${dev} info | grep -i 'Band ' | wc -l)
		if [ $no_sbands -gt 1 ]; then
			is_swiphy=1
		fi
		bandidx=0
		for _band in `iw phy ${dev} info | grep -i 'Band ' | cut -d' ' -f 2`; do
			[ ! -z $_band ] || continue

			mode_11n=""
			mode_band="a"
			channel="36"
			htmode=""
			ht_capab=""
			encryption="none"
			security=""

			if [ $is_swiphy ]; then
				expr="iw phy ${dev} info | awk  '/Band ${_band}/{ f = 1; next } /Band /{ f = 0 } f'"
			else
				expr="iw phy ${dev} info"
			fi
			expr_freq="$expr | awk '/Frequencies/,/valid /f'"

			eval $expr_freq | grep -q '5180 MHz' || \
			eval $expr_freq | grep -q '5955 MHz' || { mode_band="g"; channel="11"; }

			(eval $expr_freq | grep -q '5745 MHz' && \
			(eval $expr_freq | grep -q -F '5180 MHz [36] (disabled)')) && { mode_band="a"; channel="149"; }

			eval $expr_freq | grep -q '60480 MHz' && { mode_11n="a"; mode_band="d"; channel="2"; }

			eval $expr | grep -q 'Capabilities:' && htmode=HT20

			eval $expr | grep -q 'Capabilities:' && htmode=HT20
			vht_cap=$(eval $expr | grep -c 'VHT Capabilities')

			[ "$mode_band" = a ] && htmode="VHT80"

			eval $expr_freq | grep -q '5180 MHz' || eval $expr_freq | grep -q '5745 MHz' || {
				eval $expr_freq | grep -q '5955 MHz' && {
					channel="49"; htmode="HE80"; encryption="sae";
					append ht_capab "	option band     3" "$N"
				}
			}

			[ -n $htmode ] && append ht_capab "	option htmode   $htmode" "$N"

			append security "	option encryption  $encryption" "$N"
			if [ $encryption == "sae" ]; then
				append security "	option sae_pwe  1" "$N"
				append security "	option key      0123456789" "$N"
			fi

			if [ -x /usr/bin/readlink -a -h /sys/class/ieee80211/${dev} ]; then
				path="$(readlink -f /sys/class/ieee80211/${dev}/device)"
			else
				path=""
			fi
			if [ -n "$path" ]; then
				path="${path##/sys/devices/}"
				case "$path" in
					platform*/pci*) path="${path##platform/}";;
				esac
				dev_id="        option path     '$path'"
			else
				dev_id="        option macaddr  $(cat /sys/class/ieee80211/${dev}/macaddress)"
			fi
			if [ $is_swiphy ]; then
				devname=radio$devidx\_band$bandidx
				bandidx=$(($bandidx + 1))
			else
				devname=radio$devidx
			fi
			cat <<EOF
config wifi-device  $devname
        option type     mac80211
        option channel  ${channel}
        option hwmode   11${mode_11n}${mode_band}
$dev_id
$ht_capab
        # REMOVE THIS LINE TO ENABLE WIFI:
        option disabled 1

config wifi-iface
        option device   $devname
        option network  lan
        option mode     ap
        option ssid     OpenWrt
$security

EOF
		done

	devidx=$(($devidx + 1))
	done
}

config_mac80211() {
	detect_mac80211
}

# This start_lbd is to check the dual band availability and
# make sure that dual bands (2.4G and 5G) available before
# starting lbd init script.

start_lbd() {
	local band_24g
	local band_5g
	local i=0

	driver=$(lsmod | cut -d' ' -f 1 | grep ath10k_core)

	if [ "$driver" == "ath10k_core" ]; then
		while [ $i -lt 10 ]
		do
			BANDS=$(/usr/sbin/iw dev 2> /dev/null | grep channel | cut -d' ' -f 2 | cut -d'.' -f 1)
			for channel in $BANDS
			do
				if [ "$channel" -le "14" ]; then
					band_24g=1
				elif [ "$channel" -ge "36" ]; then
					band_5g=1
				fi
			done

			if [ "$band_24g" == "1" ] && [ "$band_5g" == "1" ]; then
				/etc/init.d/lbd start
				return 0
			fi
			sleep 1
			i=$(($i + 1))
		done
	fi
	return 0
}

post_mac80211() {
	local action=${1}

	config_get enable_smp_affinity mac80211 enable_smp_affinity 0

	if [ "$enable_smp_affinity" -eq 1 ]; then
		[ -f "/lib/smp_affinity_settings.sh" ] && {
                        . /lib/smp_affinity_settings.sh
                        enable_smp_affinity_wifi
                }
		[ -f "/lib/update_smp_affinity.sh" ] && {
			. /lib/update_smp_affinity.sh
			enable_smp_affinity_wigig
		}
	fi

	case "${action}" in
		enable)
			[ -f "/usr/sbin/fst.sh" ] && {
				/usr/sbin/fst.sh start
			}
			if [ -f "/etc/init.d/lbd" ]; then
				start_lbd &
			fi
		;;
	esac

	chipset=$(grep -o "IPQ.*" /proc/device-tree/model | awk -F/ '{print $1}')
	board=$(grep -o "IPQ.*" /proc/device-tree/model | awk -F/ '{print $2}')
	if [ "$chipset" == "IPQ5018" ]; then
		echo "q6mem" > /sys/bus/coresight/devices/coresight-tmc-etr/out_mode
		echo 1 > /sys/bus/coresight/devices/coresight-tmc-etr/curr_sink
		echo 5 > /sys/bus/coresight/devices/coresight-funnel-mm/funnel_ctrl
		echo 7 >/sys/bus/coresight/devices/coresight-funnel-in0/funnel_ctrl
		echo 1 > /sys/bus/coresight/devices/coresight-stm/enable
	elif [ "$chipset" == "IPQ8074" ] && [ "$board" != "AP-HK10-C2" ]; then
		echo "q6mem" > /sys/bus/coresight/devices/coresight-tmc-etr/out_mode
		echo 1 > /sys/bus/coresight/devices/coresight-tmc-etr/curr_sink
		echo 5 > /sys/bus/coresight/devices/coresight-funnel-mm/funnel_ctrl
		echo 6 > /sys/bus/coresight/devices/coresight-funnel-in0/funnel_ctrl
		echo 1 > /sys/bus/coresight/devices/coresight-stm/enable
	elif [ "$chipset" == "IPQ6018" ] || [ "$chipset" == "IPQ807x" ]; then
		echo "q6mem" > /sys/bus/coresight/devices/coresight-tmc-etr/out_mode
		echo 1 > /sys/bus/coresight/devices/coresight-tmc-etr/curr_sink
		echo 5 > /sys/bus/coresight/devices/coresight-funnel-mm/funnel_ctrl
		echo 6 > /sys/bus/coresight/devices/coresight-funnel-in0/funnel_ctrl
		echo 1 > /sys/bus/coresight/devices/coresight-stm/enable
	elif [ "$chipset" == "IPQ9574" ] && [ "$board" != "AP-AL02-C4" ]; then
                echo 0 > /sys/bus/coresight/devices/coresight-stm/enable
                echo "q6mem" > /sys/bus/coresight/devices/coresight-tmc-etr/out_mode
                echo 1 > /sys/bus/coresight/devices/coresight-tmc-etr/curr_sink
                echo 1 > /sys/bus/coresight/devices/coresight-stm/enable
	fi

	return 0
}

pre_mac80211() {
	local action=${1}

	case "${action}" in
		disable)
			[ -f "/usr/sbin/fst.sh" ] && {
				/usr/sbin/fst.sh set_mac_addr
				/usr/sbin/fst.sh stop
			}
			[ ! -f /etc/init.d/lbd ] || /etc/init.d/lbd stop

			extsta_path=/sys/module/mac80211/parameters/extsta
			[ -e $extsta_path ] && echo 0 > $extsta_path
		;;
	esac
	return 0
}
