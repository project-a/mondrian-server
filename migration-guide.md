# Mondrian Server Migration Guide

The Mondrian Server is build against specific versions of Penthaho Mondrian
and meteorite.bi Saiku. This document describes the necessary steps to
upgrade one of these components to a newer version.


## Updating Mondrian

To update Mondrian to a newer version, follow these steps:

**1. Determine New Mondrian Version:**
Check the [Pentaho GitHub](https://github.com/pentaho/mondrian/releases)
for available releases and the [Penthaho Nexus Repository](https://nexus.pentaho.org/#browse/browse:omni:pentaho%2Fmondrian)
for available builds.

**2. Update build.gradle:**
Edit the `build.gradle` file and set the property `ext.mondrianVersion`
to the version number from the Pentaho Nexus Repository.

**3. Check Saiku Integration: **
Make sure that the class `mondrian.olap4j.SaikuMondrianHelper` still compiles.
This file provides the connector Saiku uses to access the internal Mondrian APIs.
If not, check if a newer version of the [original file](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-olap-util/src/main/java/mondrian/olap4j/SaikuMondrianHelper.java) exists.

**4. Verify Patch:**
This project contains a patched version of `mondrian.rolap.SmartMemberReader`.
This fixes an issue with member ordering we experienced in production.
Check if there were any changes in the [original file](https://github.com/pentaho/mondrian/commits/master/mondrian/src/main/java/mondrian/rolap/SmartMemberReader.java)
and apply the same changes to the patched file. The modified lines are marked
with `[PATCH START]` and `[PATCH END]`.


## Updating Saiku Version

To update Saiku to a newer version, follow these steps:

**1. Determine New Saiku Version:**
Check the [Saiku GitHub](https://github.com/OSBI/saiku/releases)
for available releases.

**2. Update build.gradle:**
Edit the `build.gradle` file and set the property `ext.saikuReleaseTag`
to the selected release number.

**3. Update dependencies: **
The included `build.gradle` contains the merged dependencies from multiple Saiku repositories.
Check if there were any changes in the '<dependencies>' section in one of the following 'pom.xml' files:
 - [/saiku-core/saiku-olap-util/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-olap-util/pom.xml)
 - [/saiku-core/saiku-service/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-service/pom.xml)
 - [/saiku-core/saiku-web/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/saiku-web/pom.xml)
 - [/saiku-core/pom.xml](https://github.com/OSBI/saiku/blob/master/saiku-core/pom.xml)
 - [/pom.xml](https://github.com/OSBI/saiku/blob/master/pom.xml)

Update the 'dependencies' section of the `build.gradle` file accordingly.

**4. Check Spring Components: **
The package `com.projecta.mondrianserver.saiku` contains several
Spring components that override the original Saiku implementation to
remove administrative functionality. Check if these classes still compile
and fix them if necessary.

**5. Update Patched Frontend Files: **
The directory '/src/main/webapp/js' contains 2 patched JavaScript files
with minor usability improvements:

 - [SessionWorkspace.js](https://github.com/OSBI/saiku/blob/master/saiku-ui/js/saiku/models/SessionWorkspace.js)
 - [SaveQuery.js](https://github.com/OSBI/saiku/blob/master/saiku-ui/js/saiku/views/SaveQuery.js)

Checkout the updated version of these files and reapply the patch (lines are marked with `// PATCHED`)

