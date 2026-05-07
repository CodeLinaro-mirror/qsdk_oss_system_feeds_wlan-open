#!/bin/sh /etc/rc.common

# Copyright (c) Qualcomm Technologies, Inc. and/or its subsidiaries.
# SPDX-License-Identifier: ISC

START=90
STOP=94

start()
{
        insmod compat.ko
        insmod cfg80211.ko
        insmod mac80211.ko
        insmod ath.ko
        insmod ath_debug.ko
        insmod ath12k.ko
        insmod ath12k_wifi7.ko dyndbg=+p debug_mask=0xFFFFFFFF
        # Mark KPI after all drivers are loaded
        echo -n 'M - QCA wifi Ready' >> /sys/kernel/boot_kpi/kpi_values
}

stop()
{
        rmmod ath12k_wifi7
        rmmod ath12k
        rmmod ath_debug
        rmmod ath
        rmmod mac80211
        rmmod cfg80211
        rmmod compat
}
