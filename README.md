
Android Real-Time Image Compression Library
===========================================

This library may be used for compressing camera images in real-time on
Android/arm architectures. The compression algorithms are hand-tuned using
ARMv7/NEONv3 assembler code and are able to compress:
 * YUV 420 8-Bit Images
 * RAW images with bit-depths of up to 14 bits (embedded in 16-bit words)

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

Intended Audience
-----------------

This project might be suitable for developers of camera apps that require to rapidly
capture images and need some means to instantaneously compress the acquired images
before using up all the RAM. In particular, this library might come in quite handy
if CFA (Bayer raw) images shall be acquired and the storage of the uncompressed raw
data is not an option (see the [Wiki page](../../wiki)) for compression efficiency.

Please note that the main emphasis of the code in this is repository is not the
included example application but the native library and its Java bindings. The 
example application is nothing but a vehicle that is used to show how to trigger
the compression/decompression API and by no means does it constitute a complete 
(or even incomplete) camera application.


Performance
-----------

The compression speed on 8 Megapixel RAW (RGGB) images on a Qualcomm Snapdragon 800
CPU @ 2.2GHz (LG Nexus 5, 2013 model) is well within the range of 30 fps.
A simple benchmark on a real-world raw test image yielded the following timings:
 * 23 ms per image for low quality (41 fps)
 * 27 ms per image for medium quality (37 fps)
 * 31 ms per image for high quality (32 fps)

For 8-bit YUV 420 data, the timings are:
 * 30.5ms per image for low quality (32 fps)
 * 35.1ms per image for medium quality (28 fps)
 * 38.7ms per image for high quality (25 fps)

See the [Wiki page](../../wiki) for more details.


Image Quality
-------------

As the used compression algorithm performs lossy image compression, the image quality
is an additional concern. Naturally, some sacrifices are made in order to achieve
high compression throughput. However the image quality (as measured with PSNR) is
within an acceptable range (the source image was a RAW image, but the PSNR measurements
were performed on demosaiced and color-corrected 8-bit versions of the original image):

 * Low quality: 32.73 dB
 * Medium quality: 35.67 dB
 * High quality: 39.69 dB

For 8-Bit YUV420 data the PSNRs are:

 * Low quality: 39.55 dB
 * Medium quality: 41.79 dB
 * High quality: 43.31 dB

Again, see the [Wiki page](../../wiki) for more details. A complete evaluation on a
test-image corpus including some meaningful statistics is underway.



License
-------

Copyright (c) 2016 Martin Wawro

The example-part is licensed under a 2-clause BSD license whereas the library
itself (and the Java-bindings) have a proprietary license, rendering the use
of the library as free for non-commercial purposes.

[![Analytics](https://ga-beacon.appspot.com/UA-79853613-1/mtnwrw/pdqimg_main?pixel)]()
