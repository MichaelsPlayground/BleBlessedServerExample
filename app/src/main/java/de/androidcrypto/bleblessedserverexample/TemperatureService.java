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
     * 03: in BluetoothServer add some lines (see there step 03)
     */

    public static final UUID ENVIRONMENTAL_SENSING_SERVICE_UUID = UUID.fromString("0000181A-0000-1000-8000-00805f9b34fb");

    // temperature in Celsius or Fahrenheeit, optional with a TimeStamp
    // public static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A1C-0000-1000-8000-00805f9b34fb");
    // see https://github.com/oesmith/gatt-xml/blob/4fd2ede1d3da9365fdc6dec89290c346581a03f9/org.bluetooth.characteristic.temperature_measurement.xml

    // temperature in Celsius only
    public static final UUID TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A6E-0000-1000-8000-00805f9b34fb");
    // see: https://github.com/oesmith/gatt-xml/blob/master/org.bluetooth.characteristic.temperature.xml

    private @NotNull final BluetoothGattService service = new BluetoothGattService(ENVIRONMENTAL_SENSING_SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    // uses indication private @NotNull final BluetoothGattCharacteristic measurement = new BluetoothGattCharacteristic(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
    // uses indicate
    private @NotNull final BluetoothGattCharacteristic measurement = new BluetoothGattCharacteristic(TEMPERATURE_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_INDICATE, PERMISSION_READ);
    private @NotNull final Handler handler = new Handler(Looper.getMainLooper());
    private @NotNull final Runnable notifyRunnable = this::notifyTemperature;
    private int currentTemperature = 22;

    TemperatureService(@NotNull BluetoothPeripheralManager peripheralManager) {
        super(peripheralManager);
        service.addCharacteristic(measurement);
        measurement.addDescriptor(getCccDescriptor());
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

            // this is for temperature in Celsius ("00002A6E-0000-1000-8000-00805f9b34fb")
            BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
            parser.setFloatValue(currentTemperature, 2);
            return new ReadResponse(GattStatus.SUCCESS, parser.getValue());
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




        // this is for temperature in Celsius ("00002A6E-0000-1000-8000-00805f9b34fb")
        BluetoothBytesParser parser = new BluetoothBytesParser(ByteOrder.LITTLE_ENDIAN);
        parser.setFloatValue(currentTemperature, 2);
        notifyCharacteristicChanged(parser.getValue(), measurement);
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




}
