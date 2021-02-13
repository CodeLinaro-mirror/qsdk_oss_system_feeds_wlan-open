#!/bin/sh
#
# Copyright (c) 2019, The Linux Foundation. All rights reserved.
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
[ -e /lib/smp_affinity_settings.sh ] && . /lib/smp_affinity_settings.sh

perf_setup(){

	if [ -d "/sys/kernel/debug/ath11k" ]; then
		local board=$(ipq806x_board_name)
		#read the to check whether nss_offload is enabled or not if not then set 0
		enable_nss_offload=$(cat /sys/module/ath11k/parameters/nss_offload)

                if [ "$enable_nss_offload" -eq 1 ]; then
                        /etc/init.d/qca-nss-ecm start
			sysctl -w dev.nss.n2hcfg.n2h_queue_limit_core0=256 >/dev/null 2>/dev/null
			sysctl -w dev.nss.n2hcfg.n2h_queue_limit_core1=256 >/dev/null 2>/dev/null

                        #Experimental values below. Subject to changes after analysis
                        # TODO : allocate mem as per platform
                        sysctl -w dev.nss.n2hcfg.extra_pbuf_core0=9000000 >/dev/null 2>/dev/null
                        sysctl -w dev.nss.n2hcfg.n2h_high_water_core0=67392 >/dev/null 2>/dev/null
                        sysctl -w dev.nss.n2hcfg.n2h_wifi_pool_buf=40960 >/dev/null 2>/dev/null
                        return;
                fi

		if [ "$enable_nss_offload" -eq 0 ]; then
			/etc/init.d/qca-nss-ecm stop
		fi

		if [ "$board" != "ap-hk14" ]; then
			[ -d "/sys/class/net/wlan0" ] && echo e > /sys/class/net/wlan0/queues/rx-0/rps_cpus
			[ -d "/sys/class/net/wlan1" ] && echo e > /sys/class/net/wlan1/queues/rx-0/rps_cpus
			[ -d "/sys/class/net/wlan2" ] && echo e > /sys/class/net/wlan2/queues/rx-0/rps_cpus
		fi

		for ifname in $(ls /sys/class/net/ | grep 'wlan' 2>/dev/null); do
			tc qdisc replace dev $ifname root noqueue
		done

		[ -d "/proc/sys/dev/nss/n2hcfg/" ] && echo 2048 > /proc/sys/dev/nss/n2hcfg/n2h_queue_limit_core0
		[ -d "/proc/sys/dev/nss/n2hcfg/" ] && echo 2048 > /proc/sys/dev/nss/n2hcfg/n2h_queue_limit_core1

		[ -d "/proc/sys/dev/nss/rps/" ] && echo 14 > /proc/sys/dev/nss/rps/hash_bitmap
	fi
}

perf_setup
