#!/system/bin/sh
MODDIR=${0%/*}
. "$MODDIR/lib.sh"
if [ "${KSU_LATE_LOAD:-0}" = 1 ]; then
    rm -f "$EX_STATE/ready"
    ex_log unsupported_late_load
    exit 0
fi
[ -d "$EX_STATE" ] && [ ! -L "$EX_STATE" ] || exit 1
ex_write ready 1
ex_monitor
