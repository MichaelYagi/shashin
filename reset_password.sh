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

dbenv="prod"
new_password=$(tr -dc 'A-Za-z0-9!?%=' < /dev/urandom | head -c $((9+RANDOM%13)))

# Get the options
while getopts "e:p:u:" option; do
    case "${option}" in
        u)
            username=${OPTARG}
            ;;
        e)
            dbenv=${OPTARG}
            ;;
        p)
            new_password=${OPTARG}
            ;;
        *)
            echo "Unknown option $i"
            exit 1
            ;;
   esac
done

if [ "${dbenv}" != "test" ] &&  [ "${dbenv}" != "dev" ] && [ "${dbenv}" != "prod" ]; then
    echo "Environment must be one of test, dev or prod"
    exit 1
fi

environment=""
if [ "${dbenv}" = "test" ] ||  [ "${dbenv}" = "dev" ]; then
    environment="_$dbenv"
fi

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

db_command="sqlite3 shashin${environment}.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
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
    echo "Invalid tables."
    exit 1
fi

db_command="sqlite3 shashin${environment}.db 'SELECT password FROM user WHERE username = \"${username}\";'"
old_password=$(eval $db_command)

db_command="sqlite3 shashin${environment}.db 'SELECT id FROM user WHERE username = \"${username}\";'"
user_id=$(eval $db_command)

if [ -z "${user_id}" ]; then
    echo "User '$username' not found"
    exit 1
fi

db_command="sqlite3 shashin${environment}.db 'UPDATE user SET password = \"${password}\" WHERE id = ${user_id};'"
eval $db_command

db_command="sqlite3 shashin${environment}.db 'SELECT password FROM user WHERE id = ${user_id};'"
update_password=$(eval $db_command)

if [ "$old_password" != "" ] && [ "$update_password" != "" ]; then
    echo "Password reset for '${username}' in ${dbenv}. Change after logging in under Manage Account."
    echo $new_password
else
    echo "Oops, something went wrong!"
fi