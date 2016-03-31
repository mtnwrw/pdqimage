
Android Real-Time Image Compression Library
===========================================

This library may be used for compressing camera images in real-time on
Android/arm architectures. The compression algorithms are hand-tuned using
ARMv7/NEONv3 assembler code and are able to compress:
 * YUV 420 8-Bit Images
 * RAW images with bit-depth of up to 14 bits (embedded in 16-bit words)

Prerequisites
-------------

- Android Build Tools v21
- Android SDK v21
- Android Support Repository
* For convenience: Android Studio v1.x (you may use vi of course)

Contents
--------

This repository consists of a small example application demonstrating the use
of the libraries API (cameraexample). The Java-bindings to the native ARM
library can be found in the org.mtnwrw.pdqimg subfolder.



License
-------

Copyright 2016 Martin Wawro

The example-part is licensed under a 2-clause BSD license whereas the library
itself (and the Java-bindings) have a proprietary license, rendering the use
of the library as free for non-commercial purposes.

