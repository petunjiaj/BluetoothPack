package com.example.BluetoothPack;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

/** DeviceListAdapter
 * This class is the Device-list Adapter and is responsible to order device-list UI template.
 *
 * It has own layout (xml layout-file). It's used by BluetoothHelper
 */
public class DeviceListAdapter extends ArrayAdapter<BluetoothDevice> {
    private final static String TAG = "DeviceListAdapter";
    private LayoutInflater mLayoutInflater;
    private ArrayList<BluetoothDevice> mDevices;
    private int  mViewResourceId;

    DeviceListAdapter(@NonNull Context context, int resource, ArrayList<BluetoothDevice> devices) {
        super(context, resource, devices);
        this.mDevices = devices;
        mLayoutInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mViewResourceId = resource;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        convertView = mLayoutInflater.inflate(mViewResourceId, null);
        BluetoothDevice device = mDevices.get(position);
        // Check if an existing view is being reused, otherwise inflate the view
        if (convertView == null) {
            Log.d(TAG, "getView: contentView null.");
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.device_adapter_view, parent, false);
        }
        if (device != null) {
            Log.d(TAG, "getView: device not null.");
            TextView deviceName = convertView.findViewById(R.id.tvDeviceName);
            TextView deviceAdress = convertView.findViewById(R.id.tvDeviceAddress);
            if (deviceName != null) {
                deviceName.setText(device.getName());
            }
            if (deviceAdress != null) {
                deviceAdress.setText(device.getAddress());
            }
        }
        // Return the completed view to render on screen
        return convertView;
    }
}
