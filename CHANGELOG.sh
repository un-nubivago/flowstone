#! /bin/sh

VERSION=`grep -E "\bversion\b" gradle.properties | cut -d'=' -f2 | cut -d'+' -f1`

echo $VERSION

cat CHANGELOG.md \
    | sed -n "/## \[${VERSION}\]/,/## \[/{//b;p}" \
    | sed -e :a -e '/./,$!d;/^\n*$/{$d;N;};/\n$/ba'
