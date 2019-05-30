#!/usr/bin/env bash

if [ $(id -u) -ne 0 ]
then
    echo Run this script as root >&2
    exit 1
fi

systemctl stop snmpd 1>/dev/null 2>&1

apt-get update

apt-get -y install snmpd snmp-mibs-downloader mysql-client

download-mibs

install -m 600 ./snmpd.conf /etc/snmp/
install -m 755 ./bmysql-snmp.sh /usr/local/bin/
sleep 10

systemctl start snmpd

