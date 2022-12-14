package de.androidcrypto.bleblessedserverexample;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.switchmaterial.SwitchMaterial;

import java.util.ArrayList;
import java.util.List;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

    /* Local UI */
    SwitchMaterial bluetoothEnabled, advertisingActive, deviceConnected;
    com.google.android.material.textfield.TextInputEditText connectionLog, currentTime, heartBeatRate, modelName;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int ACCESS_LOCATION_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bluetoothEnabled = findViewById(R.id.swMainBleEnabled);
        advertisingActive = findViewById(R.id.swMainAdvertisingActive);
        deviceConnected = findViewById(R.id.swMainDeviceConnected);
        connectionLog = findViewById(R.id.etMainConnectionLog);
        currentTime = findViewById(R.id.etMainCurrentTime);
        heartBeatRate = findViewById(R.id.etMainHeartBeatRate);
        modelName = findViewById(R.id.etMainModelName);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        registerReceiver(advertiserStateReceiver, new IntentFilter((BluetoothServer.BLUETOOTH_HANDLER_ADVERTISER)));
        registerReceiver(connectionStateReceiver, new IntentFilter((BluetoothServer.BLUETOOTH_HANDLER_CONNECTION)));
        registerReceiver(currentTimeStateReceiver, new IntentFilter((BluetoothServer.BLUETOOTH_HANDLER_CURRENT_TIME)));
        registerReceiver(heartBeatRateStateReceiver, new IntentFilter((BluetoothServer.BLUETOOTH_HANDLER_HEART_BEAT_RATE)));
        registerReceiver(modelNameStateReceiver, new IntentFilter((BluetoothServer.BLUETOOTH_HANDLER_MODEL_NAME)));
    }

    @SuppressLint("MissingPermission")
    @Override
    protected void onResume() {
        super.onResume();

        if (!isBluetoothEnabled()) {
            bluetoothEnabled.setChecked(false);
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            bluetoothEnabled.setChecked(true);
            checkPermissions();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(advertiserStateReceiver);
        unregisterReceiver(connectionStateReceiver);
        unregisterReceiver(currentTimeStateReceiver);
        unregisterReceiver(heartBeatRateStateReceiver);
        unregisterReceiver(modelNameStateReceiver);
    }

    private boolean isBluetoothEnabled() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bluetoothAdapter == null) return false;

        return bluetoothAdapter.isEnabled();
    }

    private void checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            String[] missingPermissions = getMissingPermissions(getRequiredPermissions());
            if (missingPermissions.length > 0) {
                requestPermissions(missingPermissions, ACCESS_LOCATION_REQUEST);
            } else {
                permissionsGranted();
            }
        }
    }

    private String[] getMissingPermissions(String[] requiredPermissions) {
        List<String> missingPermissions = new ArrayList<>();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (String requiredPermission : requiredPermissions) {
                if (getApplicationContext().checkSelfPermission(requiredPermission) != PackageManager.PERMISSION_GRANTED) {
                    missingPermissions.add(requiredPermission);
                }
            }
        }
        return missingPermissions.toArray(new String[0]);
    }

    private String[] getRequiredPermissions() {
        int targetSdkVersion = getApplicationInfo().targetSdkVersion;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && targetSdkVersion >= Build.VERSION_CODES.S) {
            return new String[]{Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_ADVERTISE};
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && targetSdkVersion >= Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.ACCESS_FINE_LOCATION};
        } else return new String[]{Manifest.permission.ACCESS_COARSE_LOCATION};
    }

    private void permissionsGranted() {
        // Check if Location services are on because they are required to make scanning work
        if (checkLocationServices()) {
            initBluetoothHandler();
        }
    }

    private boolean areLocationServicesEnabled() {
        LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Timber.e("could not get location manager");
            return false;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return locationManager.isLocationEnabled();
        } else {
            boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            return isGpsEnabled || isNetworkEnabled;
        }
    }

    private boolean checkLocationServices() {
        if (!areLocationServicesEnabled()) {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Location services are not enabled")
                    .setMessage("Scanning for Bluetooth peripherals requires locations services to be enabled.") // Want to enable?
                    .setPositiveButton("Enable", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                        }
                    })
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // if this button is clicked, just close
                            // the dialog box and do nothing
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // Check if all permission were granted
        boolean allGranted = true;
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }

        if (allGranted) {
            permissionsGranted();
        } else {
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle("Location permission is required for scanning Bluetooth peripherals")
                    .setMessage("Please grant permissions")
                    .setPositiveButton("Retry", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.cancel();
                            checkPermissions();
                        }
                    })
                    .create()
                    .show();
        }
    }

    private void initBluetoothHandler()
    {
        BluetoothServer.getInstance(getApplicationContext());
    }

    /**
     * section for broadcast
     */

    private final BroadcastReceiver advertiserStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String advertiserStatus = intent.getStringExtra(BluetoothServer.BLUETOOTH_HANDLER_ADVERTISER_EXTRA);
            if (advertiserStatus == null) return;
            if (advertiserStatus.equals("ON")) {
                advertisingActive.setChecked(true);
            } else {
                advertisingActive.setChecked(false);
            }
        }
    };

    private final BroadcastReceiver connectionStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String connectionStatus = intent.getStringExtra(BluetoothServer.BLUETOOTH_HANDLER_CONNECTION_EXTRA);
            if (connectionStatus == null) return;
            if (connectionStatus.contains("connected")) {
                deviceConnected.setChecked(true);
            } else {
                deviceConnected.setChecked(false);
            }
            String newConnectionLog = connectionStatus + "\n" + connectionLog.getText().toString();
            connectionLog.setText(newConnectionLog);
        }
    };

    private final BroadcastReceiver currentTimeStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String currentTimeStatus = intent.getStringExtra(BluetoothServer.BLUETOOTH_HANDLER_CURRENT_TIME_EXTRA);
            if (currentTimeStatus == null) return;
            currentTime.setText(currentTimeStatus);
        }
    };

    private final BroadcastReceiver heartBeatRateStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String heartBeatRateStatus = intent.getStringExtra(BluetoothServer.BLUETOOTH_HANDLER_HEART_BEAT_RATE_EXTRA);
            if (heartBeatRateStatus == null) return;
            heartBeatRate.setText(heartBeatRateStatus);
        }
    };

    private final BroadcastReceiver modelNameStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String dataStatus = intent.getStringExtra(BluetoothServer.BLUETOOTH_HANDLER_MODEL_NAME_EXTRA);
            if (dataStatus == null) return;
            modelName.setText(dataStatus);
        }
    };
}