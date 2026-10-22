EX_STATE="$MODDIR/state"
umask 077
ex_prop() { /system/bin/getprop "$1"; }
ex_pm() { timeout 20 /system/bin/pm "$@"; }
ex_cmd() { timeout 20 /system/bin/cmd "$@"; }
ex_dump() { timeout 20 /system/bin/dumpsys package "$1"; }
ex_bootid() { cat /proc/sys/kernel/random/boot_id; }
ex_fingerprint() { printf '%s' "$(ex_prop ro.build.fingerprint)" | sha256sum | cut -d' ' -f1; }
ex_log() { printf '%s|%s\n' "$(date +%s)" "$1" >> "$EX_STATE/events.log"; }
ex_init() {
    [ -d "$EX_STATE" ] && [ ! -L "$EX_STATE" ] && [ -f "$EX_STATE/ready" ] || return 1
    [ "$(stat -c %u "$EX_STATE")" = 0 ] || return 1
    [ ! -e "$MODDIR/disable" ] && [ ! -e "$MODDIR/remove" ] || return 1
}
ex_lock() { exec 9>"$EX_STATE/lock"; flock -x -w 3 9; }
ex_unlock() { flock -u 9; exec 9>&-; }
ex_write() { printf '%s\n' "$2" > "$EX_STATE/.$1.tmp" && mv "$EX_STATE/.$1.tmp" "$EX_STATE/$1"; }
ex_id_valid() { printf '%s\n' "$1" | grep -Eq '^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'; }
ex_package_valid() { printf '%s\n' "$1" | grep -Eq '^[A-Za-z][A-Za-z0-9_]*(\.[A-Za-z0-9_]+)+$'; }
ex_valid_rows() {
    awk -F'|' 'NF!=2 || $2 !~ /^[0-4]$/ || seen[$1]++ {exit 1} END {if(NR<1 || NR>30) exit 1}' "$1" || return 1
    while IFS='|' read -r ex_pkg ex_enabled; do
        ex_package_valid "$ex_pkg" || return 1
        grep -Fxq "$ex_pkg" "$MODDIR/allowed-packages.txt" || return 1
    done < "$1"
}
ex_read_pending() {
    [ -f "$EX_STATE/pending" ] && [ ! -L "$EX_STATE/pending" ] || return 1
    [ "$(wc -c < "$EX_STATE/pending")" -le 16384 ] || return 1
    {
        IFS= read -r ex_format
        IFS= read -r EX_TX
        IFS= read -r EX_FP
        IFS= read -r EX_USER
        cat > "$EX_STATE/.rows"
    } < "$EX_STATE/pending"
    [ "$ex_format" = BE_RESCUE_V1 ] && ex_id_valid "$EX_TX" || return 1
    printf '%s\n' "$EX_FP" | grep -Eq '^[0-9a-f]{64}$' || return 1
    printf '%s\n' "$EX_USER" | grep -Eq '^(0|[1-9][0-9]{0,3})$' || return 1
    ex_valid_rows "$EX_STATE/.rows"
}
ex_arm() {
    ex_init && ex_lock || return 1
    if [ -e "$EX_STATE/pending" ]; then ex_unlock; printf 'error=pending_batch\n'; return 1; fi
    head -c 16385 > "$EX_STATE/.incoming"
    [ "$(wc -c < "$EX_STATE/.incoming")" -le 16384 ] || { rm -f "$EX_STATE/.incoming"; ex_unlock; return 1; }
    mv "$EX_STATE/.incoming" "$EX_STATE/pending"
    if ! ex_read_pending || [ "$EX_FP" != "$(ex_fingerprint)" ]; then
        rm -f "$EX_STATE/pending" "$EX_STATE/.rows"
        ex_unlock; printf 'error=invalid_transaction\n'; return 1
    fi
    ex_write armed_boot "$(ex_bootid)"
    ex_write attempts 0
    rm -f "$EX_STATE/observed_boot" "$EX_STATE/boot_ok" "$EX_STATE/last_result"
    ex_log "armed|$EX_TX"
    sync
    ex_unlock
    printf 'armed=%s\n' "$EX_TX"
}
ex_parse_state() {
    awk -v user="$1" '
    /^[ \t]*User[ \t]+[0-9]+[ \t]*:/ {
        line=$0;sub(/^[ \t]*User[ \t]+/,"",line);sub(/[ \t]*:.*/,"",line)
        active=(line+0==user+0)
        if(active){users++;match($0,/[^ \t]/);indent=RSTART-1;scan($0)}
        next
    }
    active && /^[ \t]*(installed|enabled|ceDataInode|deDataInode|hidden|suspended|stopped|notLaunched)=/ {
        match($0,/[^ \t]/); if(RSTART-1>indent) scan($0); else active=0
        next
    }
    active && /[^ \t]/ {active=0}
    function scan(line, n,a,i) {
        n=split(line,a,/[ \t]+/)
        for(i=1;i<=n;i++){
            if(a[i]~/^installed=/){installed++;iv=a[i];sub(/^installed=/,"",iv)}
            if(a[i]~/^enabled=/){enabled++;ev=a[i];sub(/^enabled=/,"",ev)}
        }
    }
    END {
        if(users!=1 || installed!=1 || enabled!=1 || iv!~/^(true|false)$/ || ev!~/^[0-4]$/) exit 1
        print iv "|" ev
    }'
}
ex_ready() { ex_pm list packages --user "$EX_USER" android 2>/dev/null | grep -Fxq package:android; }
ex_rollback_locked() {
    ex_read_pending || return 1
    if [ "$EX_FP" != "$(ex_fingerprint)" ]; then ex_log "blocked_fingerprint|$EX_TX"; return 1; fi
    ex_ready || { ex_log "package_manager_unavailable|$EX_TX"; return 1; }
    ex_failed=0
    while IFS='|' read -r ex_pkg ex_enabled; do
        case "$ex_enabled" in
            0) ex_action=default-state;; 1) ex_action=enable;; 2) ex_action=disable;; 3) ex_action=disable-user;; 4) ex_action=disable-until-used;;
            *) ex_failed=1; continue;;
        esac
        ex_cmd package install-existing --user "$EX_USER" "$ex_pkg" >/dev/null 2>&1
        ex_pm "$ex_action" --user "$EX_USER" "$ex_pkg" >/dev/null 2>&1
        ex_got=$(ex_dump "$ex_pkg" 2>/dev/null | ex_parse_state "$EX_USER")
        if [ "$ex_got" = "true|$ex_enabled" ]; then ex_log "restored|$EX_TX|$ex_pkg"; else ex_log "unverified|$EX_TX|$ex_pkg"; ex_failed=1; fi
    done < "$EX_STATE/.rows"
    if [ "$ex_failed" = 0 ]; then
        ex_write last_result "restored|$EX_TX"
        mv "$EX_STATE/pending" "$EX_STATE/last_restored"
        ex_log "batch_restored|$EX_TX"
    else ex_write last_result "incomplete|$EX_TX"; fi
    sync
    [ "$ex_failed" = 0 ]
}
ex_rollback() {
    ex_init && ex_lock || return 1
    if [ -n "${1:-}" ]; then
        ex_read_pending && [ "$EX_TX" = "$1" ] || { ex_unlock; return 1; }
    fi
    ex_rollback_locked; ex_result=$?; ex_unlock; return "$ex_result"
}
ex_status() {
    ex_init && ex_lock || return 1
    printf 'protocol=1\n'
    if [ -e "$EX_STATE/pending" ]; then
        if ex_read_pending; then printf 'pending=%s\n' "$EX_TX"; else printf 'pending=invalid\n'; fi
    else printf 'pending=none\n'; fi
    printf 'boot_ready=%s\n' "$(cat "$EX_STATE/boot_ok" 2>/dev/null)"
    printf 'last_result=%s\n' "$(cat "$EX_STATE/last_result" 2>/dev/null)"
    ex_unlock
}
ex_ack() {
    ex_init && ex_lock || return 1
    ex_read_pending || { ex_unlock; return 1; }
    if [ "$EX_FP" != "$(ex_fingerprint)" ] || [ "$(cat "$EX_STATE/boot_ok" 2>/dev/null)" != "$(ex_bootid)" ] || [ "$(cat "$EX_STATE/armed_boot" 2>/dev/null)" = "$(ex_bootid)" ]; then
        ex_unlock; printf 'error=successful_reboot_required\n'; return 1
    fi
    ex_write last_result "kept|$EX_TX"
    mv "$EX_STATE/pending" "$EX_STATE/last_kept"
    ex_log "kept|$EX_TX"
    sync; ex_unlock; printf 'kept=%s\n' "$EX_TX"
}
ex_monitor() {
    ex_init && ex_lock || return 0
    [ -f "$EX_STATE/pending" ] || { ex_unlock; return 0; }
    ex_read_pending || { ex_unlock; return 1; }
    ex_now=$(ex_bootid)
    [ "$(cat "$EX_STATE/armed_boot" 2>/dev/null)" != "$ex_now" ] || { ex_unlock; return 0; }
    if [ "$(cat "$EX_STATE/observed_boot" 2>/dev/null)" != "$ex_now" ]; then
        ex_attempts=$(cat "$EX_STATE/attempts" 2>/dev/null)
        case "$ex_attempts" in ''|*[!0-9]*) ex_attempts=0;; esac
        [ "$ex_attempts" -lt 10 ] || ex_attempts=10
        ex_write attempts "$((ex_attempts + 1))"
        ex_write observed_boot "$ex_now"
        sync
    fi
    ex_attempts=$(cat "$EX_STATE/attempts")
    ex_monitor_tx=$EX_TX
    ex_unlock
    ex_seconds=0
    while [ "$ex_seconds" -lt 180 ]; do
        ex_lock || return 1
        if ! ex_read_pending || [ "$EX_TX" != "$ex_monitor_tx" ]; then ex_unlock; return 0; fi
        ex_unlock
        if [ "$ex_attempts" -ge 2 ] && ex_ready; then ex_rollback "$ex_monitor_tx"; return $?; fi
        if [ "$(ex_prop sys.boot_completed)" = 1 ]; then
            ex_lock || return 1
            if ex_read_pending && [ "$EX_FP" = "$(ex_fingerprint)" ]; then
                ex_write boot_ok "$ex_now"; ex_write attempts 0; ex_log "boot_completed|$EX_TX"
            fi
            ex_unlock; return 0
        fi
        sleep 3; ex_seconds=$((ex_seconds + 3))
    done
    ex_rollback "$ex_monitor_tx"
}
ex_main() {
    case "${1:-}" in
        status) ex_status;; arm) ex_arm;; acknowledge) ex_ack;; rollback) ex_rollback;;
        *) printf 'error=unknown_operation\n'; return 1;;
    esac
}
