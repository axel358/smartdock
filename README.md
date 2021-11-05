# smartdock
A launcher for Android x86

This project is a launcher for Android x86 to ease user navigation.

Building:
The project itself was created using AIDE on Android x86. To build just clone the project and import it to AIDE.
You might require additional steps to build on Android Studio

To support android versions lower than Pie you must compile against SDK 27

Using:
The application should be installed as a system app in order to obtain the right permissions.
Without this functionallity will be limited.

You should also hide the Android navigation buttons. One way to do this is to locate and edit build.prop on your system,
usually under /system, add qemu.hw.mainkeys=1 and then reboot
