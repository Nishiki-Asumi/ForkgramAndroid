#!/usr/bin/env bash

ANDROID_SDK_ROOT=${1:-SDK}
ANDROID_NDK_VERSION=${2:-25.2.9519653}
TDLIB_SOURCE_DIR=${3:-$(dirname $0)}
ABIS=${4:-"arm64-v8a armeabi-v7a x86_64 x86"}  # Default to all ABIs if not specified

cur_dir="`pwd`"

if [ ! -d "$ANDROID_SDK_ROOT" ] ; then
  echo "Error: directory \"$ANDROID_SDK_ROOT\" doesn't exist."
  exit 1
fi

if [ ! -d "$TDLIB_SOURCE_DIR" ] ; then
  echo "Error: TDLib source directory \"$TDLIB_SOURCE_DIR\" doesn't exist."
  exit 1
fi

OPENSSL_INSTALL_DIR="${TDLIB_SOURCE_DIR}/openssl"
mkdir -p $OPENSSL_INSTALL_DIR

ANDROID_SDK_ROOT="$(cd "$(dirname -- "$ANDROID_SDK_ROOT")" >/dev/null; pwd -P)/$(basename -- "$ANDROID_SDK_ROOT")"
ANDROID_NDK_ROOT="$ANDROID_SDK_ROOT/ndk/$ANDROID_NDK_VERSION"
OPENSSL_INSTALL_DIR="$(cd "$(dirname -- "$OPENSSL_INSTALL_DIR")" >/dev/null; pwd -P)/$(basename -- "$OPENSSL_INSTALL_DIR")"
TDLIB_SOURCE_DIR="$(cd "$TDLIB_SOURCE_DIR" >/dev/null; pwd -P)"
PATH=$ANDROID_SDK_ROOT/cmake/3.22.1/bin:$PATH

CMAKE_EXTRA_FLAGS="-DCMAKE_CXX_LINK_FLAGS=-Wl,--no-icf -DCMAKE_C_LINK_FLAGS=-Wl,--no-icf"

echo "Generating TDLib source files..."
mkdir -p $TDLIB_SOURCE_DIR/build-native-Java || exit 1
cd $TDLIB_SOURCE_DIR/build-native-Java
cmake -DTD_GENERATE_SOURCE_FILES=ON $TDLIB_SOURCE_DIR || exit 1
cmake --build . || exit 1
cd ..

rm -rf $TDLIB_SOURCE_DIR/tdlib || exit 1

echo "Building TDLib for ABIs: $ABIS"
for ABI in $ABIS ; do
  echo "Building for ABI: $ABI"
  mkdir -p $TDLIB_SOURCE_DIR/tdlib/libs/$ABI/ || exit 1

  mkdir -p $TDLIB_SOURCE_DIR/build-$ABI-Java || exit 1
  cd $TDLIB_SOURCE_DIR/build-$ABI-Java
  cmake \
    -DCMAKE_TOOLCHAIN_FILE="$ANDROID_NDK_ROOT/build/cmake/android.toolchain.cmake" \
    -DOPENSSL_INCLUDE_DIR="${cur_dir}/boringssl/include" \
    -DOPENSSL_CRYPTO_LIBRARY="${cur_dir}/boringssl/build/${ABI}/crypto/libcrypto.a" \
    -DOPENSSL_SSL_LIBRARY="${cur_dir}/boringssl/build/${ABI}/ssl/libssl.a" \
    -DCMAKE_BUILD_TYPE=MinSizeRel \
    -GNinja \
    -DANDROID_ABI=$ABI \
    -DANDROID_STL=c++_static \
    -DANDROID_PLATFORM=android-16 \
    $CMAKE_EXTRA_FLAGS \
    $TDLIB_SOURCE_DIR
  cmake --build . --target tde2e -j8
  cmake --build . --target tdutils -j8

  mkdir -p "${cur_dir}/tde2e/${ABI}/"
  cp ./td/tdutils/libtdutils.a "${cur_dir}/tde2e/${ABI}/"
  cp ./td/tde2e/libtde2e.a "${cur_dir}/tde2e/${ABI}/"

  cd ..
done

echo "Done."
