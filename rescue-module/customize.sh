[ "${KSU_RUNTIME_MODE:-}" != late-load ] && [ "${KSU_LATE_LOAD:-0}" != 1 ] || abort "Modo late-load não suportado pelo resgate."
[ ! -f /data/adb/modules/bloatware_exorcism_rescue/state/pending ] || abort "Há um lote armado. Confirme ou reverta antes de atualizar o módulo."
[ "$KSU" = true ] || abort "Este módulo experimental requer KernelSU."
[ -x /data/adb/ksu/bin/busybox ] || abort "BusyBox do KernelSU indisponível."
/data/adb/ksu/bin/busybox --list | grep -qx flock || abort "BusyBox sem flock. Instalação cancelada."
mkdir -p "$MODPATH/state"
chmod 700 "$MODPATH/state"
touch "$MODPATH/skip_mount"
set_perm_recursive "$MODPATH" 0 0 0700 0600
set_perm "$MODPATH/rescue.sh" 0 0 0700
set_perm "$MODPATH/service.sh" 0 0 0700
ui_print "Resgate experimental instalado; nenhum lote está armado."
ui_print "Reinicie antes de armar o primeiro lote pelo aplicativo."
