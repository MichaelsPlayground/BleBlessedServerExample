# BLE Blessed Client sample

This is a sample app build with the BLE library **Blessed-Android**.

The library is available here: https://github.com/weliem/blessed-android

A complex sample is included and this app is a smaller version but with some more 
access to the data (or better the BLE device).

If you need a BLE Client (or peripheral) the library supports this as well, a sample app is 
available here: https://github.com/weliem/blessed-android.

If you should need more information on how to setup a peripheral see here: 
https://github.com/weliem/blessed-android/blob/master/SERVER.md

A BLE Glucose device is available here: 
https://www.diabetes.ascensia.de/produkte/contour-next-mg/

This project has an additional Temperature Service, some information on this:

taken from: https://github.com/atc1441/ATC_MiThermometer/issues/150

As per:
https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf

Found via the assigned numbers: https://www.bluetooth.com/specifications/assigned-numbers/

2A1C, with additional values to determine if C or F.

https://github.com/oesmith/gatt-xml/blob/master/org.bluetooth.characteristic.temperature_measurement.xml

2A6E, for just sending C.

https://github.com/oesmith/gatt-xml/blob/master/org.bluetooth.characteristic.temperature.xml

-2A1F - sint16 temperature in 0.1 °C
-2A6E - sint16 temperature in 0.01 °C
-2A1C - float


https://stackoverflow.com/questions/66005510/what-does-the-returned-value-of-ble-thermometer-mean

I am using xiaomi LYWSD03MMC , I have got temperature of this device by BLE characteristics and it shows : 21 0a 17 7e 0b , However I know this is hex value but unfortunately i can't understand what does it mean.I only know the number 17, which is the amount of humidity, which is hex, and when I convert it to decimal it returns 23.

According to this script from github the first two values describe the temperature and need to be converted to little endian. This would result in a hex value of 0a21 which in decimal is 2539. This needs to be divided by 100 to give you a temperature of 25.39 degrees.

As you said, the third value is humidity, the last two values describe the battery voltage. Converted to little endian (0b7e) and decimal (2942), this value needs to be divided by 1000 and gives you a voltage of 2.942


https://stackoverflow.com/questions/64474502/how-do-you-build-a-ble-app-when-you-dont-have-access-to-the-official-gatt-xml-f

All of the GATT xml specifications can be found here: https://github.com/oesmith/gatt-xml



