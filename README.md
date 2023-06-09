<div align="center">
  <h1>Smart Dock</h1>
  A user-friendly desktop mode launcher that offers a modern and customizable user interface
</div>

![Screenshot](fastlane/metadata/android/en-US/images/phoneScreenshots/1.png)

## Main features
- Very customizable, icons, colors, shapes, sounds
- Multi window support
- Keyboard shortcuts
- Support for both desktop and tablet layouts
- Compatible with all Android versions since Lollipop, no root required

## Install

[<img src="https://fdroid.gitlab.io/artwork/badge/get-it-on.png"
    alt="Get it on F-Droid"
    height="80">](https://f-droid.org/packages/cu.axel.smartdock)

You can grab the latest release from F-Droid

Note: The application should be installed as a system app in order to obtain the right permissions.
Without this functionallity will be limited.

## Usage

**Hiding Android navigation buttons:**

You should also hide the Android navigation buttons. There are several ways to do this

**With root:**
- From Smart Dock advanced settings
- Edit /system/build.prop and add qemu.hw.mainkeys=1
- Run "echo qemu.hw.mainkeys=1 >> /system/build.prop"

**Without root**
- Mount the system partition on Linux, edit /system/build.prop and add qemu.hw.mainkeys=1  
- For waydroid users, from Linux run waydroid prop set qemu.hw.mainkeys 1

**With LSPosed (might help on Android 11+):**

[See LSPosed](LSPosed.md)

The app uses an accessibility service to capture keyboard input, if that service is crashed you might need to re-enable it and/or restart the system.

## Contributing

[See contributors](Contributors.md)


## Support

Telegram support group: https://t.me/smartdock358
