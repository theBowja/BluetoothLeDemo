package com.example.bluetoothledemo;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    private static final int REQUEST_ENABLE_BT = 22;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Use this check to determine whether BLE is supported on the device.  Then you can
        // selectively disable BLE-related features.
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            // disable BLE-related features
            setContentView(R.layout.activity_disabled);
            return;
        } else {
            setContentView(R.layout.activity_main);
        }

        // Request permissions
        int PERMISSION_ALL = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION},
                PERMISSION_ALL);

        // Request enable bluetooth
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeConnectionService.ACTION_DATA_AVAILABLE);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                // Request granted - bluetooth is turning on...
                Toast.makeText(getApplicationContext(),"Bluetooth is enabled",Toast.LENGTH_SHORT).show();
            }
            if (resultCode == RESULT_CANCELED) {
                // Request denied by user, or an error was encountered while
                // attempting to enable bluetooth
                Toast.makeText(getApplicationContext(),"Bluetooth has not been enabled",Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void onClick(View view) {
        int id = view.getId();

        if(id == R.id.bluetoothConn) {
            Intent intent = new Intent(this, DeviceListActivity.class);
            startActivity(intent);

        } else if(id == R.id.enableNotification) {
            Log.d(TAG, "enabling notification");
            Intent intent = new Intent(this, BluetoothLeConnectionService.class);
            intent.setAction(BluetoothLeConnectionService.GATT_SET_NOTIFICATION);
            intent.putExtra("enabled", true);
            startService(intent);

        } else if(id == R.id.sendPing) {
            Log.d(TAG, "sending ping");
            Intent intent = new Intent(this, BluetoothLeConnectionService.class);
            intent.setAction(BluetoothLeConnectionService.GATT_WRITE_MESSAGE);
            intent.putExtra("message", "ping");
            startService(intent);

        } else if(id == R.id.stopService) {
            Log.d(TAG, "stopping service");
            Intent intent = new Intent(this, BluetoothLeConnectionService.class);
            stopService(intent);
        }

    }


    // Create a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            //Log.d(TAG, "onReceive intent: " + action);

            if(BluetoothLeConnectionService.ACTION_DATA_AVAILABLE.equals(action)) {
                String msg = intent.getStringExtra(BluetoothLeConnectionService.EXTRA_DATA);

                Toast.makeText(getApplicationContext(),"message received: " + msg, Toast.LENGTH_SHORT).show();
            }

        }
    };


}