#!/bin/sh

append DRIVERS "mac80211"

MLD_VAP_DETAILS="/lib/netifd/wireless/wifi_mld_cfg.config"

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
		config_get iface_mode $1 mode
		if [ -n "$iface_mode" ] && [[ "$iface_mode" == "ap" ]]; then
			append _ifaces $1
		fi
		}
	config_foreach mac80211_get_wifi_ifaces wifi-iface
	for _mld in $_mlds
	do
		for _ifname in $_ifaces
		do
			config_get mld_name $_ifname mld
			config_get mldevice $_ifname device
			config_get mlcaps  $mldevice mlo_capable
			if ! [[ "$mldevices" =~ "$mldevice" ]]; then
				append mldevices $mldevice
			fi

			if [ -n "$mlcaps" ] && [ $mlcaps -eq 1 ] && \
				[ -n "$mld_name" ] &&  [ "$_mld" = "$mld_name" ]; then
				mld_vaps_count=$((mld_vaps_count+1))
			fi
		done
	done

	for mldev in $mldevices
	do
		config_get disabled "$mldev" disabled
		if [ -z "$disabled" ] || [ "$disabled" -eq 0 ]; then
			radio_up_count=$((radio_up_count+1))
		fi
	done
	echo "radio_up_count=$radio_up_count mld_vaps_count=$mld_vaps_count" > $MLD_VAP_DETAILS
}


check_mac80211_device() {
	local device="$1"
	local path="$2"
	local macaddr="$3"

	[ -n "$found" ] && return 0

	phy_path=
	config_get phy "$device" phy
	json_select wlan
	[ -n "$phy" ] && case "$phy" in
		phy*)
			[ -d /sys/class/ieee80211/$phy ] && \
				phy_path="$(iwinfo nl80211 path "$dev")"
		;;
		*)
			if json_is_a "$phy" object; then
				json_select "$phy"
				json_get_var phy_path path
				json_select ..
			elif json_is_a "${phy%.*}" object; then
				json_select "${phy%.*}"
				json_get_var phy_path path
				json_select ..
				phy_path="$phy_path+${phy##*.}"
			fi
		;;
	esac
	json_select ..
	[ -n "$phy_path" ] || config_get phy_path "$device" path
	[ -n "$path" -a "$phy_path" = "$path" ] && {
		found=1
		return 0
	}

	config_get dev_macaddr "$device" macaddr

	[ -n "$macaddr" -a "$dev_macaddr" = "$macaddr" ] && found=1

	return 0
}


__get_band_defaults() {
	local phy="$1"

	( iw phy "$phy" info; echo ) | awk '
BEGIN {
        bands = ""
}

($1 == "Band" || $1 == "") && band {
        if (channel) {
		mode="NOHT"
		if (ht) mode="HT20"
		if (vht && band != "1:") mode="VHT80"
		if (he) mode="HE80"
		if (he && band == "1:") mode="HE20"
		if (eht && band== "1:") mode="EHT20"
		if (eht && band == "2:") mode="EHT80"
		if (eht && band == "4:") mode="EHT160"
                sub("\\[", "", channel)
                sub("\\]", "", channel)
                bands = bands band channel ":" mode " "
        }
        band=""
}

$1 == "Band" {
        band = $2
        channel = ""
	vht = ""
	ht = ""
	he = ""
	eht = ""
}

$0 ~ "Capabilities:" {
	ht=1
}

$0 ~ "VHT Capabilities" {
	vht=1
}

$0 ~ "HE Iftypes" {
	he=1
}

$0 ~ "EHT Iftypes" {
	eht=1
}

$1 == "*" && $3 == "MHz" && $0 !~ /disabled/ && band && !channel {
        channel = $4
}

END {
        print bands
}'
}

get_band_defaults() {
	local phy="$1"

	for c in $(__get_band_defaults "$phy"); do
		local band="${c%%:*}"
		c="${c#*:}"
		local chan="${c%%:*}"
		c="${c#*:}"
		local mode="${c%%:*}"
		case "$band" in
			1) band=2g;;
			2) band=5g;;
			3) band=60g;;
			4) band=6g;;
			*) band="";;
		esac

		[ -n "$band" ] || continue

		append mode_band $band
		append channel $chan
		append htmode $mode
	done
}

check_devidx() {
	case "$1" in
	radio[0-9]*)
		local idx="${1#radio}"
		[ "$devidx" -ge "${1#radio}" ] && devidx=$((idx + 1))
		;;
	esac
}

check_board_phy() {
	local name="$2"

	json_select "$name"
	json_get_var phy_path path
	json_select ..

	if [ "$path" = "$phy_path" ]; then
		board_dev="$name"
	elif [ "${path%+*}" = "$phy_path" ]; then
		fallback_board_dev="$name.${path#*+}"
	fi
}

pre_mac80211() {
	local action=${1}
	case "${action}" in
		disable)
			if [ -f "$MLD_VAP_DETAILS" ]; then
				rm -rf $MLD_VAP_DETAILS
			fi
		;;
	esac
	return 0
}

detect_mac80211() {
	devidx=0
	config_load wireless
	config_foreach check_devidx wifi-device

	json_load_file /etc/board.json

	for _dev in /sys/class/ieee80211/*; do
		[ -e "$_dev" ] || continue

		dev="${_dev##*/}"

		mode_band=""
		channel=""
		htmode=""
		ht_capab=""
		bandidx=1
		#Check the single wiphy support
		total_bands=$(iw phy ${dev} info | grep -E 'Band ' | wc -l)
		if [ $total_bands -gt 1 ]; then
			is_swiphy=1
		fi

		get_band_defaults "$dev"
		while [ $bandidx -le $total_bands ]
		do
			_mode_band=$(eval echo $mode_band | awk -v I=$bandidx '{print $I}')
			_channel=$(eval echo $channel | awk -v I=$bandidx '{print $I}')
			_htmode=$(eval echo $htmode | awk -v I=$bandidx '{print $I}')

			path="$(iwinfo nl80211 path "$dev")"
			macaddr="$(cat /sys/class/ieee80211/${dev}/macaddress)"

			# work around phy rename related race condition
			[ -n "$path" -o -n "$macaddr" ] || continue

			board_dev=
			fallback_board_dev=
			json_for_each_item check_board_phy wlan
			[ -n "$board_dev" ] || board_dev="$fallback_board_dev"
			[ -n "$board_dev" ] && dev="$board_dev"

			found=
			config_foreach check_mac80211_device wifi-device "$path" "$macaddr"
			[ -n "$found" ] && continue
			if [ $is_swiphy ]; then
				name="radio$devidx\_band$(($bandidx - 1))"
			else
				name="radio${devidx}"
			fi

			case "$dev" in
				phy*)
					if [ -n "$path" ]; then
						dev_id="set wireless.${name}.path='$path'"
					else
						dev_id="set wireless.${name}.macaddr='$macaddr'"
					fi
					;;
				*)
					dev_id="set wireless.${name}.phy='$dev'"
					;;
			esac

			uci -q batch <<-EOF
				set wireless.${name}=wifi-device
				set wireless.${name}.type=mac80211
				${dev_id}
				set wireless.${name}.channel=${_channel}
				set wireless.${name}.band=${_mode_band}
				set wireless.${name}.htmode=$_htmode
				set wireless.${name}.disabled=1

				set wireless.default_${name}=wifi-iface
				set wireless.default_${name}.device=${name}
				set wireless.default_${name}.network=lan
				set wireless.default_${name}.mode=ap
				set wireless.default_${name}.ssid=OpenWrt
				set wireless.default_${name}.encryption=none
	EOF
			uci -q commit wireless
			bandidx=$(($bandidx + 1))
		done
		devidx=$(($devidx + 1))
	done
}
