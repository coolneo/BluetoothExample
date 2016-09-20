package com.gargi.connnectamnon18;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {


    private class LeDeviceListAdapter extends BaseAdapter {
        private ArrayList<BluetoothDevice> mLeDevices;
        private LayoutInflater mInflator;

        public LeDeviceListAdapter() {
            super();
            mLeDevices = new ArrayList<BluetoothDevice>();
            mInflator = MainActivity.this.getLayoutInflater();
        }

        public void addDevice(BluetoothDevice device) {
            if (!mLeDevices.contains(device)) {
                mLeDevices.add(device);
                sort();
            }
        }

        public void sort() {
            Collections.sort(mLeDevices, new Comparator<BluetoothDevice>() {
                @Override
                public int compare(BluetoothDevice item1, BluetoothDevice item2) {
                    if (item1 != null && item1.getName() != null
                            && item1.getName().equalsIgnoreCase("BlueNRG"))
                        return -1;
                    return item1.getName().compareTo(item2.getName());
                };
            });
        }

        public BluetoothDevice getDevice(int position) {
            return mLeDevices.get(position);
        }

        public void clear() {
            mLeDevices.clear();
        }

        @Override
        public int getCount() {
            return mLeDevices.size();
        }

        @Override
        public Object getItem(int i) {
            return mLeDevices.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            int theType = 0;
            ViewHolder viewHolder;

            if (view == null) {
                view = mInflator.inflate(R.layout.listitem_device, null);
                viewHolder = new ViewHolder();
                viewHolder.deviceAddress = (TextView) view
                        .findViewById(R.id.device_address);
                viewHolder.deviceName = (TextView) view
                        .findViewById(R.id.device_name);
                viewHolder.deviceLogo = (ImageView) view
                        .findViewById(R.id.device_logo);

                view.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) view.getTag();
            }

            BluetoothDevice device = mLeDevices.get(i);
            final String deviceName = device.getName();
            if (deviceName != null && deviceName.length() > 0) {

                viewHolder.deviceName.setText(deviceName);
            } else
                viewHolder.deviceName.setText(R.string.unknown_device);
            viewHolder.deviceAddress.setText(device.getAddress());

            if (deviceName != null && deviceName.equalsIgnoreCase("BlueNRG")) {
                viewHolder.deviceLogo.setVisibility(View.VISIBLE);
                viewHolder.deviceLogo.setImageDrawable(getResources()
                        .getDrawable(R.mipmap.ic_launcher));
            } else {
                viewHolder.deviceLogo.setVisibility(View.GONE);
            }

            return view;
        }
    }


    private class BtReadWrite extends Thread {

        InputStream mBtInputStream;
        OutputStream mBtOutputStream;

        public BtReadWrite() {

            try {
                mBtInputStream = mBluetoothSocket.getInputStream();
                mBtOutputStream = mBluetoothSocket.getOutputStream();

            }catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            // Keep looping to listen for received messages
            while (true) {
                try {
                    bytes = mBtInputStream.read(buffer);        	//read bytes from input buffer
                    String readMessage = new String(buffer, 0, bytes);
                    // Send the obtained bytes to the UI Activity via handler
                    mBtReadHandler.obtainMessage(0, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }



        public void writeData(String strData) {
            byte[] data = strData.getBytes();
            try {
                mBtOutputStream.write(data);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    private class BtConnectionThread extends Thread {

        public BtConnectionThread(BluetoothDevice btDevice) {
            BluetoothSocket btSocket = null;

            try {
                btSocket = btDevice.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
                e.printStackTrace();
            }
            mBluetoothSocket = btSocket;
        }

        @Override
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                mBluetoothSocket.connect();
                sendWelcomeMsgToAmnon();
            } catch (IOException e) {
                try {mBluetoothSocket.close();} catch (IOException e1) {}
                return;
            }
        }

        @Override
        public void interrupt() {
            try {
                mBluetoothSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            super.interrupt();
        }
    }

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothSocket mBluetoothSocket;
    private boolean mScanning;
    private Handler mHandler;
    private ListView mBleListview;

    private static final int REQUEST_ENABLE_BT = 1;
    public final static int PERMISSION_TAG = 201;
    private static final long SCAN_PERIOD = 10000;
    private static final int SHOW_DETAILS = 100;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805f9B34FB");

    private boolean mLocationPermissionGranted =  true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHandler = new Handler();

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported,
                    Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        requestLocationAccess();

        if(!mBluetoothAdapter.isEnabled()) {
            showToastMsg("Bluetooth is not enabled , please enable it and start the app again");
        }

        initUi();


    }


    private void initUi() {
        Button scanBtn = (Button)findViewById(R.id.scan_btn);
        mBleListview = (ListView)findViewById(R.id.ble_listview);
        setListAdapter();


        scanBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mLocationPermissionGranted) {
                    startBtScanningThread();
                }
                else {
                    showToastMsg("Please give location permission to move ahead");
                }
            }
        });

        mBleListview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BluetoothDevice btDevice = mLeDeviceListAdapter.getDevice(position);

                if(btDevice.getName().toLowerCase() == "amnon18" ) {
                    showToastMsg("connecting to amnon18 please wait");
                    BtConnectionThread btConnectionThread = new BtConnectionThread(btDevice);
                    btConnectionThread.start();
                    scanLeDevice(false);
                }
                else {
                    showToastMsg("This is not amonon18");
                }
            }
        });

    }

    private void sendWelcomeMsgToAmnon() {
        showToastMsg("sending welcome sequence to amnon18");
        BtReadWrite btReadWrite = new BtReadWrite();
        btReadWrite.writeData("A");
    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }

    private void showToastMsg(String msg) {
        Toast.makeText(MainActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    private void setListAdapter() {
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        mBleListview.setAdapter(mLeDeviceListAdapter);
        mLeDeviceListAdapter.notifyDataSetChanged();
    }

    private void startBtScanningThread() {

        Thread enableThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!mBluetoothAdapter.isEnabled()) {
                        mBluetoothAdapter.enable();
                        Log.d("resume", "enable...");
                        Thread.sleep(500);
                    }
                } catch (Exception e) {
                    e.getLocalizedMessage();
                }
                Log.d("resume",
                        "bluetooth enabled: " + mBluetoothAdapter.isEnabled()
                                + "; scan...");
                scanLeDevice(true);
            }
        });
        enableThread.start();
    }

    private void scanLeDevice(final boolean enable) {

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            performScan(enable);
        }
        else {
            performScanLe(enable);
        }

    }

    private void performScan(final boolean enable) {

        if(Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();

            if(scanner == null) {
                return;
            }

            if(enable) {
                scanner.startScan(mBleScancallback);
            }
            else {
                scanner.stopScan(mBleScancallback);
            }
        }
    }

    private void performScanLe(final boolean enable) {
        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                }
            }, SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi,
                             byte[] scanRecord) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(device);
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };

    @SuppressLint("NewApi")
    private ScanCallback mBleScancallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    };

    private Handler mBtReadHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            if(msg.what == 0) {
                String data = (String)msg.obj;

                if(data == "1") {
                    Toast.makeText(MainActivity.this, "Amnon18 is connected", Toast.LENGTH_LONG);
                }
            }

        }
    };


    /*
    Permission stuff
     */

    void requestLocationAccess() {
        int camera_permission =
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION);

        if(camera_permission == PackageManager.PERMISSION_GRANTED) {
            mLocationPermissionGranted = true;
            return;
        }

        mLocationPermissionGranted = false;
        String[] perms = new String[] {Manifest.permission.ACCESS_COARSE_LOCATION};
        ActivityCompat.requestPermissions(this, perms, PERMISSION_TAG);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == PERMISSION_TAG) {
            if( grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                mLocationPermissionGranted = true;
            }
            else {
                // Permission denied
                mLocationPermissionGranted = false;
            }
        }
    }


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        ImageView deviceLogo;
    }
}
