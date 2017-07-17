package com.app.libra.emotionviewer;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, Handler.Callback {

    private static final String TAG = "MainActivity";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;

    ImageView emotionView;
    Button happyButton, angryButton, sadButton, relaxButton;
    Button scanButton;
    private int flag;

    private String connectedDeviceName = null;
    private BluetoothAdapter bluetoothAdapter = null;
    private BluetoothService bluetoothService = null;
    private Handler handler;
    // Serial Communication
    private static final byte START_POINT = 'S';
    private static final byte END_POINT = 'E';
    private boolean rxFlag = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        emotionView = (ImageView) findViewById(R.id.emotionView);
        happyButton = (Button) findViewById(R.id.happyButton);
        happyButton.setOnClickListener(this);
        angryButton = (Button) findViewById(R.id.angryButton);
        angryButton.setOnClickListener(this);
        sadButton = (Button) findViewById(R.id.sadButton);
        sadButton.setOnClickListener(this);
        relaxButton = (Button) findViewById(R.id.relaxButton);
        relaxButton.setOnClickListener(this);
        flag = 0;

        scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(this);

        // Get local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            Log.v(TAG,  "Bluetooth is not available");
            finish();
        }
    }

    @Override
    protected void onStart() {
        Log.d(TAG, "onStart");
        super.onStart();

        if (!bluetoothAdapter.isEnabled()) {
            Log.v(TAG, "Turn on Bluetooth if enable");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else if (bluetoothService == null) {
            handler = new Handler(this);
            bluetoothService = new BluetoothService(this, handler);
        }
    }

    @Override
    protected void onResume() {
        Log.d(TAG, "onResume");
        super.onResume();

        if (bluetoothService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (bluetoothService.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                bluetoothService.start();
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.v(TAG, "onDestroy is called");
        super.onDestroy();

        if (bluetoothService != null) {
            bluetoothService.stop();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                Log.v(TAG, "REQUEST_CONNECT_DEVICE_SECURE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    bluetoothService.connect(device, true);

                }
                break;
            case REQUEST_CONNECT_DEVICE_INSECURE:
                Log.d(TAG, "REQUEST_CONNECT_DEVICE_INSECURE");
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    // Get the device MAC address
                    String address = data.getExtras()
                            .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                    // Get the BluetoothDevice object
                    BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
                    // Attempt to connect to the device
                    bluetoothService.connect(device, false);
                }
                break;
            case REQUEST_ENABLE_BT:
                Log.d(TAG, "REQUEST_ENABLE_BT");
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    handler = new Handler(this);
                    bluetoothService = new BluetoothService(this, handler);
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.happyButton:
                emotionView.setImageResource(R.drawable.figure_happy);
                break;
            case R.id.angryButton:
                emotionView.setImageResource(R.drawable.figure_angry);
                break;
            case R.id.sadButton:
                emotionView.setImageResource(R.drawable.figure_depressed);
                break;
            case R.id.relaxButton:
                emotionView.setImageResource(R.drawable.figure_sleeping);
                break;
            case R.id.scanButton:
                Log.d(TAG, "Scan is called");
                Intent intent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(intent, REQUEST_CONNECT_DEVICE_SECURE);
                break;
        }
    }

    @Override
    public boolean handleMessage(Message msg) {
        switch (msg.what) {
            case Constants.MESSAGE_STATE_CHANGE:
                Log.d(TAG, "Change State");
                switch (msg.arg1) {
                    case BluetoothService.STATE_CONNECTED:
                        Log.d(TAG, "Connected");
                        Toast.makeText(this, R.string.title_connected, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.STATE_CONNECTING:
                        Log.d(TAG, "Connecting");
                        Toast.makeText(this, R.string.title_connecting, Toast.LENGTH_SHORT).show();
                        break;
                    case BluetoothService.STATE_LISTEN:
                    case BluetoothService.STATE_NONE:
                        Log.d(TAG, "Fail");
                        Toast.makeText(this, R.string.title_not_connected, Toast.LENGTH_SHORT).show();
                        break;
                }
                break;
            case Constants.MESSAGE_READ:
                Log.d(TAG, "Read");
                process((byte[]) msg.obj, msg.arg1);
                break;
            case Constants.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                connectedDeviceName = msg.getData().getString(Constants.DEVICE_NAME);
                Toast.makeText(this, "Connected to "
                        + connectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
        }
        return false;
    }

    private void process(byte[] rxPacket, int bytes) {
        for(int i = 0; i < bytes; i++) {
            switch(rxPacket[i]) {
                case START_POINT:
                    rxFlag = true;
                    break;
                case END_POINT:
                    rxFlag = false;
                    break;
                default:
                    if(rxFlag) {
                        switch(rxPacket[i]) {
                            case 'H':
                                emotionView.setImageResource(R.drawable.figure_happy);
                                break;
                            case 'A':
                                emotionView.setImageResource(R.drawable.figure_angry);
                                break;
                            case 'D':
                                emotionView.setImageResource(R.drawable.figure_depressed);
                                break;
                            case 'R':
                                emotionView.setImageResource(R.drawable.figure_sleeping);
                                break;
                            case 'N':
                                emotionView.setImageResource(R.drawable.figure_standing);
                                break;
                        }
                    }
                    break;
            }
        }
    }
}
