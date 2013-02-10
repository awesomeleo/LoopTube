#!/bin/sh

# Remove BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/R.java

# Build with ant
cd ../Loop
ant clean
ant release

