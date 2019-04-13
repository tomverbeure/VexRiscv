rm -fr ./db
rm -fr ./incremental_db
$QUARTUS_ROOTDIR/bin/quartus_map --read_settings_files=on --write_settings_files=off VexRiscv -c VexRiscv
$QUARTUS_ROOTDIR/bin/quartus_fit --read_settings_files=off --write_settings_files=off VexRiscv -c VexRiscv
$QUARTUS_ROOTDIR/bin/quartus_sta --model=slow VexRiscv -c VexRiscv

