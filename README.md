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

Hiding Android navigation buttons:
You should also hide the Android navigation buttons. There are several ways to do this
With root:
- On the Advanced settings
- Running setprop qemu.hw.mainkeys 1
Without root
- Mount the system partition on Linux, edit /system/build.prop and add qemu.hw.mainkeys=1  
- For waydroid users, from Linux run waydroid prop set qemu.hw.mainkeys 1

The app uses an accessibility service to capture keyboard input, if that service is crashed you might need to re-enable it and/or restart the system.
