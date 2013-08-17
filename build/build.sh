#!/bin/sh

# Remove BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/R.java

# Build with ant
cd ../

android update project --path ./Android-RateThisApp/library
android update project --path ./appcompat
android update project --path ./Loop

cd ./Loop
ant clean
ant release

