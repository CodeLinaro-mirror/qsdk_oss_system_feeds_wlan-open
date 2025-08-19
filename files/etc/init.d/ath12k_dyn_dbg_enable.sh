#!/bin/sh /etc/rc.common
#
# Copyright (c) 2020 The Linux Foundation. All rights reserved.
# Copyright (c) 2022 Qualcomm Innovation Center, Inc. All rights reserved.
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
#

START=03
STOP=94

boot()
{
	# Add path so ini framework will read the files from the /ini folder
	if ! echo -n "/ini" > /sys/module/firmware_class/parameters/path; then
		echo "Failed to set path for ini framework" > /dev/console
	fi
	ath12k="/etc/modules.d/ath12k"
	if [ -e $ath12k ];
	then
		# Read first line only
		content=$(head -n 1 $ath12k)

		# If first line does not have "dyndbg" substr in it
		if [[ $content != *"dyndbg"* ]];
		then
			sed -i '1s/ath12k/ath12k dyndbg=+p/' $ath12k
		fi
	fi
	update_ath12k_module_params_from_cmdline
}

# this function is to parse the bootargs and update ath12k
# module params

update_ath12k_module_params_from_cmdline() {
    ath12k="/etc/modules.d/ath12k"
    if [ -e "$ath12k" ]; then
        content=$(head -n 1 "$ath12k")
        cmdline=$(cat /proc/cmdline)

        for param in $cmdline; do
            if [[ "$param" == ath12k_* ]]; then
                option="${param#ath12k_}"
                # Validate option format (basic check)
                if [[ "$option" =~ ^[a-zA-Z0-9_]+=.+$ ]]; then
                    if [[ "$content" != *"$option"* ]]; then
                        sed -i "1s/$/ $option/" "$ath12k"
                        # Refresh content after update
                        content=$(head -n 1 "$ath12k")
                    fi
                fi
            fi
        done
    fi
}
