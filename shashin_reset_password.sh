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

usage() {
    echo "Reset a users password. Place this script in the same directory as the Shashin database. The default environment is prod."
    echo ""
    echo "$0 -u <username>"
    echo ""
    echo "Options:"
    echo "-e     Environment - test, dev or prod"
    echo "-p     Preset password, otherwise a random password will be generated"
}

# Transform long options to short ones
for arg in "$@"; do
  shift
  case "$arg" in
    '--help') set -- "$@" '-h'   ;;
    '--username') set -- "$@" '-u'   ;;
    '--password') set -- "$@" '-p'   ;;
    '--environment') set -- "$@" '-e'   ;;
    *) set -- "$@" "$arg" ;;
  esac
done

dbenv="prod"
new_password=$(tr -dc 'A-Za-z0-9!?%=' < /dev/urandom | head -c $((9+RANDOM%13)))

# Get the options
# shellcheck disable=SC2214
while getopts "e:p:u:h?" option; do
    case "${option}" in
        h)
            usage
            exit 1
            ;;
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
            usage
            exit 1
            ;;
   esac
done

if [ "${dbenv}" != "test" ] &&  [ "${dbenv}" != "dev" ] && [ "${dbenv}" != "prod" ]; then
    echo "Environment must be one of test, dev or prod"
    usage
    exit 1
fi

environment=""
if [ "${dbenv}" = "test" ] ||  [ "${dbenv}" = "dev" ]; then
    environment="_$dbenv"
fi

bcrypt="htpasswd -bnBC 12 \"\" $new_password | cut -d : -f 2"
password=$(eval $bcrypt)

if [ "${username}" == "" ]; then
    usage
    exit 1
fi

if [ -z "${password}" ]; then
    echo "Password could not be encrypted"
    exit 1
fi

db_command="sqlite3 shashin${environment}.db 'SELECT name FROM sqlite_master WHERE type = \"table\";'"
tables=($(eval $db_command))
validtables=("album" "albumcomment" "albumphoto" "albumphotocomment" "comment" "favorite" "folderdata" "keyword" "keywordphoto" "mediadir" "metadata" "notification" "recognitionlabel" "recognitionlabelphoto" "slideshowalbum" "searchhistory" "settings" "user" "useragent" "useralbum")

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
    usage
fi