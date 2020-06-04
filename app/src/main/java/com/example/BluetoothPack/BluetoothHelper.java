package com.example.BluetoothPack;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.UUID;

/** BLUETOOTH HELPER
 * This class is responsible for all Bluetooth operations and services.
 * It encloses BluetoothConnectionService instance,
 * witch is responsible for bluetooth-connection threads. (see it for more info)
 * This class has 4 broadcast-receivers witch manage broadcast communications with bluetooth services.
 * 1 - BtConnection: (filter ACTION_STATE_CHANGED) get on/off bluetooth status.
 * 2 - BtDiscover: (filter ACTION_SCAN_MODE_CHANGED) get discoverable status of device.
 * 3 - BtFind: (filter ACTION_FOUND) get info about scanning-mode for other device
 * 4 - BtBond: (filter ACTION_BOND_STATE_CHANGED) get info about other devices pairing actions.
 *
 * These broadcast-receivers are enabled by public methods witch can be called by outside (activities).
 * 1 - btEnable: switch on/off bluetooth service
 * 2 - btDiscoverable: set device discoverable by other devices. (for a limited time)
 * 3 - btFindDevices: enable finding-mode to discover other devices (witch are discoverable).
 * The only broadcast-receiver that automatically starts (in constructor) is the 4°,
 * because bond-receiver it is responsible for pairing actions that are automatics.
 *
 * More than broadcast receivers, in this class there are:
 * - BluetoothAdapter: needed by broadcast-receiver to select actions and states (static members)
 * - BluetoothDevice: the device setted to be the communication target (server or client)
 * - ArrayList<BluetoothDevice>: the list of reachable devices (found by receiver 3)
 * - DeviceListAdapter: adapter for the device-list (with own layer)
 * - BluetoothConnectionService: that manages connection how said before.
 *
 * The connection is a unsecure-connection (see documentation online) and it's made by the default UUID code.
 * For secure connection or other UUID this class could need some changes.
 *
 * The constructor need 2 parameters:
 * - Context: for register/unregister intent-filters and startActivity for Intents.
 * - IncomingMsgHandler: that is passed from activity to BluetoothConnectionService constructor class,
 *                       and it manages incoming-messages callback, to update activity-UI.
 *                       (when is received a message from the other connected device)
 */
class BluetoothHelper {
    private final String TAG = "Bluetooth helper";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    BluetoothAdapter mBluetoothAdapter;
    BluetoothConnectionService mBluetoothConnection;
    Context context;
    BluetoothDevice otherDevice;
    public ArrayList<BluetoothDevice> foundDevices = new ArrayList<>();
    public DeviceListAdapter mDeviceListAdapter;

    public BluetoothHelper(Context context, IncomingMsgHandler handler) {
        this.context = context;
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // BluetoothConnectionService (by constructor it runs AcceptThread for listening as server)
        mBluetoothConnection = new BluetoothConnectionService(context, handler);

        // devices-list adapter (see DeviceListAdaper.java):
        mDeviceListAdapter = new DeviceListAdapter(context, R.layout.device_adapter_view, foundDevices);

        //4° BROADCAST-RECEIVER (BOND). Broadcasts when bond state changes (pairing)
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        context.registerReceiver(m4_BroadcastReceiverBond, filter);
    }

    public void btWrite(String sendMsg){
        Log.d(TAG, " sent message: " + sendMsg);
        byte[] bytes = sendMsg.getBytes(Charset.defaultCharset());
        mBluetoothConnection.write(bytes);
        Log.d(TAG, " sent message: " + sendMsg);
    }


    // ENABLE-DISABLE BLUETOOTH. (bluetooth state)
    void btEnable() {
        if (mBluetoothAdapter == null) {
            Log.d(TAG, "enableDisableBT - you don't have Bluetooth capabilities");
        } else {
            if (!mBluetoothAdapter.isEnabled()) {
                Log.d(TAG, "enableDisableBT - enabling Bluetooth");
                Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                context.startActivity(enableIntent);
            } else {
                Log.d(TAG, "enableDisableBT - disabling Bluetooth");
                mBluetoothAdapter.disable();
            }
            IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
            context.registerReceiver(m1_BroadcastReceiverBtConnection, BTintent);
        }
    }

    // ENABLE-DISABLE DISCOVERING-MODE. (bluetooth action scan)
    void btDiscoverable() {
        Log.d(TAG, "btDiscoverable - switch on/off");
        Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 30);
        context.startActivity(discoverableIntent);

        IntentFilter BTintent = new IntentFilter(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED);
        context.registerReceiver(m2_BroadcastReceiverDiscovering, BTintent);
    }

    // FIND DEVICES. (bluetooth action found). Needs BT-Adapter.startDiscovery.
    void btFindDevices() {
        Log.d(TAG, "discoveryDevices - Looking for unpaired devices.");
        if (mBluetoothAdapter.isDiscovering()) {
            Log.d(TAG, "discoveryDevices - Canceling discovery.");
            mBluetoothAdapter.cancelDiscovery();
            foundDevices.clear();
            mDeviceListAdapter.notifyDataSetChanged();
        }
        Log.d(TAG, "discoveryDevices - Restarting discovery.");
        mBluetoothAdapter.startDiscovery();
        IntentFilter discoverDeviceIntent = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        context.registerReceiver(m3_BroadcastReceiverFindDevices, discoverDeviceIntent);
    }

    //-------------- BROADCAST-RECEIVERS ------------------//

    // BROADCASTRECEIVER 1: Creates a BroadcastReceiver for ACTION_STATE_CHANGED.
    private final BroadcastReceiver m1_BroadcastReceiverBtConnection = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        Log.d(TAG, "mBluetoothAdapter - state OFF.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        Log.d(TAG, "mBluetoothAdapter - state TURNING OFF.");
                        break;
                    case BluetoothAdapter.STATE_ON:
                        Log.d(TAG, "mBluetoothAdapter - state ON.");
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        Log.d(TAG, "mBluetoothAdapter - state TURNING ON.");
                        break;
                }
            }
        }
    };

    // BROADCASTRECEIVER 2: Creates a BroadcastReceiver for ACTION_SCAN_MODE_CHANGED.
    private final BroadcastReceiver m2_BroadcastReceiverDiscovering = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothAdapter.ACTION_SCAN_MODE_CHANGED)) {
                final int mode = intent.getIntExtra(BluetoothAdapter.EXTRA_SCAN_MODE, BluetoothAdapter.ERROR);
                switch (mode) {
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE:
                        Log.d(TAG, "mBluetoothAdapter - state CONNECTABLE.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE:
                        Log.d(TAG, "mBluetoothAdapter - state DISCOVERABLE.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTING:
                        Log.d(TAG, "mBluetoothAdapter - state CONNECTING.");
                        break;
                    case BluetoothAdapter.STATE_CONNECTED:
                        Log.d(TAG, "mBluetoothAdapter - state CONNECTED.");
                        break;
                    case BluetoothAdapter.SCAN_MODE_NONE:
                        Log.d(TAG, "mBluetoothAdapter - state NONE.");
                        break;
                }
            }
        }
    };

    // BROADCASTRECEIVER 3: Creates a BroadcastReceiver for ACTION_FOUND.
    private final BroadcastReceiver m3_BroadcastReceiverFindDevices = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "BroadcastReceiver 3 - finding devices..");
            final String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                Log.d(TAG, "onReceive - ACTION FOUND.");
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                assert device != null;
                Log.d(TAG, "onReceive - device name: " + device.getName() + " / UUID:"  + device.getAddress());
                // add device to devices-list:
                if(!foundDevices.contains(device)) {
                    foundDevices.add(device);
                    mDeviceListAdapter.notifyDataSetChanged();
                }
            }
        }
    };

    // BROADCASTRECEIVER 4: Broadcast Receiver that detects bond state changes (Pairing status changes)
    private final BroadcastReceiver m4_BroadcastReceiverBond = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            assert action != null;
            if (action.equals(BluetoothDevice.ACTION_BOND_STATE_CHANGED)) {
                BluetoothDevice mDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //3 cases:
                //case1: bonded already
                assert mDevice != null;
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDED) {
                    Log.d(TAG, "BroadcastReceiver - BOND_BONDED.");
                    otherDevice = mDevice; // assigns device to connection (see startBTConnection)
                }
                //case2: creating a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_BONDING) {
                    Log.d(TAG, "BroadcastReceiver - BOND_BONDING.");
                }
                //case3: breaking a bond
                if (mDevice.getBondState() == BluetoothDevice.BOND_NONE) {
                    Log.d(TAG, "BroadcastReceiver - BOND_NONE.");
                }
            }
        }
    };


    public void startClientConnection(int deviceNumber){
        BluetoothDevice serverDevice = foundDevices.get(deviceNumber);
        //first cancel discovery because its very memory intensive.
        mBluetoothAdapter.cancelDiscovery();
        Log.d(TAG, "onItemClick - You Clicked on a device:");
        Log.d(TAG, "onItemClick - deviceName = " + serverDevice.getName());
        Log.d(TAG, "onItemClick - deviceAddress = " +  serverDevice.getAddress());
        serverDevice.createBond();
        Log.d(TAG, "startConnection - Initializing RFCOM Bluetooth connection with the server. (UUID:" +  MY_UUID_INSECURE + ")");
        mBluetoothConnection.startClient(serverDevice);
    }

    public void unregister(){
        context.unregisterReceiver(m1_BroadcastReceiverBtConnection);
        context.unregisterReceiver(m2_BroadcastReceiverDiscovering);
        context.unregisterReceiver(m3_BroadcastReceiverFindDevices);
        context.unregisterReceiver(m4_BroadcastReceiverBond);
    }

    public void closeClient() {
        mBluetoothConnection.closeClient();
    }
}
