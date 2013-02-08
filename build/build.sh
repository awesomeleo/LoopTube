#!/bin/sh

# Remove BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/BuildConfig.java

# Build with ant
cd ../Loop
ant clean
ant release

