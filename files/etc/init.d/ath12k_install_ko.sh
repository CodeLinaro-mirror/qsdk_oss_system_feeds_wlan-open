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
	# ath12k debug_mask belongs to ath12k.ko (not ath12k_wifi7.ko)
        insmod ath12k.ko dyndbg=+p
        insmod ath12k_wifi7.ko
        # Mark KPI after all drivers are loaded
        echo -n 'M - QCA wifi Ready' >> /sys/kernel/boot_kpi/kpi_values

        sleep 60
        echo 1 > /proc/irq/225/smp_affinity
        echo 1 > /proc/irq/226/smp_affinity
        echo 1 > /proc/irq/227/smp_affinity
        echo 1 > /proc/irq/228/smp_affinity
        echo 2 > /proc/irq/229/smp_affinity
        echo 4 > /proc/irq/230/smp_affinity
        echo 8 > /proc/irq/231/smp_affinity
        echo 1 > /proc/irq/232/smp_affinity
        echo 2 > /proc/irq/233/smp_affinity
        echo 4 > /proc/irq/234/smp_affinity
        echo 8 > /proc/irq/235/smp_affinity
        echo 2 > /proc/irq/236/smp_affinity
        echo 4 > /proc/irq/237/smp_affinity
        echo 1 > /proc/irq/238/smp_affinity
        echo 1 > /proc/irq/239/smp_affinity
        echo 4 > /proc/irq/241/smp_affinity
        echo 1 > /proc/irq/242/smp_affinity
        echo 1 > /proc/irq/243/smp_affinity
        echo 1 > /proc/irq/248/smp_affinity
        echo 1 > /proc/irq/257/smp_affinity
        echo 1 > /proc/irq/258/smp_affinity
        echo 1 > /proc/irq/259/smp_affinity
        echo 1 > /proc/irq/260/smp_affinity
        echo 2 > /proc/irq/261/smp_affinity
        echo 4 > /proc/irq/262/smp_affinity
        echo 8 > /proc/irq/263/smp_affinity
        echo 1 > /proc/irq/264/smp_affinity
        echo 8 > /proc/irq/265/smp_affinity
        echo 8 > /proc/irq/266/smp_affinity
        echo 8 > /proc/irq/267/smp_affinity
        echo 4 > /proc/irq/268/smp_affinity
        echo 8 > /proc/irq/269/smp_affinity
        echo 1 > /proc/irq/270/smp_affinity
        echo 1 > /proc/irq/271/smp_affinity
        echo 8 > /proc/irq/273/smp_affinity
        echo 1 > /proc/irq/274/smp_affinity
        echo 1 > /proc/irq/275/smp_affinity
        echo 1 > /proc/irq/280/smp_affinity
        echo 1 > /proc/irq/289/smp_affinity
        echo 1 > /proc/irq/290/smp_affinity
        echo 1 > /proc/irq/291/smp_affinity
        echo 1 > /proc/irq/292/smp_affinity
        echo 2 > /proc/irq/293/smp_affinity
        echo 4 > /proc/irq/294/smp_affinity
        echo 8 > /proc/irq/295/smp_affinity
        echo 1 > /proc/irq/296/smp_affinity
        echo 2 > /proc/irq/297/smp_affinity
        echo 2 > /proc/irq/298/smp_affinity
        echo 8 > /proc/irq/299/smp_affinity
        echo 8 > /proc/irq/300/smp_affinity
        echo 2 > /proc/irq/301/smp_affinity
        echo 1 > /proc/irq/302/smp_affinity
        echo 1 > /proc/irq/303/smp_affinity
        echo 2 > /proc/irq/305/smp_affinity
        echo 1 > /proc/irq/306/smp_affinity
        echo 1 > /proc/irq/307/smp_affinity
        echo 1 > /proc/irq/312/smp_affinity
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
