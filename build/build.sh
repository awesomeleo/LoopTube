#!/bin/sh

# Remove BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/BuildConfig.java
rm ../Loop/gen/com/kskkbys/loop/R.java

# Build with ant
cd ../facebook-android-sdk-3.0/facebook
android update project --path ./

cd ../../ActionBarSherlock/library
android update project --path ./

cd ../../Android-RateThisApp/library
android update project --path ./

cd ../../Loop
android update project --path ./
ant clean
ant release

