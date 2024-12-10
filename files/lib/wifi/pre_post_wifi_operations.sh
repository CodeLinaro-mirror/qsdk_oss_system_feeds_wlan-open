#!/bin/sh
#
# Copyright (c) 2024 Qualcomm Innovation Center, Inc. All rights reserved.
#
# Permission to use, copy, modify, and/or distribute this software for any
# purpose with or without fee is hereby granted, provided that the above
# copyright notice and this permission notice appear in all copies.
#
# THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
# WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
# MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR
# ANY SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
# WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
# ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF
# OR IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.

append DRIVERS "mac80211"

mac80211_update_mld_iface_config() {
	vif_name=$1
	mld_name=$2
	# Get the following from section wifi-mld
	config_get mld_ssid "$mld_name" ssid
	config_get mld_encryption "$mld_name" encryption
	config_get mld_key "$mld_name" key
	config_get mld_sae "$mld_name" sae_pwe
	config_get mld_vp "$mld_name" ppe_vp
	if [ -n "$mld_ssid" ]; then
		uci_set wireless "$vif_name" ssid "$mld_ssid"
	fi
	if [ -n "$mld_encryption" ]; then
		uci_set wireless "$vif_name" encryption "$mld_encryption"
	fi
	if [ -n "$mld_key" ]; then
		uci_set wireless "$vif_name" key "$mld_key"
	fi
	if [ -n "$mld_sae" ]; then
		uci_set wireless "$vif_name" sae_pwe "$mld_sae"
	fi
	if [ -n "$mld_vp" ]; then
		uci_set wireless "$vif_name" ppe_vp "$mld_vp"
	fi
	uci commit wireless
}

mac80211_update_mld_configs() {
	local iflist
	config_load wireless
	mac80211_update_mld_cfg() {
		append iflist "$1"
	}
	config_foreach mac80211_update_mld_cfg wifi-iface
	for name in $iflist
	do
		config_get mld_name "$name" mld
		config_get ml_device "$name" device
		config_get ht_mode "$ml_device" htmode
		if ([ -n "$ht_mode" ] && [[ "$ht_mode" == "EHT"* ]]  && [ -n "$mld_name" ]); then
			append mld_names "$mld_name"
			mac80211_update_mld_iface_config "$name" "$mld_name"
		fi
	done
}

mlo_add_link() {
	local data
	local link
	local conf_idx
	local ssid
	local encryption
	local sae_pwe
	local key
	local channels
	local mld

	mld=$(uci show wireless | grep "$3" | cut -d "." -f 2)
	[ -n "$mld" ] || {
		echo "wrong interface name is given or mld doesn't found for given interface" > /dev/ttyMSM0
		return
	}

	case "$2" in
		2g)
		channels="1-14"
		;;
		5g)
		channels="36-177"
		;;
		5gl)
		channels="36-64"
		;;
		5gh)
		channels="100-177"
		;;
		6g)
		channels="2-233"
		;;
		6gl)
		channels="2-93"
		;;
		6gh)
		channels="129-233"
		;;
		*) echo "wrong band is given" > /dev/ttyMSM0
		return;;
	esac
	link=$(uci show wireless | grep $channels | cut -d "_" -f 2 | cut -d "." -f 1)
	[ -n "$link" ] || {
		echo "failed to find band number" > /dev/ttyMSM0
		return
	}
	uci add wireless wifi-iface
	conf_idx=$(uci show wireless | sed -n 's/.*@wifi-iface\[\([0-9]\+\)\].*/\1/p' | sort -n | tail -1)
	echo 1 > /tmp/mlo_support.txt&
	ssid=$(uci get wireless."$mld".ssid)
	encryption=$(uci get wireless."$mld".encryption)
	sae_pwe=$(uci get wireless."$mld".sae_pwe)
	key=$(uci get wireless."$mld".key)

	uci set wireless.@wifi-iface[$conf_idx]=wifi-iface
	uci set wireless.@wifi-iface[$conf_idx].device=radio0_$link
	uci set wireless.@wifi-iface[$conf_idx].network='lan'
	uci set wireless.@wifi-iface[$conf_idx].mode='ap'
	uci set wireless.@wifi-iface[$conf_idx].ssid=$(uci get wireless."$mld".ssid)
	uci set wireless.@wifi-iface[$conf_idx].encryption=$(uci get wireless."$mld".encryption)
	uci set wireless.@wifi-iface[$conf_idx].sae_pwe=$(uci get wireless."$mld".sae_pwe)
	uci set wireless.@wifi-iface[$conf_idx].key=$(uci get wireless."$mld".key)
	uci set wireless.@wifi-iface[$conf_idx].mld="$mld"
	uci set wireless.@wifi-iface[$conf_idx].macaddr="$4"
	uci commit wireless
	input_file=/var/run/hostapd-${1}_${link}.conf
	if [ -f $input_file ]; then
		output_file=/tmp/hostapd-${1}_${link}.conf
		if grep -q "bss=" "$input_file"; then
			awk '/bss=/ {exit} {print}' "$input_file" > "$output_file"
		else
			cp "$input_file" "$output_file"
		fi
		echo "wpa_passphrase=$key" >> $output_file
		echo "ssid=$ssid" >> $output_file
		if [ $sae_pwe = 1 ]; then
			echo "sae_pwe=$sae_pwe" >> $output_file
		fi
		if [ $encryption = "sae" ]; then
			echo "wpa_key_mgmt=SAE" >> $output_file
		fi
		echo "bssid=$4" >> $output_file
		echo "interface=$3" >> $output_file
		hostapd_cli -i $3 mld_add_link bss_config=${1}:"$output_file"
		rm "$output_file"
	else
		ubus call network reload
		json_load "$(ubus_wifi_cmd "status" "radio0_${link}")"
		data=$(json_dump)
		data=$(echo "$data" | sed 's/.*\("config": { "path\)/\1/' | sed 's/}$//')
		data=$(echo "$data" | sed '$ s/..$/}/')
		data="{ $data"
		data=$(echo "$data" | sed -e 's/"interfaces": \[/"interfaces": { "0": /' -e 's/\} ]/} }/')
		start_string='"section"'
		end_string='"section": "@wifi-iface['"$conf_idx"']"'
		start_index=$(echo "$data" | awk -v pat="$start_string" 'BEGIN{IGNORECASE=1} index($0,pat) {print index($0,pat)}')
		end_index=$(echo "$data" | awk -v pat="$end_string" 'BEGIN{IGNORECASE=1} index($0,pat) {print index($0,pat)}')
		m_data="${data:0:start_index}${data:end_index}"
		data="$m_data"
		data=$(echo "$data" | sed -e "s/\"section\": \"@wifi-iface\[$conf_idx\]\"/\"bridge\": \"br-lan\", \"bridge_ifname\": \"br-lan\"/")
		data=$(echo "$data" | sed -e 's/\[\ ]/{ }/g' -e 's/"stations"/"stas"/g')
		json_select "radio0_${link}"
		_wdev_handler_1 "$data" "mac80211" "setup" "radio0_$link" 2> /dev/null
		json_select ..
		if [ "6g" = "$2" ]; then
			echo "mbssid=2" >> "$input_file"
			echo "ema=1" >> "$input_file"
		fi
		hostapd_cli -i $3 mld_add_link bss_config=${1}:/var/run/hostapd-${1}_${link}.conf
	fi
	uci set wireless.radio0_${link}.disabled='0'
	uci commit wireless
	rm /tmp/mlo_support.txt 2>/dev/null
}

configure_service_param() {
	enable_service=$2
	phy=$3
	json_load "$1"
	json_get_var svc_id svc_id
	json_get_var disable disable

	[ -z "$disable" ] && disable='0'

	if [ $enable_service -eq 1 ] && [ "$disable" -eq 0 ]; then
		json_get_var min_thruput_rate min_thruput_rate
		json_get_var max_thruput_rate max_thruput_rate
		json_get_var burst_size burst_size
		json_get_var service_interval service_interval
		json_get_var delay_bound delay_bound
		json_get_var msdu_ttl msdu_ttl
		json_get_var priority priority
		json_get_var tid tid
		json_get_var msdu_rate_loss msdu_rate_loss
		json_get_var ul_service_interval ul_service_interval
		json_get_var ul_min_tput ul_min_tput
		json_get_var ul_max_latency ul_max_latency
		json_get_var ul_burst_size ul_burst_size
		json_get_var ul_ofdma_disable ul_ofdma_disable
		json_get_var ul_mu_mimo_disable ul_mu_mimo_disable

		cmd="iw $phy service_class create $svc_id "
		[ ! -z "$min_thruput_rate" ] && cmd=$cmd"min_tput $min_thruput_rate "
		[ ! -z "$max_thruput_rate" ] && cmd=$cmd"max_tput $max_thruput_rate "
		[ ! -z "$burst_size" ] && cmd=$cmd"burst_size $burst_size "
		[ ! -z "$service_interval" ] && cmd=$cmd"service_interval $service_interval "
		[ ! -z "$delay_bound" ] && cmd=$cmd"delay_bound $delay_bound "
		[ ! -z "$msdu_ttl" ] && cmd=$cmd"msdu_ttl $msdu_ttl "
		[ ! -z "$priority" ] && cmd=$cmd"priority $priority "
		[ ! -z "$tid" ] && cmd=$cmd"tid $tid "
		[ ! -z "$msdu_rate_loss" ] && cmd=$cmd"msdu_loss $msdu_rate_loss "
		[ ! -z "$ul_service_interval" ] && cmd=$cmd"ul_service_interval $ul_service_interval "
		[ ! -z "$ul_min_tput" ] && cmd=$cmd"ul_min_tput $ul_min_tput "
		[ ! -z "$ul_max_latency" ] && cmd=$cmd"ul_max_latency $ul_max_latency "
		[ ! -z "$ul_burst_size" ] && cmd=$cmd"ul_burst_size $ul_burst_size "
		[ ! -z "$ul_ofdma_disable" ] && cmd=$cmd"ul_ofdma_disable $ul_ofdma_disable "
		[ ! -z "$ul_mu_mimo_disable" ] && cmd=$cmd"ul_mu_mimo_disable $ul_mu_mimo_disable "

		eval $cmd
	elif [ $enable_service -eq 0 ]; then
		cmd="iw $phy service_class disable $svc_id"
		eval $cmd
	fi
}

configure_service_class() {
	PHY_PATH="/sys/kernel/debug/ieee80211"
	phy_present=false
	if [ -d  $PHY_PATH ]
	then
		for phy in $(ls $PHY_PATH 2>/dev/null); do
			dir_name="$PHY_PATH/$phy/ath12k*"
			for dir in $dir_name; do
				[ -d $dir ] && phy_present=true && break
			done
			[ $phy_present = true ] && break
		done
	fi
	[ $phy_present = false ] && return

	json_init
	json_set_namespace default_ns
	json_load_file /lib/wifi/sawf/def_service_classes.json
	json_select service_class
	json_get_keys svc_class_indexes
	svc_class_index=0
	enable_svc=$1

	svc_class_index_count=$(echo "$svc_class_indexes" | wc -w)
	while [ $svc_class_index -lt $svc_class_index_count ]
	do
		svc_class_json=$(jsonfilter -i /lib/wifi/sawf/def_service_classes.json -e "@.service_class[$svc_class_index]")
		configure_service_param "$svc_class_json" "$enable_svc" "$phy"
		svc_class_index=$((svc_class_index+1))
	done

	json_set_namespace default_ns
	json_load_file /lib/wifi/sawf/service_classes.json
	json_select service_class
	json_get_keys svc_class_indexes
	svc_class_index=0

	svc_class_index_count=$(echo "$svc_class_indexes" | wc -w)
	while [ $svc_class_index -lt $svc_class_index_count ]
	do
		svc_class_json=$(jsonfilter -i /lib/wifi/sawf/service_classes.json -e "@.service_class[$svc_class_index]")
		configure_service_param "$svc_class_json" "$enable_svc" "$phy"
		svc_class_index=$((svc_class_index+1))
	done
}

configure_sla_param() {
	json_load "$1"
	json_get_var svc_id svc_id
	json_get_var disable disable
	json_get_var min_thruput_rate min_thruput_rate
	json_get_var max_thruput_rate max_thruput_rate
	json_get_var burst_size burst_size
	json_get_var service_interval service_interval
	json_get_var delay_bound delay_bound
	json_get_var msdu_ttl msdu_ttl
	json_get_var msdu_rate_loss msdu_rate_loss

	[ -z "$min_thruput_rate" ] && min_thruput_rate='X'
	[ -z "$max_thruput_rate" ] && max_thruput_rate='X'
	[ -z "$burst_size" ] && burst_size='X'
	[ -z "$service_interval" ] && service_interval='X'
	[ -z "$delay_bound" ] && delay_bound='X'
	[ -z "$msdu_ttl" ] && msdu_ttl='X'
	[ -z "$msdu_rate_loss" ] && msdu_rate_loss='X'
	[ -z "$disable" ] && disable='0'

	if [ "$disable" -eq 0 ]; then
		cmd="iw $phy telemetry sla_thershold "$svc_id" "$min_thruput_rate" "$max_thruput_rate" "$burst_size" "$service_interval" "$delay_bound" "$msdu_ttl" "$msdu_rate_loss""
		echo "$svc_id" "$min_thruput_rate" "$max_thruput_rate" "$burst_size" "$service_interval" "$delay_bound" "$msdu_ttl" "$msdu_rate_loss"
		eval $cmd
	fi
}

configure_telemetry_sla_thersholds() {
	json_init
	json_set_namespace sla_ns
	json_load_file /lib/wifi/sawf/telemetry/sla.json
	json_select sla
	json_get_keys sla_indexes
	sla_index=0

	sla_index_count=$(echo "$sla_indexes" | wc -w)

	echo "SLA Count: $sla_index_count" > /dev/console

	while [ $sla_index -lt $sla_index_count ]
	do
		sla_json=$(jsonfilter -i /lib/wifi/sawf/telemetry/sla.json -e "@.sla[$sla_index]")
		configure_sla_param "$sla_json"

		sla_index=$(expr $sla_index + 1)
	done
}

configure_telemetry_sla_detect() {
	json_init
	json_load_file /lib/wifi/sawf/telemetry/sla_detect.json
	json_select x_packet
		json_get_var delay_x_packet delay
		json_get_var msdu_loss_x_packet msdu_loss
		json_get_var ttl_drop_x_packet ttl_drop
	json_select ..
	json_select 1_second
		json_get_var min_throutput min_throutput
		json_get_var max_throughput max_throughput
	json_select ..
	json_select mov_average
		json_get_var delay_mov_avg delay
	json_select ..
	json_select x_second
		json_get_var service_interval service_interval
		json_get_var burst_size burst_size
		json_get_var msdu_loss_x_sec msdu_loss
		json_get_var ttl_drop_x_sec ttl_drop
	json_select ..

	cmd="iw $phy telemetry sla_detection_cfg num_packet 0 0 0 0 $delay_x_packet $ttl_drop_x_packet $msdu_loss_x_packet"
	eval $cmd
	cmd="iw $phy telemetry sla_detection_cfg per_second $min_throutput $max_throughput 0 0 0 0 0"
	eval $cmd
	cmd="iw $phy telemetry sla_detection_cfg moving_avg 0 0 0 0 $delay_mov_avg 0 0"
	eval $cmd
	cmd="iw $phy telemetry sla_detection_cfg num_second 0 0 $burst_size $service_interval 0 $ttl_drop_x_sec $msdu_loss_x_sec"
	eval $cmd
}

configure_telemetry_sla_samples() {
	json_init
	json_load_file /lib/wifi/sawf/telemetry/config.json

# Parsing the moving average params
	json_get_var mavg_num_packet mavg_num_packet
	json_get_var mavg_num_window mavg_num_window

# Parsing the sla params
	json_get_var sla_num_packet sla_num_packet
	json_get_var sla_time_secs sla_time_secs

	iw $phy telemetry sla_samples_cfg "$mavg_num_packet" "$mavg_num_window" "$sla_num_packet" "$sla_time_secs"
}

pre_wifi_updown() {
	mac80211_update_mld_configs
	:
}

post_wifi_updown() {
	:
}

restart_rsrcmgr() {
	[ -f /tmp/rsrcmgr.log ] && {
		rm -rf /tmp/rsrcmgr.log
	}
	/etc/init.d/rsrcmgr restart
}

pre_mac80211() {
	local action=${1}
	case "${action}" in
		disable)
			has_updated_cfg=$(ls /var/run/hostapd-*-updated-cfg 2>/dev/null | wc -l)
			if [ "$has_updated_cfg" -gt 0 ]; then
				rm -rf /var/run/hostapd-*updated-cfg
			fi
			rm -rf /var/run/wpa_supplicant-*-updated-cfg  2>/dev/null
			rm -rf /tmp/*_freq_list 2>/dev/null
			if [ -f "$MLD_VAP_DETAILS" ]; then
				rm -rf $MLD_VAP_DETAILS
			fi
			sawf_supp="/sys/module/ath12k/parameters/sawf"
			if [ -f $sawf_supp ] && [ $(cat $sawf_supp) == "Y" ] && \
			   [ -f "/tmp/svc_configured" ]; then
				configure_service_class 0
				rm /tmp/svc_configured
			fi
			if [ -f "/tmp/apsta_mode.pid" ]; then
				pid=$(cat /tmp/apsta_mode.pid)
				kill -15 $pid 2>/dev/null
				rm /tmp/apsta_mode.pid 2>/dev/null
			fi
		;;
		enable)
			restart_rsrcmgr
		;;
	esac
	return 0
}

post_mac80211() {
	local action=${1}

	case "${action}" in
		enable)
			sawf_supp="/sys/module/ath12k/parameters/sawf"
			if [ -f $sawf_supp ] && [ $(cat $sawf_supp) == "Y" ]; then
				configure_service_class 1
				touch /tmp/svc_configured
				configure_telemetry_sla_samples
				configure_telemetry_sla_thersholds
				configure_telemetry_sla_detect
			fi
		;;
	esac
	return 0
}
