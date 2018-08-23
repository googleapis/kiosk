# Kiosk Android App

The kiosk app runs on Android phones or an [Android Things](https://developer.android.com/things/)
device with suitable display hardware, such as the NXP i.MX7D Starter Kit. (TODO: Android 
Things version isn't yet complete).

When you run the application if will try to register itself as a new kiosk. Once registered,
you may use the kiosk API to send content to the device.

## Running

It's easiest to get started using Android Studio and an emulator. 

First, open the project in Android Studio. Then start the Kiosk server locally:

```bash
$ server
```

Next, run the 'mobile' target on an emulator. The app will look for a kiosk server running 
on `10.0.2.2:8080`, which will be your local machine when running in the emulator. If you
want to change the address then click on the gear icon in the upper right corner of the
app.

Finally, send some content to the kiosk. The kiosk_id will be visible in the top
left hand corner of the app. For example:

```bash
$ k create sign robot --text=robot --image="Android_Robot.png"
$ k set sign <sign_id> for kiosk <kiosk_id>
```

## Notes

The app will rember the kiosk id that it's assigned after it has registed. If you stop the kiosk server
this information will be lost and the kiosk app will show an error unless you re-register
a new kiosk with the same id. You can "reset" the app by uninstalling and reinstalling the
app, and we might add a button for it in the future!
