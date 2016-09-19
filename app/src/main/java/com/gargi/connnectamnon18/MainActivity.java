package com.gargi.connnectamnon18;

import android.app.ListActivity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.UUID;

public class MainActivity extends ListActivity {


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
                mBtOutputStream = mBluetoothSocket.getOutputStream()

            }catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        public void run() {
        }

        private void readData() {
            
        }

        public void writeData(byte[] data) {
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

    private static final int REQUEST_ENABLE_BT = 1;

    private static final long SCAN_PERIOD = 10000;
    private static final int SHOW_DETAILS = 100;
    private static final UUID MY_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getActionBar().setTitle(R.string.title_devices);

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

        setListAdapter();

    }

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
    }


    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null)
            return;

        if (device != null && device.getName() != null
                && device.getName().contains("amnon18")) {

            String amnon18Msg = "Amnon18 Connected with device id = "+device.getAddress();
            Toast.makeText(MainActivity.this, amnon18Msg, Toast.LENGTH_LONG);

            if (mScanning) {
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanning = false;
            }

            BtConnectionThread connectionThread = new BtConnectionThread(device);
            connectionThread.start();

        } else
            return;
    }



    private void setListAdapter() {
        mLeDeviceListAdapter = new LeDeviceListAdapter();
        setListAdapter(mLeDeviceListAdapter);
        mLeDeviceListAdapter.notifyDataSetChanged();
        startBtScanningThread();

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

        if (enable) {
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
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


    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        ImageView deviceLogo;
    }
}
