# JRrd2

Jrrd2 provides a native interface for Java to [rrdtool](http://oss.oetiker.ch/rrdtool).

It is a rewrite and successor of [jrrd](https://github.com/OpenNMS/jrrd).

## 👩‍🔧 Building

Requirement:
* maven (tested with 3.9.11)
* cmake (tested with 4.1.2)
* OpenJDK 17

```bash
make
```
The dist/ folder should now contain both **jrrd2-api-VERSION.jar** and **libjrrd2.so**.

Cleanup:

```bash
make clean
```

## 📦 Packaging

Requirements:
* [fpm](https://github.com/jordansissel/fpm) (tested with 1.15.1)
* rpmbuild binary

```bash
make deb-pkg rpm-pkg
```
The dist/ folder should now contain both .deb and .rpm packages.
