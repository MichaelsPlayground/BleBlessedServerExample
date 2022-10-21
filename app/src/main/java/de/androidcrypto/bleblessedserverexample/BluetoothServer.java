package de.androidcrypto.bleblessedserverexample;

import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT16;
import static com.welie.blessed.BluetoothBytesParser.FORMAT_UINT8;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;

import androidx.annotation.NonNull;

import com.welie.blessed.AdvertiseError;
import com.welie.blessed.BluetoothBytesParser;
import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.BluetoothPeripheralManagerCallback;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;
import java.util.UUID;

import timber.log.Timber;

/**
 * this BluetoothServer is changed to accept a writeCharacteristic on DeviceInformation /
 * Model Number String = MODEL_NUMBER_CHARACTERISTIC_UUID
 */

@SuppressLint("MissingPermission")
class BluetoothServer {

    private static Context mContext;
    private static BluetoothServer instance = null;
    private BluetoothPeripheralManager peripheralManager;
    private final HashMap<BluetoothGattService, Service> serviceImplementations = new HashMap<>();

    // Intent constants
    public static final String BLUETOOTH_HANDLER_ADVERTISER = "androidcrypto.bluetoothhandler.advertiser";
    public static final String BLUETOOTH_HANDLER_ADVERTISER_EXTRA = "androidcrypto.bluetoothhandler.advertiser.extra";
    public static final String BLUETOOTH_HANDLER_CONNECTION = "androidcrypto.bluetoothhandler.connection";
    public static final String BLUETOOTH_HANDLER_CONNECTION_EXTRA = "androidcrypto.bluetoothhandler.connection.extra";
    public static final String BLUETOOTH_HANDLER_CURRENT_TIME = "androidcrypto.bluetoothhandler.currenttime";
    public static final String BLUETOOTH_HANDLER_CURRENT_TIME_EXTRA = "androidcrypto.bluetoothhandler.currenttime.extra";
    public static final String BLUETOOTH_HANDLER_HEART_BEAT_RATE = "androidcrypto.bluetoothhandler.heartbeatrate";
    public static final String BLUETOOTH_HANDLER_HEART_BEAT_RATE_EXTRA = "androidcrypto.bluetoothhandler.heartbeatrate.extra";
    public static final String BLUETOOTH_HANDLER_MODEL_NAME = "androidcrypto.bluetoothhandler.modelname";
    public static final String BLUETOOTH_HANDLER_MODEL_NAME_EXTRA = "androidcrypto.bluetoothhandler.modelname.extra";

    public static synchronized BluetoothServer getInstance(Context context) {
        mContext = context;
        if (instance == null) {
            instance = new BluetoothServer(context.getApplicationContext());
        }
        return instance;
    }

    private final BluetoothPeripheralManagerCallback peripheralManagerCallback = new BluetoothPeripheralManagerCallback() {
        @Override
        public void onServiceAdded(@NotNull GattStatus status, @NotNull BluetoothGattService service) {
        }

        @Override
        public @NotNull ReadResponse onCharacteristicRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                return serviceImplementation.onCharacteristicRead(central, characteristic);
            }
            return super.onCharacteristicRead(central, characteristic);
        }

        @Override
        public @NotNull GattStatus onCharacteristicWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic, @NotNull byte[] value) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                System.out.println("* onCharWrite modelName: " + new String(value));
                // new - when the model name characteristic is read the model name is shown on th UI
                if (characteristic.getUuid().equals(DeviceInformationService.MODEL_NUMBER_CHARACTERISTIC_UUID)) {
                    String data = new String(value);
                    Intent intent = new Intent(BLUETOOTH_HANDLER_MODEL_NAME);
                    intent.putExtra(BLUETOOTH_HANDLER_MODEL_NAME_EXTRA, data);
                    sendToMain(intent);
                }
                return serviceImplementation.onCharacteristicWrite(central, characteristic, value);
            }
            return GattStatus.REQUEST_NOT_SUPPORTED;
        }

        @Override
        public void onCharacteristicWriteCompleted(@NonNull BluetoothCentral central, @NonNull BluetoothGattCharacteristic characteristic, @NonNull byte[] value) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                serviceImplementation.onCharacteristicWriteCompleted(central, characteristic, value);
            }
        }

        @Override
        public @NotNull ReadResponse onDescriptorRead(@NotNull BluetoothCentral central, @NotNull BluetoothGattDescriptor descriptor) {
            BluetoothGattCharacteristic characteristic = Objects.requireNonNull(descriptor.getCharacteristic(), "Descriptor has no Characteristic");
            BluetoothGattService service = Objects.requireNonNull(characteristic.getService(), "Characteristic has no Service");

            Service serviceImplementation = serviceImplementations.get(service);
            if (serviceImplementation != null) {
                return serviceImplementation.onDescriptorRead(central, descriptor);
            }
            return super.onDescriptorRead(central, descriptor);
        }

        @NonNull
        @Override
        public GattStatus onDescriptorWrite(@NotNull BluetoothCentral central, @NotNull BluetoothGattDescriptor descriptor, @NotNull byte[] value) {
            BluetoothGattCharacteristic characteristic = Objects.requireNonNull(descriptor.getCharacteristic(), "Descriptor has no Characteristic");
            BluetoothGattService service = Objects.requireNonNull(characteristic.getService(), "Characteristic has no Service");
            Service serviceImplementation = serviceImplementations.get(service);
            if (serviceImplementation != null) {
                return serviceImplementation.onDescriptorWrite(central, descriptor, value);
            }
            return GattStatus.REQUEST_NOT_SUPPORTED;
        }

        @Override
        public void onNotifyingEnabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                serviceImplementation.onNotifyingEnabled(central, characteristic);
            }
        }

        @Override
        public void onNotifyingDisabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                serviceImplementation.onNotifyingDisabled(central, characteristic);
            }
        }

        @Override
        public void onNotificationSent(@NotNull BluetoothCentral central, byte[] value, @NotNull BluetoothGattCharacteristic characteristic, @NotNull GattStatus status) {
            Service serviceImplementation = serviceImplementations.get(characteristic.getService());
            if (serviceImplementation != null) {
                serviceImplementation.onNotificationSent(central, value, characteristic, status);
            }
            if (characteristic.getUuid().equals(CurrentTimeService.CURRENT_TIME_CHARACTERISTIC_UUID)) {
                BluetoothBytesParser parser = new BluetoothBytesParser(value, ByteOrder.LITTLE_ENDIAN);
                Date date = parser.getDateTime();
                Intent intent = new Intent(BLUETOOTH_HANDLER_CURRENT_TIME);
                intent.putExtra(BLUETOOTH_HANDLER_CURRENT_TIME_EXTRA, date.toString());
                sendToMain(intent);
            }
            if (characteristic.getUuid().equals(HeartRateService.HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
                BluetoothBytesParser parser = new BluetoothBytesParser(value);
                int flags = parser.getIntValue(FORMAT_UINT8);
                final int unit = flags & 0x01;
                // Parse heart rate
                int pulse = (unit == 0) ? parser.getIntValue(FORMAT_UINT8) : parser.getIntValue(FORMAT_UINT16);
                Intent intent = new Intent(BLUETOOTH_HANDLER_HEART_BEAT_RATE);
                intent.putExtra(BLUETOOTH_HANDLER_HEART_BEAT_RATE_EXTRA, String.valueOf(pulse));
                sendToMain(intent);
            }
        }

        @Override
        public void onCentralConnected(@NotNull BluetoothCentral central) {
            for (Service serviceImplementation : serviceImplementations.values()) {
                serviceImplementation.onCentralConnected(central);
            }
            Intent intent = new Intent(BLUETOOTH_HANDLER_CONNECTION);
            intent.putExtra(BLUETOOTH_HANDLER_CONNECTION_EXTRA, "connected to MAC: " + central.getAddress());
            sendToMain(intent);
        }

        @Override
        public void onCentralDisconnected(@NotNull BluetoothCentral central) {
            for (Service serviceImplementation : serviceImplementations.values()) {
                serviceImplementation.onCentralDisconnected(central);
            }
            Intent intent = new Intent(BLUETOOTH_HANDLER_CONNECTION);
            intent.putExtra(BLUETOOTH_HANDLER_CONNECTION_EXTRA, "DISCONNECTED from MAC: " + central.getAddress());
            sendToMain(intent);
        }

        @Override
        public void onAdvertisingStarted(@NotNull AdvertiseSettings settingsInEffect) {
            Intent intent = new Intent(BLUETOOTH_HANDLER_ADVERTISER);
            intent.putExtra(BLUETOOTH_HANDLER_ADVERTISER_EXTRA, "ON");
            sendToMain(intent);
        }

        @Override
        public void onAdvertiseFailure(@NotNull AdvertiseError advertiseError) {
            Intent intent = new Intent(BLUETOOTH_HANDLER_ADVERTISER);
            intent.putExtra(BLUETOOTH_HANDLER_ADVERTISER_EXTRA, "OFF");
            sendToMain(intent);
        }

        @Override
        public void onAdvertisingStopped() {
            Intent intent = new Intent(BLUETOOTH_HANDLER_ADVERTISER);
            intent.putExtra(BLUETOOTH_HANDLER_ADVERTISER_EXTRA, "OFF");
            sendToMain(intent);
        }
    };

    public void startAdvertising(UUID serviceUUID) {
        AdvertiseSettings advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData advertiseData = new AdvertiseData.Builder()
                .setIncludeTxPowerLevel(true)
                .addServiceUuid(new ParcelUuid(serviceUUID))
                .build();

        AdvertiseData scanResponse = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .build();

        peripheralManager.startAdvertising(advertiseSettings, advertiseData, scanResponse);
    }

    private void setupServices() {
        for (BluetoothGattService service : serviceImplementations.keySet()) {
            peripheralManager.add(service);
        }
    }

    BluetoothServer(Context context) {
        //Timber.plant(new Timber.DebugTree());

        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            Timber.e("bluetooth not supported");
            return;
        }

        final BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isMultipleAdvertisementSupported()) {
            Timber.e("not supporting advertising");
            return;
        }

        // Set the adapter name as this is used when advertising
        bluetoothAdapter.setName(Build.MODEL);

        this.peripheralManager = new BluetoothPeripheralManager(context, bluetoothManager, peripheralManagerCallback);
        this.peripheralManager.removeAllServices();

        DeviceInformationService dis = new DeviceInformationService(peripheralManager);
        CurrentTimeService cts = new CurrentTimeService(peripheralManager);
        HeartRateService hrs = new HeartRateService(peripheralManager);
        serviceImplementations.put(dis.getService(), dis);
        serviceImplementations.put(cts.getService(), cts);
        serviceImplementations.put(hrs.getService(), hrs);
        // new in step 04
        TemperatureService temperatureService = new TemperatureService(peripheralManager);
        serviceImplementations.put(temperatureService.getService(), temperatureService);

        setupServices();
        startAdvertising(hrs.getService().getUuid());
        // new in step 04
        startAdvertising(temperatureService.getService().getUuid());
    }

    /**
     * section for broadcast
     */

    private void sendToMain(@NotNull Intent intent) {
        mContext.sendBroadcast(intent);
    }
}
