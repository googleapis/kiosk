# Kiosk Android App

The kiosk app runs on Android phones or an
[Android Things](https://developer.android.com/things/) device with suitable
display hardware, such as the NXP i.MX7D Starter Kit.

When you run the application if will try to register itself as a new kiosk.
Once registered, you may use the kiosk API to send content to the device.

## Running

It's easiest to get started using Android Studio and an emulator.

First, open the project in Android Studio. Then start the Kiosk server locally:

```bash
$ server
```

Next, run the 'mobile' target on an emulator. The app will look for a kiosk
server running on `10.0.2.2:8080`, which will be your local machine when
running in the emulator. If you want to change the address then click on the
gear icon in the upper right corner of the app.

Finally, send some content to the kiosk. The kiosk_id will be visible in the
top left hand corner of the app. For example:

```bash
$ k create sign robot --text=robot --image="Android_Robot.png"
$ k set sign <sign_id> for kiosk <kiosk_id>
```

## Android Things

You can install the "things" version of the app on a NXP i.MX7D Starter Kit.
The installation process is the same. Connect the device and use adb or Android
Studio to install the application.

The things version of the app is similar to the mobile version. You can change
how the image is scaled by touching the "A" button or reset the kiosk by
pressing the "C" button three times in a row. Oh, and it beeps!

### Flashing

The build used for demonstration purposes is available here and can be flashed
on a device by:

Before flashing, ensure that you have a recent version of the Android SDK
installed and that you have the ANDROID_HOME environment variable set.

1. Go to the Android Things Console and find the
   [available builds](https://partner.android.com/things/console/#/kbmc8e/model/rouoar/build).
2. Select the version to use (the latest is the first entry).
3. Click the download link and download the "development" version.
4. Connect the device to your machine and wait for it to power up.
5. Reboot to the bootloader:
   ```bash
   $ adb reboot bootloader
   ```
6. Wait for the device to appear:
   ```bash
   $ fastboot devices
   # ready when this command has a non-empty response>
   ```
7. Flash the device:
   ```bash
   $ unzip <image>.zip
   $ ./flash-all.sh
   ```

The device will reboot when the process is complete and the kiosk app will
load. After flashing, it's a good idea to set up the Wifi on the device as
described in the next section.

### Wifi

There is no UI for setting up the Wifi connection on the device. To connect it
to a network plug the device into a USB port and run the following (this only
needs to be done once):

```bash
$ adb shell
$ am startservice \
    -n com.google.wifisetup/.WifiSetupService \
    -a WifiSetupService.Connect \
    -e ssid <ssid_of_network> \
    -e passphrase <password>
```

More information is
[available here](https://developer.android.com/things/hardware/wifi-adb).

## Notes

The app will remember the kiosk id that it's assigned after it has registered.
If you stop the kiosk server this information will be lost and the kiosk app
will show an error unless you re-register a new kiosk with the same id. You can
"reset" the app by pressing the "Reset Kiosk" button from the overflow menu at
the top right hand corner of the screen.
