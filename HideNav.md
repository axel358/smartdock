### Hide Android navigation buttons:

**With root:**
- From Smart Dock advanced settings
- Edit /system/build.prop and add qemu.hw.mainkeys=1
- Run "echo qemu.hw.mainkeys=1 >> /system/build.prop"

**Without root**
- Mount the system partition on Linux, edit /system/build.prop and add qemu.hw.mainkeys=1
- For waydroid users, from Linux run waydroid prop set qemu.hw.mainkeys 1

**With LSPosed (might help on Android 11+):**

- Install [initrd-magisk](https://github.com/HuskyDG/initrd-magisk) by HuskyDG following installation wiki.

- Install [LSPosed](https://github.com/LSPosed/LSPosed/releases) using magisk manager.
([Riru](https://github.com/RikkaApps/Riru/releases) magisk module required for riru version of lsposed.)

- Install and enable GravityBox from LSPosed modules repository, and reboot.
(Make sure to choose suitable variant for running android version.)

- Open GravityBox, go to;
 - Display tweaks > Expanded desktop mode,
and choose "Hide navigation bar"

- Now you can toggle expanded desktop mode from power menu.
(use Ctrl+Alt+Del for power menu)

*. Alternatively you can toggle Navigation bar tweaks from GravityBox and you can change height and width of navbar to 0%.
