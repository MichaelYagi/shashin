#!/bin/bash

if [ "$#" -ne 3 ]; then
    echo "Usage: $0 <version> <gh_token> <changes_file>"
    echo ""
    echo "<version> - The release version without pre-pended 'v'"
    echo "<gh_token> - The Github token to create the release - must have content write privileges"
    echo "<changes_file> - The changelog to parse - must be https://keepachangelog.com/ format"
    exit 1
fi
version=$1
ghtoken=$2
changesfile=$3

# Get release notes from change log by version
awk -v ver="${version}" '/^#+ \[/ { if (p) { exit }; if ($2 == "["ver"]") { p=1; next} } p && NF' "${changesfile}" > shashin_release_notes.txt

# Replace carriage return with newline
release_notes=$(cat shashin_release_notes.txt)
release_notes="${release_notes//$'\r'/\\n}"
release_notes="${release_notes//$'\n'}"

# Create json payload
touch shashin_release_data.json
echo "{\"tag_name\":\"v$version\",\"body\":\"$release_notes\"}" > shashin_release_data.json

# curl request to create release
curl_cmd="curl --request POST --header 'Content-Type: application/json' --header 'Authorization: Bearer $ghtoken' -v --http1.1 'https://api.github.com/repos/michaelyagi/shashin/releases' -d @shashin_release_data.json"
eval "$curl_cmd"

# Cleanup files
del_data_cmd="rm -f shashin_release_data.json"
eval "$del_data_cmd"
del_txt_cmd="rm -f shashin_release_notes.txt"
eval "$del_txt_cmd"