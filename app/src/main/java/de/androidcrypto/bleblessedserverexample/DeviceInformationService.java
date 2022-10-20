package de.androidcrypto.bleblessedserverexample;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED;
import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ_ENCRYPTED_MITM;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_WRITE; // new
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_WRITE; // new

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Build;

import androidx.annotation.NonNull;

import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import timber.log.Timber;

/**
 * this services is changed from the original server to accept a write command to the
 * MODEL_NUMBER_CHARACTERISTIC_UUID
 */

class DeviceInformationService extends BaseService {

    public static final UUID DEVICE_INFORMATION_SERVICE_UUID = UUID.fromString("0000180A-0000-1000-8000-00805f9b34fb");
    public static final UUID MANUFACTURER_NAME_CHARACTERISTIC_UUID = UUID.fromString("00002A29-0000-1000-8000-00805f9b34fb");
    public static final UUID MODEL_NUMBER_CHARACTERISTIC_UUID = UUID.fromString("00002A24-0000-1000-8000-00805f9b34fb");

    private @NotNull final BluetoothGattService service = new BluetoothGattService(DEVICE_INFORMATION_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
    private byte[] newModel; // new

    public DeviceInformationService(@NotNull BluetoothPeripheralManager peripheralManager) {
        super(peripheralManager);

        BluetoothGattCharacteristic manufacturer = new BluetoothGattCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID, PROPERTY_READ, PERMISSION_READ);

        // this will ask for pairing /bonding with the same 6-digit pin
        //BluetoothGattCharacteristic manufacturer = new BluetoothGattCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID, PROPERTY_READ, PERMISSION_READ_ENCRYPTED_MITM);
        // this will ask for pairing /bonding with the same 6-digit pin
        //BluetoothGattCharacteristic manufacturer = new BluetoothGattCharacteristic(MANUFACTURER_NAME_CHARACTERISTIC_UUID, PROPERTY_READ, PERMISSION_READ_ENCRYPTED);

        service.addCharacteristic(manufacturer);

        // org: BluetoothGattCharacteristic modelNumber = new BluetoothGattCharacteristic(MODEL_NUMBER_CHARACTERISTIC_UUID, PROPERTY_READ, PERMISSION_READ);
        BluetoothGattCharacteristic modelNumber = new BluetoothGattCharacteristic(MODEL_NUMBER_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_WRITE, PERMISSION_READ | PERMISSION_WRITE);
        //PROPERTY_READ | PROPERTY_NOTIFY | PROPERTY_WRITE, PERMISSION_READ | PERMISSION_WRITE
        service.addCharacteristic(modelNumber);
    }

    // new
    @Override
    public GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic, byte[] value) {
        newModel = value.clone();
        return super.onCharacteristicWrite(central, characteristic, value);
    }

    // new
    @Override
    public void onCharacteristicWriteCompleted(@NonNull BluetoothCentral central, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
        System.out.println("* DeviceInformationService onCharacteristicWriteCompleted new model number written: " + new String(newModel) + " *");
    }

    @Override
    public ReadResponse onCharacteristicRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(MANUFACTURER_NAME_CHARACTERISTIC_UUID)) {
            return new ReadResponse(GattStatus.SUCCESS, Build.MANUFACTURER.getBytes());
        } else if (characteristic.getUuid().equals(MODEL_NUMBER_CHARACTERISTIC_UUID)) {
            // new for simulating write
            if (newModel != null) {
                return new ReadResponse(GattStatus.SUCCESS, newModel);
            } else {
                return new ReadResponse(GattStatus.SUCCESS, Build.MODEL.getBytes());
            }
        }
        return super.onCharacteristicRead(central, characteristic);
    }

    @Override
    public @NotNull BluetoothGattService getService() {
        return service;
    }

    @Override
    public String getServiceName() {
        return "Device Information Service";
    }
}
