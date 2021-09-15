Drive Copy
==========

Drive Copy is a simple command line utility to download, replace and upload Google Drive binary files.

![build](https://github.com/gherynos/drivecopy/workflows/build/badge.svg)

Getting started
---------------

Download the [latest release](https://github.com/gherynos/drivecopy/releases) and see the examples below.  
The first time Drive Copy tries to reach Google it will ask authorization codes: see [Setup](https://github.com/gherynos/drivecopy/wiki/Setup) steps.  
For the complete list of options see the [How To](https://github.com/gherynos/drivecopy/wiki/How-To).

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

Author
------

> GitHub [@gherynos](https://github.com/gherynos)

License
-------

Drive Copy is licensed under the [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0).
