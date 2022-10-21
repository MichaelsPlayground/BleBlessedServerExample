package de.androidcrypto.bleblessedserverexample;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_INDICATE;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;
import java.util.UUID;

import timber.log.Timber;

public class TemperatureService extends BaseService {

    /**
     * steps to create your own Service peripheral
     * 01: find the correct service UUID, here: temperature is in Environmental Sensing Service (see 16-bit UUID Numbers document)
     * 02: find the correct characteristic UUID, here: 0x2A6E Temperature Measurement in Celsius only
     * 03: set the needed properties (e.g. read/write) and permissions (e.g. read/write/notify/indicate) to the characteristic UUID
     * 04: in BluetoothServer add some lines (see there step 04)
     * 05: you need to provide the data in the correct sequence for readCharacteristic and notifyEnabled
     *     for this you need to read the specification xml-files. The right source for this is the Bluetooth website but
     *     unfortunately the data is not available anymore for free access. A source for the data is the website
     *     https://github.com/oesmith/gatt-xml, here search e.g. for
     *     "https://github.com/oesmith/gatt-xml/blob/master/org.bluetooth.characteristic.temperature_measurement.xml"
     *     to get the right order of data
     * 06: you need to get the data in the correct sequence for writeCharacteristic
     */

    public static final UUID ENVIRONMENTAL_SENSING_SERVICE_UUID = UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb");

    // temperature in Celsius or Fahrenheeit, optional with a TimeStamp
    public static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    // see https://github.com/oesmith/gatt-xml/blob/4fd2ede1d3da9365fdc6dec89290c346581a03f9/org.bluetooth.characteristic.temperature_measurement.xml

    // temperature in Celsius only
    //public static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb");
    // see: https://github.com/oesmith/gatt-xml/blob/master/org.bluetooth.characteristic.temperature.xml

    public static final UUID TEMPERATURE_FAHRENHEIT_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A20-0000-1000-8000-00805f9b34fb");

    private @NotNull final BluetoothGattService service = new BluetoothGattService(ENVIRONMENTAL_SENSING_SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    // uses indication private @NotNull final BluetoothGattCharacteristic measurement = new BluetoothGattCharacteristic(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
    // uses indicate
    private @NotNull final BluetoothGattCharacteristic measurement = new BluetoothGattCharacteristic(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_INDICATE, PERMISSION_READ);
    private @NotNull final BluetoothGattCharacteristic measurementFahrenheit = new BluetoothGattCharacteristic(TEMPERATURE_FAHRENHEIT_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_INDICATE, PERMISSION_READ);
    private @NotNull final Handler handler = new Handler(Looper.getMainLooper());
    private @NotNull final Runnable notifyRunnable = this::notifyTemperature;
    private int currentTemperature = 22;

    TemperatureService(@NotNull BluetoothPeripheralManager peripheralManager) {
        super(peripheralManager);
        service.addCharacteristic(measurement);
        measurement.addDescriptor(getCccDescriptor());

        // to provide Fahrenheit values directly
        service.addCharacteristic(measurementFahrenheit);
        measurementFahrenheit.addDescriptor(getCccDescriptor());

    }

    @Override
    public void onCentralDisconnected(@NotNull BluetoothCentral central) {
        if (noCentralsConnected()) {
            stopNotifying();
        }
    }

    @Override
    public ReadResponse onCharacteristicRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            // step 05: provide the data in the correct sequence

            // this is for temperature in Celsius or Fahrenheit ("00002A1C-0000-1000-8000-00805f9b34fb")
            byte flag = 0;
            // bit 0 = Temperature Units Flag, 0 = Celsius, 1 = Fahrenheit
            // for Celsius no bitSetting
            flag = setBitInByte(flag, 0); // set fahrenheit
            // bit 1 = Time Stamp Flag, 0 = no TimeStamp, 1 = TimeStamp present
            // for noTimeStamp no bitSetting
            // bit 2 = Temperature Type Flag, 0 = Temperature type not present, 1 = Temperature type present
            // for no Temperature type present no bitSetting
            // flag = setBitInByte(flag, 0);
            // following Temperature value (in Celsius or Fahrenheit) is FLOAT

            // as we want Fahrenheit we need to converse the data
            float temperature = celsiusToFahrenheit(currentTemperature);
            byte[] returnByte = getTemperatureValue(flag, temperature);
            return new ReadResponse(GattStatus.SUCCESS, returnByte);

            /*
            // this is for temperature in Celsius ("00002A6E-0000-1000-8000-00805f9b34fb")
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setFloatValue(currentTemperature, 2);
            return new ReadResponse(GattStatus.SUCCESS, parser.getValue());
             */
        } else if (characteristic.getUuid().equals(TEMPERATURE_FAHRENHEIT_MEASUREMENT_CHARACTERISTIC_UUID)) {
            // step 05: provide the data in the correct sequence

            // temperature in Fahrenheit
            // todo examime correct value, this is giving wrong value
            // this is for temperature in Fahrenheit (""00002A20-0000-1000-8000-00805f9b34fb"")
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setFloatValue(celsiusToFahrenheit(currentTemperature), 2);
            return new ReadResponse(GattStatus.SUCCESS, parser.getValue());

            /*
            // this is for temperature in Celsius or Fahrenheit ("00002A1C-0000-1000-8000-00805f9b34fb")
            byte flag = 0;
            // bit 0 = Temperature Units Flag, 0 = Celsius, 1 = Fahrenheit
            // for Celsius no bitSetting
            flag = setBitInByte(flag, 0); // set fahrenheit
            // bit 1 = Time Stamp Flag, 0 = no TimeStamp, 1 = TimeStamp present
            // for noTimeStamp no bitSetting
            // bit 2 = Temperature Type Flag, 0 = Temperature type not present, 1 = Temperature type present
            // for no Temperature type present no bitSetting
            // flag = setBitInByte(flag, 0);
            // following Temperature value (in Celsius or Fahrenheit) is FLOAT

            // as we want Fahrenheit we need to converse the data
            float temperature = celsiusToFahrenheit(currentTemperature);

            byte[] returnByte = getTemperatureValue(flag, temperature);

            return new ReadResponse(GattStatus.SUCCESS, returnByte);
            */

            /*
            // this is for temperature in Celsius ("00002A6E-0000-1000-8000-00805f9b34fb")
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setFloatValue(currentTemperature, 2);
            return new ReadResponse(GattStatus.SUCCESS, parser.getValue());
             */
        }
        return super.onCharacteristicRead(central, characteristic);
    }

    @Override
    public void onNotifyingEnabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            notifyTemperature();
        }
    }

    @Override
    public void onNotifyingDisabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            stopNotifying();
        }
    }

    private void notifyTemperature() {
        currentTemperature += (int) ((Math.random() * 10) - 5);
        if (currentTemperature > 40) currentTemperature = 40;
        // step 05: provide the data in the correct sequence

        // todo check for UUIDs to provide different data

        // this is for temperature in Celsius or Fahrenheit ("00002A1C-0000-1000-8000-00805f9b34fb")
        byte flag = 0;
        // bit 0 = Temperature Units Flag, 0 = Celsius, 1 = Fahrenheit
        // for Celsius no bitSetting
        flag = setBitInByte(flag, 0); // set fahrenheit
        // bit 1 = Time Stamp Flag, 0 = no TimeStamp, 1 = TimeStamp present
        // for noTimeStamp no bitSetting
        // bit 2 = Temperature Type Flag, 0 = Temperature type not present, 1 = Temperature type present
        // for no Temperature type present no bitSetting
        // flag = setBitInByte(flag, 0);
        // following Temperature value (in Celsius or Fahrenheit) is FLOAT

        // as we want Fahrenheit we need to converse the data
        float temperature = celsiusToFahrenheit(currentTemperature);

        byte[] returnByte = getTemperatureValue(flag, temperature);
        notifyCharacteristicChanged(returnByte, measurement);

        /*
        // this is for temperature in Celsius ("00002A6E-0000-1000-8000-00805f9b34fb")
        BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
        parser.setFloatValue(currentTemperature, 2);
        notifyCharacteristicChanged(parser.getValue(), measurement);
        */

        handler.postDelayed(notifyRunnable, 1000); // every second a new value
        System.out.println("** newTemp: " + currentTemperature);
        Timber.i("new temp: %d", currentTemperature);
    }

    private void stopNotifying() {
        handler.removeCallbacks(notifyRunnable);
    }

    @Override
    public @NotNull BluetoothGattService getService() {
        return service;
    }

    @Override
    public String getServiceName() {
        return "Temperature Service";
    }

    /**
     * utilities for manipulating the data
     */

    private byte[] getTemperatureValue(byte flag, float temperature) {
        byte[] flagByte = new byte[1];
        flagByte[0] = flag;
        BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
        parser.setFloatValue(temperature, 2);
        byte[] floatValueByte = parser.getValue();
        int floatValueByteLength = floatValueByte.length;
        byte[] returnByte = new byte[1 + floatValueByteLength]; // 1 byte for flag, xx bytes for floatValue
        System.arraycopy(flagByte, 0, returnByte, 0, 1);
        System.arraycopy(floatValueByte, 0, returnByte, 1, floatValueByteLength);
        return returnByte;
    }

    public float celsiusToFahrenheit (@NotNull float celsius) {
        celsius = celsius * 9;
        celsius = celsius / 5;
        return (celsius + 32);
    }

    public float fahrenheitToCelsius (@NotNull float fahrenheit) {
        fahrenheit = fahrenheit - 32;
        fahrenheit = fahrenheit / 9;
        return (fahrenheit * 5);
    }


// position is 0 based starting from right to left
    public static byte setBitInByte(byte input, int pos) {
        return (byte) (input | (1 << pos));
    }

    // position is 0 based starting from right to left
    public static byte unsetBitInByte(byte input, int pos) {
        return (byte) (input & ~(1 << pos));
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte b, int n) {
        int mask = 1 << n; // equivalent of 2 to the nth power
        return (b & mask) != 0;
    }

    // https://stackoverflow.com/a/29396837/8166854
    public static boolean testBit(byte[] array, int n) {
        int index = n >>> 3; // divide by 8
        int mask = 1 << (n & 7); // n modulo 8
        return (array[index] & mask) != 0;
    }
}
