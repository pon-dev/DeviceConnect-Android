/*
 HeartRateDevice
 Copyright (c) 2015 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.heartrate.data;

/**
 * @author NTT DOCOMO, INC.
 */
public class HeartRateDevice {
    private int mId = -1;
    private String mName;
    private String mAddress;
    private boolean mRegisterFlag;
    private boolean mConnectFlag;

    public int getId() {
        return mId;
    }

    public void setId(int id) {
        mId = id;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        mName = name;
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

    public boolean isConnectFlag() {
        return mConnectFlag;
    }

    public void setConnectFlag(boolean connectFlag) {
        mConnectFlag = connectFlag;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        HeartRateDevice that = (HeartRateDevice) o;

        if (mId != that.mId) {
            return false;
        }
        if (mAddress != null ? !mAddress.equals(that.mAddress) : that.mAddress != null) {
            return false;
        }
        if (mName != null ? !mName.equals(that.mName) : that.mName != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = mId;
        result = 31 * result + (mName != null ? mName.hashCode() : 0);
        result = 31 * result + (mAddress != null ? mAddress.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("{\"name\": " + mName + ", ");
        builder.append("\"address\": " + mAddress + ", ");
        builder.append("\"registerFlag\": " + mRegisterFlag + ", ");
        builder.append("\"connectFlag\": " + mConnectFlag + "} ");
        return builder.toString();
    }
}
