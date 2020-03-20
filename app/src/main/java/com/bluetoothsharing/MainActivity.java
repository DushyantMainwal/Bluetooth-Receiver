package com.bluetoothsharing;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    public static final String btNameConnection = "Blade_Bluetooth";
    private static final String NAME = "BT_DEMO";
    private static final UUID BT_UUID = UUID.fromString("02001101-0001-1000-8080-00805F9BA9BA");
    private final int BUFFER_SIZE = 1024;
    List<BluetoothDevice> bluetoothDevices = new ArrayList<>();
    private BluetoothAdapter bTAdatper;
    private ConnectThread connectThread;
    private ListenerThread listenerThread;
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            System.out.println("Actionsd: " + action);

            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                final int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                System.out.println("State: " + state);

                switch (state) {
                    case BluetoothAdapter.STATE_OFF:
                        // Bluetooth has been turned off;
                        break;
                    case BluetoothAdapter.STATE_TURNING_OFF:
                        // Bluetooth is turning off;
                        break;
                    case BluetoothAdapter.STATE_ON:
                        // Bluetooth has been on
                        bTAdatper = BluetoothAdapter.getDefaultAdapter();
                        initReceiver();
                        listenerThread = new ListenerThread();
                        listenerThread.start();
                        openBlueTooth();
                        break;
                    case BluetoothAdapter.STATE_TURNING_ON:
                        // Bluetooth is turning on
                        break;
                }
            }

            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    bluetoothDevices.add(device);
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)) {
                Toast.makeText(MainActivity.this, "Start search", Toast.LENGTH_SHORT).show();
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                Toast.makeText(MainActivity.this, "Search completed", Toast.LENGTH_SHORT).show();

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < bluetoothDevices.size(); i++) {
                            BluetoothDevice device = bluetoothDevices.get(i);
                            if (device.getName().equalsIgnoreCase(btNameConnection)) {
                                connectDevice(Objects.requireNonNull(device));
                            }
                        }
                    }
                });
            }
        }
    };
    private TextView tvRecv;
    private ImageView imageView;
    private int previewFormat = -1;
    private int width = -1, height = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        bTAdatper = BluetoothAdapter.getDefaultAdapter();
        initReceiver();
        listenerThread = new ListenerThread();
        listenerThread.start();
        openBlueTooth();
        tvRecv = findViewById(R.id.tv_recv);

        imageView = findViewById(R.id.image_view);

        BluetoothAdapter bluetoothAdapter = null;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        Log.d("MainActivity", "localdevicename : " + bluetoothAdapter.getName() + " localdeviceAddress : " + bluetoothAdapter.getAddress());
        bluetoothAdapter.setName(btNameConnection);
        Log.d("MainActivity", "localdevicename : " + bluetoothAdapter.getName() + " localdeviceAddress : " + bluetoothAdapter.getAddress());
    }

    private void initReceiver() {
        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(mReceiver, filter);
    }

    private void openBlueTooth() {
        if (bTAdatper == null) {
            Toast.makeText(this, "The current device does not support Bluetooth", Toast.LENGTH_SHORT).show();
        }
        if (!bTAdatper.isEnabled()) {
           /* Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivity(i);*/
            bTAdatper.enable();
        }
        if (bTAdatper.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            i.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 0);
            startActivity(i);
        }
    }

    private void connectDevice(BluetoothDevice device) {
        try {
            //Socket
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(BT_UUID);
            connectThread = new ConnectThread(socket, true);
            connectThread.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (bTAdatper != null && bTAdatper.isDiscovering()) {
            bTAdatper.cancelDiscovery();
        }
        unregisterReceiver(mReceiver);
    }

    private class ConnectThread extends Thread {

        InputStream inputStream;
        OutputStream outputStream;
        private BluetoothSocket socket;
        private boolean activeConnect;

        private ConnectThread(BluetoothSocket socket, boolean connect) {
            this.socket = socket;
            this.activeConnect = connect;
        }

        @Override
        public void run() {
            try {
                if (activeConnect) {
                    socket.connect();
                }
//                text_state.post(new Runnable() {
//                    @Override
//                    public void run() {
//                        text_state.setText(getResources().getString(R.string.connect_success));
//                    }
//                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.connect_success), Toast.LENGTH_SHORT).show();
                    }
                });

                System.out.println("ConnectThread: " + getResources().getString(R.string.connect_success));
                inputStream = socket.getInputStream();
                outputStream = socket.getOutputStream();

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytes;
                while (true) {
                    bytes = inputStream.read(buffer);
                    if (bytes > 0) {
                        final byte[] data = new byte[bytes];
                        System.arraycopy(buffer, 0, data, 0, bytes);
//                        tvRecv.post(new Runnable() {
//                            @SuppressLint("SetTextI18n")
//                            @Override
//                            public void run() {
//                                tvRecv.setText(new String(data));
//                                Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);

//                            }
//                        });

                        final Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                imageView.setImageBitmap(bitmap);
                            }
                        });
                        //Toast.makeText(MainActivity.this,  getResources().getString(R.string.get_msg)+new String(data), Toast.LENGTH_SHORT).show();
//                        System.out.println("ConnectThread: " + getResources().getString(R.string.get_msg) + new String(data));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
//                text_state.post(new Runnable() {
//                    @Override
//                    public void run() {
//                                text_state.setText(getResources().getString(R.string.connect_error));
//                    }
//                });

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(MainActivity.this, getResources().getString(R.string.connect_error), Toast.LENGTH_SHORT).show();
                    }
                });
//                Toast.makeText(MainActivity.this, getResources().getString(R.string.connect_error), Toast.LENGTH_SHORT).show();
                System.out.println("ConnectThread: " + getResources().getString(R.string.connect_error));
            }
        }
    }


    private class ListenerThread extends Thread {

        private BluetoothServerSocket serverSocket;
        private BluetoothSocket socket;

        @Override
        public void run() {
            try {
                serverSocket = bTAdatper.listenUsingRfcommWithServiceRecord(
                        NAME, BT_UUID);
                while (true) {
                    socket = serverSocket.accept();
//                    text_state.post(new Runnable() {
//                        @Override
//                        public void run() {
//                            text_state.setText(getResources().getString(R.string.connecting));
//                        }
//                    });

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(MainActivity.this, getResources().getString(R.string.connecting), Toast.LENGTH_SHORT).show();
                        }
                    });
//                    System.out.println("Connect ");
//                    Toast.makeText(MainActivity.this, getResources().getString(R.string.connecting), Toast.LENGTH_SHORT).show();
                    System.out.println("Listener: " + getResources().getString(R.string.connecting));
                    connectThread = new ConnectThread(socket, false);
                    connectThread.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
