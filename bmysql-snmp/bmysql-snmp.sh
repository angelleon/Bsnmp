#!/usr/bin/env bash

mysql -h localhost -u boletazodev -pcontrapass -e exit Boletazo 1>/dev/null 2>&1

if [ $? -eq 0 ]
then 
    echo successful db connection
else
    echo failed db connection
fi
