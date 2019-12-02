package com.example.bluetoothledemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.List;

public class DeviceListActivity extends AppCompatActivity {
    private static final String TAG = "DeviceListActivity";

    private BluetoothAdapter mBtAdapter;

    private Handler mHandler;
    // Stops scanning after 5 seconds.
    private static final long SCAN_PERIOD = 5000;

    private TextView mCurrentlyConnectedTextView;
    private LeDeviceListAdapter mLeDeviceListAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_device_list);
        mHandler = new Handler();

        // Initializes a Bluetooth adapter.  For API level 18 and above, get a reference to
        // BluetoothAdapter through BluetoothManager.
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBtAdapter = bluetoothManager.getAdapter();

        mCurrentlyConnectedTextView = findViewById(R.id.currently_connected);

        // set the text for current status message
        if (mBtAdapter == null) {
            mCurrentlyConnectedTextView.setText(R.string.disabled);
            return;
        }
        if (!mBtAdapter.isEnabled()) {
            mCurrentlyConnectedTextView.setText(R.string.bluetooth_off);
            return;
        } else {
            mCurrentlyConnectedTextView.setText(R.string.no_device);
        }

        // Initialize array adapters.
        // Find and set up the ListView for available devices
        ListView availableDevicesListView = findViewById(R.id.available_devices);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this, new ArrayList<BluetoothDevice>());
        availableDevicesListView.setAdapter(mLeDeviceListAdapter);
        availableDevicesListView.setOnItemClickListener(mDeviceClickListener);

        // Register for broadcasts
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothLeConnectionService.ACTION_GATT_CONNECTED);
        filter.addAction(BluetoothLeConnectionService.ACTION_GATT_DISCONNECTED);
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onStart() {
        super.onStart();

        scanLeDevice(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Make sure we're not doing discovery anymore
        if (mBtAdapter != null) {
            mBtAdapter.stopLeScan(mLeScanCallback);
        }

        // Unregister broadcast listeners
        this.unregisterReceiver(mReceiver);
    }

    private void scanLeDevice(final boolean enable) {
        // TODO: check if bluetooth is enabled

        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    Log.d(TAG, "Scan stopping 1");
                    mBtAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mBtAdapter.startLeScan(mLeScanCallback);
        } else {
            Log.d(TAG, "Scan stopping 2");
            mBtAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    // Device scan callback. API < 21
    private BluetoothAdapter.LeScanCallback mLeScanCallback =
            new BluetoothAdapter.LeScanCallback() {
                @Override
                public void onLeScan(final BluetoothDevice device, int rssi, byte[] scanRecord) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(device != null) {
                                //String deviceName = device.getName();
                                //String deviceAddress = device.getAddress(); // MAC address
                                //Log.d(TAG, "device found: " + deviceName + ", " + deviceAddress);
                                mLeDeviceListAdapter.addDevice(device);
                                mLeDeviceListAdapter.notifyDataSetChanged();
                            }
                        }
                    });
                }
            };

    /**
     * The on-click listener for all devices in the ListViews
     */
    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> arg0, View view, int pos, long id) {
            // Cancel discovery because it's costly and we're about to connect
            scanLeDevice(false);

            mCurrentlyConnectedTextView.setText("connecting...");

            // Get the BluetoothDevice object
            BluetoothDevice btdevice = mLeDeviceListAdapter.getDevice(pos);

            // Request the service to start the connection
            Intent intent = new Intent();
            intent.setAction(BluetoothLeConnectionService.GATT_START_CONNECTION);
            intent.setClass(getApplicationContext(), BluetoothLeConnectionService.class);
            intent.putExtra( "address", btdevice.getAddress() );
            startService(intent);
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive intent: " + action);

        if(BluetoothLeConnectionService.ACTION_GATT_CONNECTED.equals(action)) {
            String address = intent.getStringExtra("address");
            if(address != null) {
                String name = mBtAdapter.getRemoteDevice(address).getName();
                mCurrentlyConnectedTextView.setText(name+'\n'+address);
            }

        } else if(BluetoothLeConnectionService.ACTION_GATT_DISCONNECTED.equals(action)) {
            mCurrentlyConnectedTextView.setText("disconnected");
        }

        }
    };

    private class LeDeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
        private Context mContext;
        private List<BluetoothDevice> devicesList;

        public LeDeviceListAdapter(Context context, ArrayList<BluetoothDevice> devices) {
            super(context, 0, devices);
            mContext = context;
            devicesList = devices;
        }

        public void addDevice(BluetoothDevice device) {
            if(!devicesList.contains(device)) {
                devicesList.add(device);
            }
        }

        public BluetoothDevice getDevice(int pos) {
            return devicesList.get(pos);
        }

        public void clear() {
            devicesList.clear();
        }

        @Override
        public int getCount() {
            return devicesList.size();
        }

        @Override
        public BluetoothDevice getItem(int pos) {
            return devicesList.get(pos);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if(convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.device_name,parent,false);
            }

            BluetoothDevice currentDevice = devicesList.get(position);
            TextView info = convertView.findViewById(R.id.device_info);

            String text = currentDevice.getName() + "\n" + currentDevice.getAddress();
            info.setText(text);

            return convertView;
        }

    }





}
