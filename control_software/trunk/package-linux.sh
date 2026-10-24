#!/bin/sh
# ---------------------------------------------------------------------------
#  Builds a stand-alone Linux version of the PRM80 control software
#  (program folder with its own Java runtime, no Java installation needed).
#
#  Before: build the project once ("Clean and Build" in NetBeans or
#          "mvn package"), so that target/prm80-control-software-*.jar exists.
#  Result: target/dist/PRM80/bin/PRM80   (copy or tar the whole PRM80 folder)
#
#  Optional: "./package-linux.sh deb" builds a .deb package instead
#            (needs dpkg-deb and fakeroot), "rpm" an .rpm (needs rpmbuild).
# ---------------------------------------------------------------------------
set -e
cd "$(dirname "$0")"

TYPE=${1:-app-image}
EXTRA=""
if [ "$TYPE" != "app-image" ]; then
    EXTRA="--linux-shortcut --linux-menu-group HamRadio"
fi

# jpackage of JAVA_HOME if JAVA_HOME is a JDK, otherwise the one in the PATH
JPACKAGE=jpackage
if [ -n "$JAVA_HOME" ] && [ -x "$JAVA_HOME/bin/jpackage" ]; then
    JPACKAGE="$JAVA_HOME/bin/jpackage"
fi
if ! command -v "$JPACKAGE" >/dev/null 2>&1; then
    echo "jpackage not found - install a JDK 21 or newer (JAVA_HOME or PATH)."
    exit 1
fi

JAR=$(cd target 2>/dev/null && ls prm80-control-software-*.jar 2>/dev/null | head -n 1)
if [ -z "$JAR" ]; then
    echo "target/prm80-control-software-*.jar not found - build the project first."
    exit 1
fi

# version from the jar name (= <version> in pom.xml), e.g. 2.0.0
VERSION=$(echo "$JAR" | sed -e 's/^prm80-control-software-//' -e 's/\.jar$//' -e 's/-SNAPSHOT$//')

rm -rf target/jpackage-input target/dist
mkdir -p target/jpackage-input
cp "target/$JAR" target/jpackage-input/

"$JPACKAGE" --type "$TYPE" --name PRM80 --app-version "$VERSION" --vendor F4FEZ \
    --description "PRM80 control software" --icon packaging/PRM80.png \
    --input target/jpackage-input --main-jar "$JAR" \
    --main-class f4fez.prm80x0.PRM80X0App \
    --add-modules java.base,java.desktop,java.logging,java.prefs \
    --jlink-options "--strip-debug --no-header-files --no-man-pages" \
    --java-options "--enable-native-access=ALL-UNNAMED" \
    $EXTRA --dest target/dist

echo
echo "Done: target/dist (version $VERSION)"
