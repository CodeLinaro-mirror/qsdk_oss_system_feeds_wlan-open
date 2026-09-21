# rdk.mk
#
# GNU Make wrapper that includes ath.mk to generate the backports .config.
# Invoked from do_configure:prepend() in open-mac80211.bb as:
#
#   make -f rdk.mk print-config \
#        OUTPUT_CONFIG=<path>/.config  \
#        CONFIG_KERNEL_IPQ_MEM_PROFILE=<512|""> \
#        CONFIG_PACKAGE_ATH_DEBUG=y    \
#        CONFIG_PACKAGE_MAC80211_DEBUGFS=y \
#        ... (all CONFIG_PACKAGE_* / CONFIG_* vars from ipq_mem_open/512)
#
# ath.mk only needs two OpenWrt helpers:
#   config_package  — returns 'm' if CONFIG_PACKAGE_kmod-<name> is set
#   ConfigVars      — converts config-y/m lists to CPTCFG_*=y/m lines

# Stubs for the two OpenWrt helpers ath.mk needs

# Returns 'm' if CONFIG_PACKAGE_kmod-<name> is set, empty otherwise.
# RDK has no BUILD_VARIANT concept so the variant argument is ignored.
config_package = $(if $(CONFIG_PACKAGE_kmod-$(1)),m)

# Emits CPTCFG_<FLAG>=<val> for each flag in config-<val>
space :=
space +=
define ConfigVars
$(subst $(space),,$(foreach opt,$(config-$(1)),CPTCFG_$(opt)=$(1)
))
endef

# Empty stubs so ath.mk's KernelPackage/ath* blocks do not cause errors
KernelPackage/mac80211/Default =
AutoProbe =

# Always-on flags from mac80211/Makefile (before include ath.mk)
config-y := \
	WLAN \
	EXPERT \
	NL80211_TESTMODE \
	CFG80211_CERTIFICATION_ONUS \
	CFG80211_DEFAULT_PS \
	CFG80211_CRDA_SUPPORT \
	MAC80211_RC_MINSTREL \
	MAC80211_RC_MINSTREL_HT \
	MAC80211_RC_MINSTREL_VHT \
	MAC80211_RC_DEFAULT_MINSTREL \
	WLAN_VENDOR_ADMTEK \
	WLAN_VENDOR_ATH \
	WLAN_VENDOR_RSI

config-$(call config_package,cfg80211,$(ALL_VARIANTS)) += CFG80211
config-$(CONFIG_PACKAGE_CFG80211_TESTMODE) += NL80211_TESTMODE
config-$(call config_package,mac80211,$(ALL_VARIANTS)) += MAC80211
config-$(CONFIG_PACKAGE_MAC80211_MESH) += MAC80211_MESH
config-$(CONFIG_PACKAGE_kmod-mac80211) += MAC80211_DEBUG_MENU MAC80211_STA_DEBUG MAC80211_MLME_DEBUG

include ath.mk

# Flags from mac80211/Makefile after include ath.mk
ifdef CONFIG_PACKAGE_MAC80211_DEBUGFS
  config-y += \
	CFG80211_DEBUGFS \
	MAC80211_DEBUGFS
endif

ifdef CONFIG_PACKAGE_MAC80211_TRACING
  config-y += \
	IWLWIFI_DEVICE_TRACING
endif

config-$(CONFIG_PACKAGE_MAC80211_PPE_SUPPORT) += MAC80211_PPE_SUPPORT
config-$(CONFIG_PACKAGE_MAC80211_DS_SUPPORT) += MAC80211_DS_SUPPORT ATH12K_PPE_DS_SUPPORT
config-$(CONFIG_PACKAGE_MAC80211_SFE_SUPPORT) += MAC80211_SFE_SUPPORT
config-$(CONFIG_PACKAGE_MAC80211_MESSAGE_TRACING) += MAC80211_MESSAGE_TRACING ATH10K_TRACING ATH11K_TRACING ATH12K_TRACING
config-$(CONFIG_PACKAGE_MAC80211_DEBUG_MENU) += MAC80211_DEBUG_MENU
config-$(CONFIG_PACKAGE_MAC80211_VERBOSE_DEBUG) += MAC80211_VERBOSE_DEBUG
config-$(CONFIG_PACKAGE_MAC80211_PS_DEBUG) += MAC80211_PS_DEBUG
config-$(CONFIG_PACKAGE_MAC80211_ATHMEMDEBUG) += MAC80211_ATHMEMDEBUG
config-$(CONFIG_PACKAGE_QCN_EXTN) += QCN_EXTN
config-$(CONFIG_PACKAGE_QCA_LAB_TEST_FEATURES) += QCA_LAB_TEST_FEATURES
config-$(CONFIG_PACKAGE_QCN_EXTN_MESH_SUPPORT) += QCN_EXTN_MESH_SUPPORT
config-$(call config_package,mac80211-hwsim) += MAC80211_HWSIM
config-$(CONFIG_PACKAGE_MAC80211_ATHDEBUG) += ATHDEBUG DEBUG_FS
config-y += WL_TI WILINK_PLATFORM_DATA
config-$(CONFIG_LEDS_TRIGGERS) += MAC80211_LEDS

# Write resolved CPTCFG_* flags to OUTPUT_CONFIG
.PHONY: print-config
print-config:
	@( $(foreach opt,$(config-m),echo "CPTCFG_$(opt)=m";) \
	   $(foreach opt,$(config-y),echo "CPTCFG_$(opt)=y";) ) > $(OUTPUT_CONFIG)
	@echo "rdk.mk: wrote $$(wc -l < $(OUTPUT_CONFIG)) CPTCFG_* flags to $(OUTPUT_CONFIG)"
