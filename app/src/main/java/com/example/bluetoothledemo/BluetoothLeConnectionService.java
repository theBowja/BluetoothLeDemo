package com.example.bluetoothledemo;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import androidx.annotation.Nullable;

import java.util.UUID;

public class BluetoothLeConnectionService extends Service {
    private static final String TAG = "BTLeConnectionServ";
    private Looper serviceLooper;
    private ServiceHandler serviceHandler;

    private final BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    // Action strings for Intents that will tell this service what to do
    public final static String GATT_START_CONNECTION =
            "com.example.bluetoothledemo.GATT_START_CONNECTION";
    public final static String GATT_SET_NOTIFICATION =
            "com.example.bluetoothledemo.GATT_SET_NOTIFICATION";
    public final static String GATT_WRITE_MESSAGE =
            "com.example.bluetoothledemo.GATT_WRITE_MESSAGE";
    public final static String GATT_STOP_CONNECTION =
            "com.example.bluetoothledemo.GATT_STOP_CONNECTION";

    // These are Actions that this service will broadcast
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetoothledemo.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetoothledemo.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetoothledemo.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetoothledemo.ACTION_DATA_AVAILABLE";
    public final static String EXTRA_DATA =
            "com.example.bluetoothledemo.EXTRA_DATA";

    // these are the relevant UUIDs for the HM-10 module
    public final static UUID CUSTOM_SERVICE =
            UUID.fromString("0000FFE0-0000-1000-8000-00805F9B34FB");
    public final static UUID CUSTOM_CHARACTERISTIC =
            UUID.fromString("0000FFE1-0000-1000-8000-00805F9B34FB");
    public final static UUID CUSTOM_CCCD =
            UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    // empty constructor
    public BluetoothLeConnectionService() { }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // Handler that receives messages from the thread
    private final class ServiceHandler extends Handler {
        public ServiceHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            onHandleIntent((Intent)msg.obj);
            //stopSelf(msg.arg1);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        // Start up the thread running the service. Note that we create a
        // separate thread because the service normally runs in the process's
        // main thread, which we don't want to block. We also make it
        // background priority so CPU-intensive work doesn't disrupt our UI.
        HandlerThread thread = new HandlerThread("BLEIntentService");
        thread.start();

        // Get the HandlerThread's Looper and use it for our Handler
        serviceLooper = thread.getLooper();
        serviceHandler = new ServiceHandler(serviceLooper);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "Service starting...");
        Message msg = serviceHandler.obtainMessage();
        msg.arg1 = startId;
        msg.obj = intent;
        serviceHandler.sendMessage(msg);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "Service destroying...");
        mBluetoothGatt.close();
        serviceLooper.quit();
    }

    protected void onHandleIntent(@Nullable Intent intent) {
        if(intent == null) return;
        if(mBluetoothAdapter == null) return;
        String action = intent.getAction();
        if(action == null) return;

        Log.i(TAG, "onHandleIntent: action=" + action);
        switch(action) {
            case GATT_START_CONNECTION:
                String address = intent.getStringExtra("address");
                if(address != null) {
                    boolean result = // TODO: call function
                    Log.d(TAG, "connect(...) "+ (result?"succeeded":"failed") +" initiation");
                }
                break;
            case GATT_STOP_CONNECTION:
                mBluetoothGatt.close();
                break;
            case GATT_WRITE_MESSAGE:
                if(mBluetoothGatt != null && mConnectionState == STATE_CONNECTED) {
                    String message = intent.getStringExtra("message");
                    if(message == null) {
                        Log.d(TAG, "message to write was null");
                    } else {
                        boolean result = // TODO: call function
                        Log.d(TAG, "write(...) "+ (result?"succeeded":"failed") +" initiation");
                    }
                }
                break;
            case GATT_SET_NOTIFICATION:
                if(mBluetoothGatt != null && mConnectionState == STATE_CONNECTED) {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    // TODO: call function
                }
                break;
        }

    }

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.d(TAG, "Connected to GATT server.");

                mConnectionState = STATE_CONNECTED;
                Intent intent = new Intent(ACTION_GATT_CONNECTED);
                intent.putExtra("address", mBluetoothDeviceAddress);
                sendBroadcast(intent);

                // Attempts to discover services after successful connection.
                Log.d(TAG, "Attempting to start service discovery: " + mBluetoothGatt.discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.d(TAG, "Disconnected from GATT server.");

                mBluetoothDeviceAddress = null;
                mConnectionState = STATE_DISCONNECTED;
                Intent intent = new Intent(ACTION_GATT_DISCONNECTED);
                sendBroadcast(intent);
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered success.");
                for (BluetoothGattService gattService : gatt.getServices())
                    Log.v(TAG, "Service UUID Found: " + gattService.getUuid().toString());

                Intent intent = new Intent(ACTION_GATT_SERVICES_DISCOVERED);
                sendBroadcast(intent);
            } else {
                Log.w(TAG, "onServicesDiscovered failed with status: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            Log.i(TAG, "onCharacteristicRead called");

            if (status == BluetoothGatt.GATT_SUCCESS) {
                // unimplemented
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic,
                                          int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onCharacteristicWrite success!");
            } else {
                Log.i(TAG, "onCharacteristicWrite failed! status: " + status);
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            Log.i(TAG, "onCharacteristicChanged called");

            UUID uuid = characteristic.getUuid();

            // TODO: filter by the characteristic we want to listen to

            String value = // TODO: get the value from the characterstic
            Log.d(TAG, "VALUE GOT: " + value);

            final Intent intent = new Intent(ACTION_DATA_AVAILABLE);
            intent.putExtra(EXTRA_DATA, value);
            sendBroadcast(intent);
        }
    };

    /**
     * Set the notifications
     *   subscribe :3
     * prerequisite: mBluetoothGatt != null
     */
    private void setNotification(boolean enable) {
        if(mBluetoothGatt == null) return;

        // TODO: fill in missing parameters
        BluetoothGattService mSVC = mBluetoothGatt.getService( ... );
        BluetoothGattCharacteristic mCH = mSVC.getCharacteristic( ... );
        mBluetoothGatt.setCharacteristicNotification(mCH, ... ); // sets notification locally on device

        BluetoothGattDescriptor descriptor = mCH.getDescriptor( ... );
        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
        mBluetoothGatt.writeDescriptor(descriptor); // enable notification on server
    }

    /**
     * prerequisite: mBluetoothGatt != null
     * prerequisite: all services on the device to have been read
     * @return true if write is initiated successfully. The result is reported asynchronously in onCharacteristicWrite(...)
     */
    private boolean write(String message) {
        if(mBluetoothGatt == null) return false;

        // TODO: fill in missing parameters

        BluetoothGattService mSVC = mBluetoothGatt.getService( ... );
        BluetoothGattCharacteristic mCH = mSVC.getCharacteristic( ... );
        mCH.setValue( ... );
        return mBluetoothGatt.writeCharacteristic(mCH);
    }

    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address.
     * @throws NullPointerException if mBluetoothAdapter is null
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    private boolean connect(final String address) {
        // TODO: fill in missing parameters
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice( ... );

        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        // Close existing GATT since we want to connect to a new device
        if(mBluetoothGatt != null)
            mBluetoothGatt.close();

        // We want to directly connect to the device, so we are setting the autoConnect parameter to false.
        mBluetoothGatt = device.connectGatt(this, true, mGattCallback);

        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }

}
