package com.example.BluetoothPack;

import android.Manifest;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

/** EXAMPLE ACTIVITY.
 *    This activity shows how to use BluetoothPack.
 *    It uses only:
 *    - BluetoothHelper: to manage bluetooth services and connections.
 *    - IncomingMsgHandler: to get incoming messages by listener.
 *    The rest of activity has only UI widgets_
 *    - Button (4): on/off, discover, find, close.
 *    - ListView: show device-list found.
 *    - EditText: send messages to other paired device.
 *    - TextView: get incoming messages to other device.
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener{
    private static final String TAG = "MainActivity";
    ListView listfoundDevices;
    EditText sendText;
    TextView receiveText;
    // bluetooh service class
    BluetoothHelper btHelper;
    IncomingMsgHandler handler;


    // note: to make works handle onCreate is setted as final.
    @Override
    protected final void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // need to ask explicit CoarseLocation-permission (first use):
        int MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION = 1;
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                MY_PERMISSIONS_REQUEST_ACCESS_COARSE_LOCATION);

        // layout widgets:
        setContentView(R.layout.activity_main);
        Button btnOnOff = findViewById(R.id.btn_on_off_bt);             // enable/disable bluetooth
        Button btnscanMode = findViewById(R.id.btn_discoverable_bt);    // make device discoverable
        Button btnFindDevices = findViewById(R.id.btn_find_bt);         // find other discoverable devices
        Button btnSend = findViewById(R.id.btn_send);                   // send a message
        Button btnClose = findViewById(R.id.btn_close_connection_bt);   // close client-connection
        sendText = findViewById(R.id.et_send_text);                     // edit outgoing message
        receiveText = findViewById(R.id.tw_received_msg);               // show incoming messages
        listfoundDevices = findViewById(R.id.lv_found_devices);         // list found devices
        // click listeners
        btnOnOff.setOnClickListener(this);
        btnscanMode.setOnClickListener(this);
        btnFindDevices.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnClose.setOnClickListener(this);
        listfoundDevices.setOnItemClickListener(this);

        // new handler for incoming messages (with listener-interface):
        handler = new IncomingMsgHandler(new IncomingMsgHandler.OnMessageReceivedListener() {
            @Override
            public void handleMessage(final String message) {
                Log.d(TAG, "handled message: " + message);
                receiveText.setText(message);
            }
        });
        btHelper = new BluetoothHelper(this, handler);      // set new BluetoothHelper
        listfoundDevices.setAdapter(btHelper.mDeviceListAdapter);   // set adapter for list-view:
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.btn_on_off_bt:
                btHelper.btEnable();
                break;
            case R.id.btn_discoverable_bt:
                btHelper.btDiscoverable();
                break;
            case R.id.btn_find_bt:
                btHelper.btFindDevices();
                break;
            case R.id.btn_close_connection_bt:
                btHelper.closeClient();
            case R.id.btn_send:
                write();
                break;
        }
    }

    // send a message:
    private void write(){
        String outputMsg = sendText.getText().toString();
        btHelper.btWrite(outputMsg);
        sendText.setText("");
    }

    // click on listViewDevices item:
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        btHelper.startClientConnection(position); // on item click select device and start client-connection.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        btHelper.unregister();
        handler.clear();
    }
}
