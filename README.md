Drive Copy
==========

Drive Copy is a simple command line utility to download, replace and upload Google Drive binary files.

Getting started
---------------

Download the [latest release](https://pkg.nharyes.net/drivecopy/drivecopy-1.2.0.jar) and see the examples below.  
The first time Drive Copy tries to reach Google it will ask authorization codes: see [Setup](https://github.com/Gherynos/DriveCopy/wiki/Setup) steps.  
For the complete list of options see the [How To](https://github.com/Gherynos/DriveCopy/wiki/How-To).

Features
--------

* download binary files from Google Drive
* upload binary files to Google Drive
* replace Google Drive binary files
* backup directories to Google Drive

Usage
-----

Download a file:

```bash
$ java -jar drivecopy.jar -f <local_file> download <drive_file>
```

Upload a file:

```bash
$ java -jar drivecopy.jar -f <local_file> upload <drive_file>
```

Replace a file:

```bash
$ java -jar drivecopy.jar -f <local_file> replace <drive_file>
```

Copyright and license
---------------------

Copyright 2012-2013 Luca Zanconato (<luca.zanconato@nharyes.net>)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License in the LICENSE file, or at:

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
