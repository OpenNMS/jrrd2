# JRrd2

Jrrd2 provides a native interface for Java to [rrdtool](http://oss.oetiker.ch/rrdtool).

It is a rewrite and successor of [jrrd](https://github.com/OpenNMS/jrrd).

## Building

Requires maven (tested with 3.8.8) and cmake (tested with 3.28.3)

```
make
```
The dist/ folder should now contain both **jrrd2-api-VERSION.jar** and **libjrrd2.so**.

## Packaging

Requires [fpm](https://github.com/jordansissel/fpm) (tested with 1.15.1)

```
make deb-pkg
make rpm-pkg
```
The dist/ folder should now contain both .deb and .rpm packages.
