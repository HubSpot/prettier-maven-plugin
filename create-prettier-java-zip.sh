#!/bin/bash

set -e

if [[ $# -eq 0 ]] ; then
    echo "Usage: $0 [prettier-java-version]"
    exit 1
fi

dir=`mktemp -d -t prettier-java`
cd $dir
mkdir prettier-java && cd prettier-java
echo "{\"dependencies\": {\"prettier\": \"1.18.2\", \"prettier-plugin-java\": \"$1\"}}" > package.json
npm install
cd ..
zip_name="prettier-java-$1.zip"
zip -r $zip_name prettier-java
printf "\n\nprettier-java zip available at:\n$dir/$zip_name\n"
