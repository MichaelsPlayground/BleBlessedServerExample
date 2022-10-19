package de.androidcrypto.bleblessedserverexample;

import static android.bluetooth.BluetoothGattCharacteristic.PERMISSION_READ;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_NOTIFY;
import static android.bluetooth.BluetoothGattCharacteristic.PROPERTY_READ;
import static android.bluetooth.BluetoothGattService.SERVICE_TYPE_PRIMARY;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.Handler;
import android.os.Looper;

import com.welie.blessed.BluetoothCentral;
import com.welie.blessed.BluetoothPeripheralManager;
import com.welie.blessed.GattStatus;
import com.welie.blessed.ReadResponse;

import org.jetbrains.annotations.NotNull;

import java.util.UUID;

import timber.log.Timber;

class HeartRateService extends BaseService {

    public static final UUID HEART_BEAT_RATE_SERVICE_UUID = UUID.fromString("0000180D-0000-1000-8000-00805f9b34fb");
    public static final UUID HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID = UUID.fromString("00002A37-0000-1000-8000-00805f9b34fb");

    private @NotNull final BluetoothGattService service = new BluetoothGattService(HEART_BEAT_RATE_SERVICE_UUID, SERVICE_TYPE_PRIMARY);
    private @NotNull final BluetoothGattCharacteristic measurement = new BluetoothGattCharacteristic(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID, PROPERTY_READ | PROPERTY_NOTIFY, PERMISSION_READ);
    private @NotNull final Handler handler = new Handler(Looper.getMainLooper());
    private @NotNull final Runnable notifyRunnable = this::notifyHeartRate;
    private int currentHR = 80;

    public HeartRateService(@NotNull BluetoothPeripheralManager peripheralManager) {
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
        if (characteristic.getUuid().equals(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            return new ReadResponse(GattStatus.SUCCESS, new byte[]{0x00, 0x40});
        }
        return super.onCharacteristicRead(central, characteristic);
    }

    @Override
    public void onNotifyingEnabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            notifyHeartRate();
        }
    }

    @Override
    public void onNotifyingDisabled(@NotNull BluetoothCentral central, @NotNull BluetoothGattCharacteristic characteristic) {
        if (characteristic.getUuid().equals(HEART_BEAT_RATE_MEASUREMENT_CHARACTERISTIC_UUID)) {
            stopNotifying();
        }
    }

    private void notifyHeartRate() {
        currentHR += (int) ((Math.random() * 10) - 5);
        if (currentHR > 120) currentHR = 100;
        final byte[] value = new byte[]{0x00, (byte) currentHR};
        notifyCharacteristicChanged(value, measurement);
        handler.postDelayed(notifyRunnable, 1000); // every second a new value

        Timber.i("new hr: %d", currentHR);
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
        return "HeartRate Service";
    }
}
