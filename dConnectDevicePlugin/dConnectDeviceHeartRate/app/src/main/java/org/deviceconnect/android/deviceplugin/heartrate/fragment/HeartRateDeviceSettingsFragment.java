/*
 HeartRateDeviceSettingsFragment
 Copyright (c) 2015 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.heartrate.fragment;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import org.deviceconnect.android.deviceplugin.heartrate.HeartRateApplication;
import org.deviceconnect.android.deviceplugin.heartrate.HeartRateManager;
import org.deviceconnect.android.deviceplugin.heartrate.R;
import org.deviceconnect.android.deviceplugin.heartrate.activity.HeartRateDeviceSettingsActivity;
import org.deviceconnect.android.deviceplugin.heartrate.data.HeartRateDevice;
import org.deviceconnect.android.deviceplugin.heartrate.fragment.dialog.ErrorDialogFragment;
import org.deviceconnect.android.deviceplugin.heartrate.fragment.dialog.ProgressDialogFragment;

import java.util.ArrayList;
import java.util.List;

import static org.deviceconnect.android.deviceplugin.heartrate.HeartRateManager.OnHeartRateDiscoveryListener;

/**
 * This fragment do setting of the connection to the ble device.
 * @author NTT DOCOMO, INC.
 */
public class HeartRateDeviceSettingsFragment extends Fragment {
    /**
     * Adapter.
     */
    private DeviceAdapter mDeviceAdapter;

    /**
     * Dialog.
     */
    private DialogFragment mDialogFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        setRetainInstance(true);

        mDeviceAdapter = new DeviceAdapter(getActivity(), createDeviceContainers());

        View rootView = inflater.inflate(R.layout.fragment_heart_rate_device_settings, null);
        ListView listView = (ListView) rootView.findViewById(R.id.device_list_view);
        listView.setAdapter(mDeviceAdapter);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        getManager().setOnHeartRateDiscoveryListener(mEvtListener);
        getManager().startScanBle();
    }

    @Override
    public void onPause() {
        super.onPause();
        getManager().setOnHeartRateDiscoveryListener(null);
        getManager().stopScanBle();
    }

    /**
     * Connect to Ble device has heart rate service.
     * @param device Ble device has heart rate service.
     */
    private void connectDevice(DeviceContainer device) {
        getManager().connectBleDevice(device.getAddress());
        showProgressDialog(device.getName());
    }

    /**
     * Disconnect to Ble device has heart rate service.
     * @param device Ble device has heart rate service.
     */
    private void disconnectDevice(final DeviceContainer device) {
        getManager().disconnectBleDevice(device.getAddress());

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                DeviceContainer container = findDeviceContainerByAddress(device.getAddress());
                if (container != null) {
                    container.setRegisterFlag(false);
                    mDeviceAdapter.notifyDataSetChanged();
                }
            }
        });

    }

    private void showProgressDialog(String name) {
        Resources res = getActivity().getResources();
        String title = res.getString(R.string.heart_rate_setting_connecting_title);
        String message = res.getString(R.string.heart_rate_setting_connecting_message, name);
        mDialogFragment = ProgressDialogFragment.newInstance(title, message);
        mDialogFragment.show(getFragmentManager(), "dialog");
    }

    private void dismissProgressDialog() {
        if (mDialogFragment != null) {
            mDialogFragment.dismiss();
        }
    }

    private void showErrorDialog(String name) {
        Resources res = getActivity().getResources();
        String title = res.getString(R.string.heart_rate_setting_dialog_error_title);
        String message = null;
        if (name == null) {
            res.getString(R.string.heart_rate_setting_dialog_error_message,
                    R.string.heart_rate_setting_default_name);
        } else {
            res.getString(R.string.heart_rate_setting_dialog_error_message, name);
        }
        mDialogFragment = ErrorDialogFragment.newInstance(title, message);
        mDialogFragment.show(getFragmentManager(), "dialog");
    }

    /**
     * Gets a instance of HeartRateManager.
     * @return HeartRateManager
     */
    private HeartRateManager getManager() {
        HeartRateDeviceSettingsActivity activity =
                (HeartRateDeviceSettingsActivity) getActivity();
        HeartRateApplication application =
                (HeartRateApplication) activity.getApplication();
        return application.getHeartRateManager();
    }

    private OnHeartRateDiscoveryListener mEvtListener = new OnHeartRateDiscoveryListener() {
        @Override
        public void onConnected(final BluetoothDevice device) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DeviceContainer container = findDeviceContainerByAddress(device.getAddress());
                    if (container != null) {
                        container.setRegisterFlag(true);
                        mDeviceAdapter.notifyDataSetChanged();
                    }
                    dismissProgressDialog();
                }
            });
        }

        @Override
        public void onConnectFailed(final BluetoothDevice device) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    dismissProgressDialog();
                    showErrorDialog(device.getName());
                }
            });
        }

        @Override
        public void onDiscovery(final List<BluetoothDevice> devices) {
           getActivity().runOnUiThread(new Runnable() {
               @Override
               public void run() {
                   mDeviceAdapter.clear();
                   mDeviceAdapter.addAll(createDeviceContainers());
                   for (BluetoothDevice device : devices) {
                       if (!containAddressForAdapter(device)) {
                           mDeviceAdapter.add(createContainer(device));
                       }
                   }
                   mDeviceAdapter.notifyDataSetChanged();
               }
           });
        }
    };

    private List<DeviceContainer> createDeviceContainers() {
        List<DeviceContainer> containers = new ArrayList<>();
        List<HeartRateDevice> devices = getManager().getRegisterDevices();
        for (HeartRateDevice device : devices) {
            containers.add(createContainer(device, true));
        }
        return containers;
    }

    private DeviceContainer findDeviceContainerByAddress(final String address) {
        int size = mDeviceAdapter.getCount();
        for (int i = 0; i < size; i++) {
            DeviceContainer container = mDeviceAdapter.getItem(i);
            if (container.getAddress().equalsIgnoreCase(address)) {
                return container;
            }
        }
        return null;
    }

    private DeviceContainer createContainer(final BluetoothDevice device) {
        DeviceContainer container = new DeviceContainer();
        container.setName(device.getName());
        container.setAddress(device.getAddress());
        return container;
    }

    private DeviceContainer createContainer(final HeartRateDevice device, final boolean register) {
        DeviceContainer container = new DeviceContainer();
        container.setName(device.getName());
        container.setAddress(device.getAddress());
        container.setRegisterFlag(register);
        return container;
    }

    private boolean containAddressForAdapter(final BluetoothDevice device) {
        int size = mDeviceAdapter.getCount();
        for (int i = 0; i < size; i++) {
            DeviceContainer container = mDeviceAdapter.getItem(i);
            if (container.getAddress().equalsIgnoreCase(device.getAddress())) {
                return true;
            }
        }
        return false;
    }

    private class DeviceContainer {
        private String mName;
        private String mAddress;
        private boolean mRegisterFlag;

        public String getName() {
            return mName;
        }

        public void setName(String name) {
            if (name == null) {
                mName = getActivity().getResources().getString(
                    R.string.heart_rate_setting_default_name);
            } else {
                mName = name;
            }
        }

        public String getAddress() {
            return mAddress;
        }

        public void setAddress(String address) {
            mAddress = address;
        }

        public boolean isRegisterFlag() {
            return mRegisterFlag;
        }

        public void setRegisterFlag(boolean registerFlag) {
            mRegisterFlag = registerFlag;
        }
    }

    private class DeviceAdapter extends ArrayAdapter<DeviceContainer> {
        private LayoutInflater mInflater;
        public DeviceAdapter(final Context context, final List<DeviceContainer> objects) {
            super(context, 0, objects);
            mInflater = (LayoutInflater) context.getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.item_heart_rate_device, null);
            }

            final DeviceContainer device = getItem(position);

            TextView nameView = (TextView) convertView.findViewById(R.id.device_name);
            nameView.setText(device.getName());

            TextView addressView = (TextView) convertView.findViewById(R.id.device_address);
            addressView.setText(device.getAddress());

            Button btn = (Button) convertView.findViewById(R.id.btn_connect_device);
            if (device.isRegisterFlag()) {
                btn.setBackgroundResource(R.drawable.button_red);
                btn.setText(R.string.heart_rate_setting_disconnect);
            } else {
                btn.setBackgroundResource(R.drawable.button_blue);
                btn.setText(R.string.heart_rate_setting_connect);
            }
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(final View v) {
                    if (device.isRegisterFlag()) {
                        disconnectDevice(device);
                    } else {
                        connectDevice(device);
                    }
                }
            });

            return convertView;
        }
    }
}
