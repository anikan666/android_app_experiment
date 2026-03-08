#!/bin/sh
APP_BASE_NAME=$(basename "$0")
DIRNAME=$(dirname "$0")

WRAPPER_PROPERTIES="$DIRNAME/gradle/wrapper/gradle-wrapper.properties"
DIST_URL=$(grep 'distributionUrl' "$WRAPPER_PROPERTIES" | sed 's/distributionUrl=//;s/\\//g')
ZIP_NAME=$(basename "$DIST_URL" .zip)

GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
DIST_DIR="$GRADLE_USER_HOME/wrapper/dists/$ZIP_NAME"

# The zip contains a directory named like 'gradle-8.2'
GRADLE_VERSION=$(echo "$ZIP_NAME" | sed 's/-bin$//' | sed 's/-all$//')
GRADLE_HOME="$DIST_DIR/$GRADLE_VERSION"

if [ ! -d "$GRADLE_HOME" ]; then
    mkdir -p "$DIST_DIR"
    echo "Downloading $DIST_URL..."
    curl -sfL -o "$DIST_DIR/$ZIP_NAME.zip" "$DIST_URL" || wget -q -O "$DIST_DIR/$ZIP_NAME.zip" "$DIST_URL"
    echo "Extracting..."
    unzip -q -o "$DIST_DIR/$ZIP_NAME.zip" -d "$DIST_DIR"
    rm -f "$DIST_DIR/$ZIP_NAME.zip"
fi

exec "$GRADLE_HOME/bin/gradle" "$@"
