#!/bin/bash

export LC_CTYPE=C
export LANG=C

available=1
if ! command -v htpasswd > /dev/null; then
    echo "htpasswd command is not available"
    available=0
fi

if ! command -v sqlite3 > /dev/null; then
    echo "sqlite3 command is not available"
    available=0
fi

if [ $available -eq 0 ]; then
    exit 1
fi

environment="prod"
new_password=$(tr -dc 'A-Za-z0-9!?%=' < /dev/urandom | head -c 10)

# Get the options
while getopts "e:p:u:" option; do
    case "${option}" in
        u)
            username=${OPTARG}
            ;;
        e)
            environment=${OPTARG}
            ;;
        p)
            new_password=${OPTARG}
            ;;
   esac
done

bcrypt="htpasswd -bnBC 12 \"\" $new_password | cut -d : -f 2"
password=$(eval $bcrypt)

if [ "${username}" == "" ]; then
    echo "Place this script in the Shashin root directory. The default environment is prod."
    echo "$0 -u <username>"
    echo "Options:"
       echo "-e     Environment - one of test/dev/prod"
       echo "-p     Preset password"
    exit 1
fi

if [ -z "${password}" ]; then
    echo "Password could not be encrypted"
    exit 1
fi

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
else
    db_command="sqlite3 shashin_${environment}.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
fi

tables=($(eval $db_command))
validtables=("album" "albumcomment" "albumphoto" "albumphotocomment" "comment" "favorite" "keyword" "keywordphoto" "mediadir" "metadata" "notification" "recognitionlabel" "recognitionlabelphoto" "searchhistory" "settings" "user" "useragent" "useralbum")

count=0
for i in "${tables[@]}"
do
    for k in "${validtables[@]}"
    do
        if [[ $i = $k ]]; then
            count=$((count+1))
        fi
    done
done

if [ $count != ${#validtables[@]} ]; then
    echo "Invalid tables"
    exit 1
fi

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'SELECT password FROM user WHERE username = \"${username}\";'"
else
    db_command="sqlite3 shashin_${environment}.db 'SELECT password FROM user WHERE username = \"${username}\";'"
fi

old_password=$(eval $db_command)

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'SELECT id FROM user WHERE username = \"${username}\";'"
else
    db_command="sqlite3 shashin_${environment}.db 'SELECT id FROM user WHERE username = \"${username}\";'"
fi

user_id=$(eval $db_command)

if [ -z "${user_id}" ]; then
    echo "User '$username' not found"
    exit 1
fi

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'UPDATE user SET password = \"${password}\" WHERE id = ${user_id};'"
else
    db_command="sqlite3 shashin_${environment}.db 'UPDATE user SET password = \"${password}\" WHERE id = ${user_id};'"
fi

eval $db_command

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'SELECT password FROM user WHERE id = ${user_id};'"
else
    db_command="sqlite3 shashin_${environment}.db 'SELECT password FROM user WHERE id = ${user_id};'"
fi

update_password=$(eval $db_command)

if [ "$old_password" != "" ] && [ "$update_password" != "" ]; then
    echo "Password reset for '${username}' in ${environment}. Change after logging in under Manage Account: $new_password"
else
    echo "Oops, something went wrong!"
fi