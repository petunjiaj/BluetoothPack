package com.example.BluetoothPack;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;

/** BluetoothConnectionService Class
 * This class manages bluetooth connections by 3 threads.
 * They are 3 inner classes (witch extend thread.class):
 * - AcceptThread: runs while listening for incoming connections (server)
 * - ConnectThread: starts and attempts to make a connection (client)
 * - ConnectedThread: during connection send and receive data.
 *
 * This class let a double-side connection service (client & server) and can be used by both devices.
 * AccetpThread runs on both device by constructor and starts server-service in listening mode.
 * ConnectThread is launched by startConnection and opens client-connection.
 * When AccetpThread receive a not null socket by listening channel, it accepts connection.
 * (The system will assign an unused RFCOMM channel to listen on.
 * The system will also register a Service Discovery Protocol (SDP) record
 * with the local SDP server containing the specified UUID, service name, and auto-assigned channel.
 * Remote Bluetooth devices can use the same UUID to query our SDP server and discover which channel to connect to.)
 * Finally, on both devices (client & server) is called connected(), that starts ConnectedThread,
 * that permits data-exchange in input/output stream.
 *
 * More than the three threads listed above, this class contains:
 * - constructor: get
 *        - context, create a new BluetoothAdapter (DefaultAdapter) and start AcceptThread.
 *        - IncomingMsgHandler: witch manages incoming-messages callback, to update activity-UI.
 *
 * - (synchronized) start: (invoked by constructor) start chat service (server-connection)
 * - startClient: get server device with UUID and start ConnectThread with it (client-connection)
 * - connected: this method is called by AccetpThread and ConnectThread, and start a new ConnectedThread to perform transmissions.
 * - write: this is a public method called by outside to invoke ConnectedThread.write(bytes) and send data.
 *
 */

class BluetoothConnectionService {
    private  static final String TAG = "BTConnectionServ";
    // AppName and UUID for Bluetooth connection (bluetoothAdapter):
    private static final String APPNAME = "BluetoothTinyChat";
    private static final UUID MY_UUID_INSECURE = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    // local variables:
    private final BluetoothAdapter mBluetoothAdapter;
    private Context mContext;
    private ProgressDialog mProgressDialog;
    // threads:
    private AcceptThread mInsecureAcceptThread; // listens for incoming connections (server-socket)
    private ConnectThread mConnectThread;       //
    private ConnectedThread mConnectedThread;
    // other device parameters:
    private BluetoothDevice mmDevice;
    private IncomingMsgHandler incomingMsgHandler;

    // constructor:
    BluetoothConnectionService(Context mContext, IncomingMsgHandler handler) {
        this.mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        this.mContext = mContext;
        incomingMsgHandler = handler;
        // start AcceptThread mode (listening as Server for incoming connections):
        start();
    }
    // Start the chat service (AcceptThread) to begin a session in server-mode (listening):
    private synchronized void start(){
        Log.d(TAG, "start.");
        if(mConnectThread!=null){       // if there is already a connectThread
            mConnectThread.cancel();    // cancel it
            mConnectThread = null;      // and set connectThread as null
        }
        if(mInsecureAcceptThread == null){              // if there is no old acceptThread
            mInsecureAcceptThread = new AcceptThread(); // create new acceptThread
            mInsecureAcceptThread.start();              // start acceptThread.
        }
    }
    // Start a connection (connectThread) with the other devices AcceptThread (client-mode):
    void startClient(BluetoothDevice device){
        Log.d(TAG, "startClient - started.");
        // init progress-dialog:
        mProgressDialog = ProgressDialog.show(mContext,"connecting", "please wait..", true);
        mConnectThread = new ConnectThread(device); // create new ConnectThread
        mConnectThread.start();                           // start ConnectThread.
    }

    /** This thread runs while listening for incoming connections. It behaves like a server-side client.
     * It runs until a connection is accepted (or until cancelled). */
    private class AcceptThread extends Thread{
        private final BluetoothServerSocket mmServerSocket; // The local server socket
        // Constructor creates a new bluetooth listening server socket (BluetoothServerSocket):
        AcceptThread(){
            BluetoothServerSocket tmp = null;
            try {
                tmp = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(APPNAME, MY_UUID_INSECURE);
                Log.d(TAG, "AcceptThread - setting up Server using :" + MY_UUID_INSECURE);
            }catch (IOException e){
                e.printStackTrace();
            }
            mmServerSocket = tmp;
        }
        // run creates a socket (BluetoothSocket) from BluetoothServerSocket.
        public void run(){
            Log.d(TAG, "run: acceptThread is running." );
            BluetoothSocket socket = null;
            // This is a blocking call and will only return on a successful connection or an exception
            try {
                Log.d(TAG, "run: RFCOM server socket start..");
                // accepts socket from local server socket:
                socket = mmServerSocket.accept();
                Log.d(TAG, "run: RFCON server socket accepted connection.");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "run: RFCON server socket failed connection: " + e.getMessage());
            }
            // if socket is not null, connect server:
            if(socket != null){
                Log.d(TAG, "run: RFCON server socket not null. launch connectedThread.");
                connected(socket);
            }
            else{
                Log.d(TAG, "run: RFCON server socket  null!!");
            }
            Log.d(TAG, "end AcceptThread.");
        }
        // close serverSocket. This method is not used because Server lets both-side connection.
//        public void cancel() {
//            Log.d(TAG, "cancel: cancelling AcceptThread");
//            try {
//                mmServerSocket.close();
//            } catch (IOException e) {
//                Log.e(TAG, "cancel: close of AcceptThread ServerSocket failed: " + e.getMessage());
//            }
//        }
    }

     /** ConnectThread  starts and attempts to make a connection with other devices AcceptThread. */
    private class ConnectThread extends Thread{
        private BluetoothSocket mmSocket;
        ConnectThread(BluetoothDevice device) {
            Log.d(TAG, "ConnectThread started.");
            mmDevice = device;  // get other device and uuid
        }
        // run creates a BluetoothSocket with the other device (server)
        public void run(){
            BluetoothSocket tmp = null;
            Log.d(TAG, "run: mConnectThread.");
            try {
                Log.d(TAG, "ConnectThread: trying to create InsecureRFcommSocket using UUID:" + MY_UUID_INSECURE);
                tmp = mmDevice.createRfcommSocketToServiceRecord(MY_UUID_INSECURE);
            } catch (IOException e) {
                Log.d(TAG, "ConnectThread: could not create InsecureRFcommSocket" + e.getMessage());
            }
            mmSocket = tmp;
            mBluetoothAdapter.cancelDiscovery(); // cancel discovery (it will slow down a connection)
            try {
                mmSocket.connect(); // // Make a connection to the BluetoothSocket
                Log.d(TAG, "run: ConnectThread: successful connected");
            } catch (IOException e) {
                try {
                    mmSocket.close(); // if can't connect, close the socket
                    Log.d(TAG,"run: socket closed.");
                } catch (IOException ex) {
                    Log.d(TAG,"ConnectThread run: unable to close connection in socket: "+ e.getMessage());
                }
                Log.d(TAG, "ConnectThread run: could not connect to UUID: " + MY_UUID_INSECURE);
            }
            //connect client:
            connected(mmSocket);
        }
        // close client-socket (connection)
        void cancel(){
            try {
                mmSocket.close();
                Log.d(TAG,"cancel: socket closed.");
            } catch (IOException ex) {
                Log.d(TAG,"cancel: unable to close connection in socket: "+ ex.getMessage());
            }
        }
    }

    /** ConnectedThread which is responsible for maintaining the BTConnection,
     * Sending the data, and receiving incoming data through input/output streams respectively.**/
    private class ConnectedThread extends Thread{
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        ConnectedThread(BluetoothSocket mmSocket) {
            this.mmSocket = mmSocket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            if(mProgressDialog != null){
                mProgressDialog.dismiss(); // dismiss progress-dialog:
            }
            try {
                tmpIn = mmSocket.getInputStream();
                tmpOut = mmSocket.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }
        public void run(){
            byte[] buffer = new byte[1024];
            int bytes;
            while (true){
                try {
                    bytes = mmInStream.read(buffer);
                    String incomingMessage = new String(buffer, 0, bytes);
                    Log.d(TAG,"inputStream: " + incomingMessage);
                    //(incomingMessage);
                    incomingMsgHandler.handleMessage(incomingMessage);

                } catch (IOException e) {
                    Log.d(TAG, "write: error reading inputStream: " + e.getMessage());
                    break;
                }
            }
        }


        void write(byte[] bytes){
            String text = new String(bytes, Charset.defaultCharset());
            Log.d(TAG, " write: writing outputStream message: " + text);
            try {
                mmOutStream.write(bytes);
            } catch (IOException e) {
                Log.d(TAG, "write: error writing outputStream: " + e.getMessage());
            }
        }

        public void cancel(){
            try {
                mmSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void connected(BluetoothSocket mmSocket) {
        Log.d(TAG, "connected: starting.");
        // start the thread to manage the connection and perform transmissions:
        mConnectedThread = new ConnectedThread(mmSocket);
        mConnectedThread.start();
    }

    // write method for BluetoothConnectionService (invokes only ConnectedThread.write()):
    void write(byte[] out){
        // perform the write unsynchronized (the write method of ConnectedThread is not reachable from outside):
        Log.d(TAG, "write: call write.");
        mConnectedThread.write(out);
    }

    public void closeClient(){
        mConnectedThread.cancel();
    }
}
