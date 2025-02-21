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

if [ "$#" -lt 1 ]; then
    echo "Usage, place this script in the shashin root directory, default environment is prod: $0 <username>"
    exit 1
fi
username=$1
environment="prod"
if [ "$#" -eq 2 ]; then
    # prod, dev or test
    environment=$2
fi

new_password=$(tr -dc 'A-Za-z0-9!?%=' < /dev/urandom | head -c 10)
bcrypt="htpasswd -bnBC 12 \"\" $new_password | cut -d : -f 2"
password=$(eval $bcrypt)

if [ -z "${password}" ]; then
    echo "Password could not be generated"
    exit 1
fi

db_command=""
if [ "${environment}" = "prod" ]; then
    db_command="sqlite3 shashin.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
else
    db_command="sqlite3 shashin_${environment}.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
fi

tables=$(eval $db_command)
validtables=("album" "albumcomment" "albumphoto" "albumphotocomment" "comment" "favorite" "keyword" "keywordphoto" "mediadir" "metadata" "notification" "persistent_logins" "persistent_logins_expiry" "recognitionlabel" "recognitionlabelphoto" "searchhistory" "settings" "user" "useragent" "useralbum")
# shellcheck disable=SC2068
diff=(`echo ${tables[@]} ${validtables[@]} | tr ' ' '\n' | sort | uniq -u `)
arraylength=${#diff[@]}

if [ $arraylength -ne 0 ]; then
    echo "Validating tables failed. Missing tables:"
    echo $diff
    exit 1;
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