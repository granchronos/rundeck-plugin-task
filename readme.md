Rundeck
========

| Travis | Deb | RPM | War |
|--------|-----|-----|-----|
|[![Travis CI](https://travis-ci.org/rundeck/rundeck.svg?branch=master)](https://travis-ci.org/rundeck/rundeck/builds#)|[Download](https://www.rundeck.com/downloads)|[Download](https://www.rundeck.com/downloads)|[Download](https://www.rundeck.com/downloads)|

Rundeck is an open source automation service with a web console,
command line tools and a WebAPI.
It lets you easily run automation tasks across a set of nodes.

* Site: <https://www.rundeck.com>

* Latest documentation: <https://docs.rundeck.com/docs/>

* Get Help: <https://docs.rundeck.com/docs/introduction/getting-help.html>


See the [Release Notes](https://docs.rundeck.com/docs/history/) for the latest version information.


To Build:
=====

Primary build is supported with gradle. More info in the [wiki](https://github.com/rundeck/rundeck/wiki/Building-and-Testing).

Gradle Build
=====

    ./gradlew build

Gradle Version
=====

    X.Y => 0.1

Artifacts:

* `rundeckapp/build/libs/rundeck-plugin-tasking-X.Y.jar`


RPM and DEB package builds
=======

To build .rpm and .deb packages, you must first clone [the rundeck packaging repo](https://github.com/rundeck/packaging) into the rundeck repo.
A sample list of simple build steps is below, where $RELEASE_VERSION is the version you want
to build, i.e. 3.4.3

    git clone https://github.com/rundeck/rundeck
    cd rundeck
    git checkout refs/tags/v$RELEASE_VERSION
    git clone https://github.com/rundeck/packaging
    ./gradlew build -Penvironment=release
    cd packaging

For 3.2.x builds, check out the `maint-3.4.x` branch of `packaging`

    git checkout maint-3.4.x
    mkdir -p artifacts
    cp ../rundeckapp/build/libs/rundeck*.war artifacts/
    ./gradlew -PpackageRelease=$RELEASE_VERSION clean packageArtifacts

To build clean:

    make clean

Installation
======

There are several install options: a self-contained war file, or RPM, or Debian.

To start from the rundeck-X.Y.war, put it in a directory named ~/rundeck, then execute:

    java -Xmx1024m -Xms256m -XX:MaxMetaspaceSize=256m -server -jar rundeck-plugin-tasking-X.Y.jar

If you'd like to install via RPM, you can use Yum:

    rpm -Uvh http://repo.rundeck.org/latest.rpm
    yum install rundeck

OR install directly from RPM:

    rpm -ivh rundeck-2.0.0-xxx.rpm rundeck-config-2.0.0-xxx.rpm

Once the RPM is installed, execute:

    sudo /etc/init.d/rundeckd start

The server should launch on port 4440, with default username/password of `admin/admin`.

For Debian, download the .deb from the [downloads page](https://www.rundeck.com/downloads), then run:

    dpkg -i rundeck-2.0.0-x.deb

* For more info and configuration information, see the [Rundeck docs](https://docs.rundeck.com/docs).

Requirements
=======

Java 8 (openjdk, oracle)

Node JS 12

Documentation
======

Available online at <https://docs.rundeck.com/docs>

FAQ: <https://github.com/rundeck/rundeck/wiki/FAQ>

Development
======

Refer to the [IDE Development Environment](https://github.com/rundeck/rundeck/wiki/IDE-Development-Environment) to get set up using IntelliJ IDEA or Eclipse/STS.

* [Issue tracker](https://github.com/rundeck/rundeck/issues) at github.com

Do you have changes to contribute? Please see the [Development](https://github.com/rundeck/rundeck/wiki/Development) wiki page.

License
======

Copyright 2020 Rundeck, Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.