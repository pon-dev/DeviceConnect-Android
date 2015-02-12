/*
 HeartRateConnector
 Copyright (c) 2015 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.heartrate;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;

import org.deviceconnect.android.deviceplugin.heartrate.ble.BleDeviceDetector;
import org.deviceconnect.android.deviceplugin.heartrate.ble.BleUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT16;
import static android.bluetooth.BluetoothGattCharacteristic.FORMAT_UINT8;

/**
 * @author NTT DOCOMO, INC.
 */
public class HeartRateConnector {
    private static final int CHK_FIRST_WAIT_PERIOD = 10 * 1000;
    private static final int CHK_WAIT_PERIOD = 10 * 1000;
    /**
     * application context.
     */
    private Context mContext;

    /**
     * Instance of HeartRateConnectEventListener.
     */
    private HeartRateConnectEventListener mListener;

    private Map<BluetoothGatt, DeviceState> mHRDevices = new HashMap<>();

    private List<String> mRegisterDevices = new ArrayList<>();

    /**
     * Instance of ScheduledExecutorService.
     */
    private ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();

    /**
     * ScheduledFuture of scan timer.
     */
    private ScheduledFuture<?> mScanTimerFuture;

    private BleDeviceDetector mBleDeviceDetector;

    /**
     * Constructor.
     *
     * @param context application context
     */
    public HeartRateConnector(Context context) {
        mContext = context;
    }

    /**
     * Sets a instance of BleDeviceDetector.
     * @param detector instance of BleDeviceDetector
     */
    public void setBleDeviceDetector(BleDeviceDetector detector) {
        mBleDeviceDetector = detector;
    }

    /**
     * Sets a listener.
     *
     * @param listener listener
     */
    public void setListener(HeartRateConnectEventListener listener) {
        mListener = listener;
    }

    /**
     * Connect to the bluetooth device.
     *
     * @param device bluetooth device
     */
    public void connectDevice(final BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("device is null");
        }
        device.connectGatt(mContext, false, mBluetoothGattCallback);
        if (!mRegisterDevices.contains(device.getAddress())) {
            mRegisterDevices.add(device.getAddress());
        }
    }

    /**
     * Disconnect to the bluetooth device.
     *
     * @param device bluetooth device
     */
    public void disconnectDevice(final BluetoothDevice device) {
        if (device == null) {
            throw new IllegalArgumentException("device is null");
        }
        String address = device.getAddress();
        for (BluetoothGatt gatt : mHRDevices.keySet()) {
            if (gatt.getDevice().getAddress().equalsIgnoreCase(address)) {
                gatt.disconnect();
            }
        }
        mRegisterDevices.remove(device.getAddress());
    }

    /**
     * Start Bluetooth LE connect automatically.
     */
    public void start() {
        mScanTimerFuture = mExecutor.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                synchronized (mRegisterDevices) {
                    for (String address : mRegisterDevices) {
                        if (!containGatt(address)) {
                            BluetoothDevice device = mBleDeviceDetector.getDevice(address);
                            if (device != null) {
                                connectDevice(device);
                            }
                        }
                    }
                }
            }
        }, CHK_FIRST_WAIT_PERIOD, CHK_WAIT_PERIOD, TimeUnit.MILLISECONDS);
    }

    /**
     * Stop Bluetooth LE connect automatically.
     */
    public void stop() {
        mHRDevices.clear();
        mScanTimerFuture.cancel(true);
        mRegisterDevices.clear();
    }

    private boolean containGatt(String address) {
        for (BluetoothGatt gatt : mHRDevices.keySet()) {
            if (gatt.getDevice().getAddress().equalsIgnoreCase(address)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks Ble Device has Heart Rate Service.
     *
     * @param gatt GATT Service
     * @return true ble device has Heart Rate Service
     */
    private boolean hasHeartRateService(BluetoothGatt gatt) {
        BluetoothGattService service = gatt.getService(UUID.fromString(
                BleUtils.SERVICE_HEART_RATE_SERVICE));
        return service != null;
    }

    private boolean isCharacteristic(final BluetoothGattCharacteristic characteristic,
                                     final String checkUuid) {
        String uuid = characteristic.getUuid().toString();
        return checkUuid.equalsIgnoreCase(uuid);
    }

    private boolean isBodySensorLocation(final BluetoothGattCharacteristic characteristic) {
        return isCharacteristic(characteristic, BleUtils.CHAR_BODY_SENSOR_LOCATION);
    }

    private boolean isHeartRateMeasurement(final BluetoothGattCharacteristic characteristic) {
        return isCharacteristic(characteristic, BleUtils.CHAR_HEART_RATE_MEASUREMENT);
    }

    private void newHeartRateDevice(final BluetoothGatt gatt) {
        mHRDevices.put(gatt, DeviceState.GET_LOCATION);
        if (mListener != null) {
            mListener.onConnected(gatt.getDevice());
        }
    }

    /**
     * Get a body sensor location from GATT Service.
     *
     * @param gatt GATT Service
     * @return true if gatt has Generic Access Service, false if gatt has no service.
     */
    private boolean callGetBodySensorLocation(BluetoothGatt gatt) {
        boolean result = false;
        BluetoothGattService service = gatt.getService(UUID.fromString(
                BleUtils.SERVICE_HEART_RATE_SERVICE));
        if (service != null) {
            BluetoothGattCharacteristic c = service.getCharacteristic(
                    UUID.fromString(BleUtils.CHAR_BODY_SENSOR_LOCATION));
            if (c != null) {
                result = gatt.readCharacteristic(c);
            }
        }
        return result;
    }

    /**
     * Register notification of HeartRateMeasurement Characteristic.
     *
     * @param gatt GATT Service
     * @return true if successful in notification of registration
     */
    private boolean callRegisterHeartRateMeasurement(BluetoothGatt gatt) {
        boolean registered = false;
        BluetoothGattService service = gatt.getService(UUID.fromString(
                BleUtils.SERVICE_HEART_RATE_SERVICE));
        if (service != null) {
            BluetoothGattCharacteristic c = service.getCharacteristic(
                    UUID.fromString(BleUtils.CHAR_HEART_RATE_MEASUREMENT));
            if (c != null) {
                registered = gatt.setCharacteristicNotification(c, true);
                if (registered) {
                    for (BluetoothGattDescriptor descriptor : c.getDescriptors()) {
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                        gatt.writeDescriptor(descriptor);
                    }
                    mHRDevices.put(gatt, DeviceState.REGISTER_NOTIFY);
                }
            }
        }
        return registered;
    }

    private boolean next(final BluetoothGatt gatt) {
        Log.i("ABC", "@@@@@@ next: enter");
        if (!mHRDevices.containsKey(gatt)) {
            newHeartRateDevice(gatt);
        }

        DeviceState state = mHRDevices.get(gatt);
        switch (state) {
            case GET_LOCATION:
                if (!callGetBodySensorLocation(gatt)) {
                    mHRDevices.put(gatt, DeviceState.REGISTER_NOTIFY);
                    gatt.discoverServices();
                }
                break;
            case REGISTER_NOTIFY:
                if (!callRegisterHeartRateMeasurement(gatt)) {
                    mHRDevices.put(gatt, DeviceState.ERROR);
                }
                break;
            default:
                break;
        }

        return false;
    }


    private void notifyHeartRateMeasurement(final BluetoothGatt gatt,
            final BluetoothGattCharacteristic characteristic) {
        int heartRate = 0;
        int energyExpended = 0;
        double rrInterval = 0;
        int offset = 1;

        byte[] buf = characteristic.getValue();
        if (buf.length > 1) {
            // Heart Rate Value Format bit
            if ((buf[0] & 0x80) != 0) {
                heartRate = characteristic.getIntValue(FORMAT_UINT16, offset);
                offset += 2;
            } else {
                heartRate = characteristic.getIntValue(FORMAT_UINT8, offset);
                offset += 1;
            }

            // Sensor Contact Status bits
            if ((buf[0] & 0x60) != 0) {
            }

            // Energy Expended Status bit
            if ((buf[0] & 0x10) != 0) {
                energyExpended = characteristic.getIntValue(FORMAT_UINT16, offset);
                offset += 2;
            }

            // RR-Interval bit
            if ((buf[0] & 0x08) != 0) {
                int value = characteristic.getIntValue(FORMAT_UINT16, offset);
                rrInterval = ((double) value / 1024.0) * 1000.0;
            }
        }

        if (BuildConfig.DEBUG) {
            Log.i("ABC", "HEART RATE: " + heartRate);
            Log.i("ABC", "EnergyExpended: " + energyExpended);
            Log.i("ABC", "RR-Interval: " + rrInterval);
        }

        BluetoothDevice device = gatt.getDevice();
        if (mListener != null) {
            mListener.onReceivedData(device, heartRate, energyExpended, rrInterval);
        }
    }

    /**
     * This class is the implement of BluetoothGattCallback.
     */
    private final BluetoothGattCallback mBluetoothGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(final BluetoothGatt gatt,
                                            final int status, final int newState) {
            Log.i("ABC", "@@@@@@ onConnectionStateChange: " + status + " -> " + newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHRDevices.remove(gatt);
                gatt.close();
                if (mListener != null) {
                    mListener.onDisconnected(gatt.getDevice());
                }
            }
        }

        @Override
        public void onServicesDiscovered(final BluetoothGatt gatt, final int status) {
            Log.w("ABC", "onServicesDiscovered");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (!hasHeartRateService(gatt)) {
                    // ble device has no heart rate service.
                    gatt.close();
                    if (mListener != null) {
                        mListener.onConnectFailed(gatt.getDevice());
                    }
                } else {
                    next(gatt);
                }
            } else {
                // connect error
                gatt.close();
                if (mListener != null) {
                    mListener.onDisconnected(gatt.getDevice());
                }
            }
        }

        @Override
        public void onCharacteristicRead(final BluetoothGatt gatt,
                                         final BluetoothGattCharacteristic characteristic, final int status) {
            Log.i("ABC", "@@@@@@ onCharacteristicRead: ");
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (isBodySensorLocation(characteristic)) {
                }
            }
            mHRDevices.put(gatt, DeviceState.REGISTER_NOTIFY);
            gatt.discoverServices();
        }

        @Override
        public void onCharacteristicChanged(final BluetoothGatt gatt,
                                            final BluetoothGattCharacteristic characteristic) {
            if (isHeartRateMeasurement(characteristic)) {
                notifyHeartRateMeasurement(gatt, characteristic);
            }
        }
    };

    private enum DeviceState {
        GET_LOCATION,
        REGISTER_NOTIFY,
        CONNECTED,
        DISCONNECT,
        ERROR,
    }

    /**
     * This interface is used to implement {@link HeartRateConnector} callbacks.
     */
    public static interface HeartRateConnectEventListener {
        void onConnected(BluetoothDevice device);

        void onDisconnected(BluetoothDevice device);

        void onConnectFailed(BluetoothDevice device);

        void onReceivedData(BluetoothDevice device, int heartRate,
                            int energyExpended, double rrInterval);
    }
}
