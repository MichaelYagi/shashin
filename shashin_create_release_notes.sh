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

# Create json payload (using jq to safely escape special characters)
jq -n --arg tag_name "v$version" --arg body "$release_notes" \
  '{tag_name: $tag_name, body: $body}' > shashin_release_data.json

# Check if a release already exists for this tag
existing_id=$(curl --silent \
  --header "Authorization: Bearer $ghtoken" \
  --http1.1 \
  "https://api.github.com/repos/michaelyagi/shashin/releases/tags/v$version" \
  | grep '"id"' | head -1 | grep -o '[0-9]*')

if [ -n "$existing_id" ]; then
  echo "Updating existing release $existing_id for v$version"
  http_status=$(curl --request PATCH \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer $ghtoken" \
    --http1.1 \
    --write-out '%{http_code}' \
    --output curl_response.json \
    "https://api.github.com/repos/michaelyagi/shashin/releases/$existing_id" \
    -d @shashin_release_data.json)
else
  echo "Creating new release for v$version"
  http_status=$(curl --request POST \
    --header 'Content-Type: application/json' \
    --header "Authorization: Bearer $ghtoken" \
    --http1.1 \
    --write-out '%{http_code}' \
    --output curl_response.json \
    "https://api.github.com/repos/michaelyagi/shashin/releases" \
    -d @shashin_release_data.json)
fi

cat curl_response.json

# Cleanup files
rm -f shashin_release_data.json shashin_release_notes.txt curl_response.json

if [ "$http_status" -lt 200 ] || [ "$http_status" -gt 299 ]; then
  echo "GitHub release creation failed with HTTP $http_status"
  exit 1
fi
