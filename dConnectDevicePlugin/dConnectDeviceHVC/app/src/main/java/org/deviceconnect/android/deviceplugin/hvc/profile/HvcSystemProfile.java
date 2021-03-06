/*
 HvcSystemProfile.java
 Copyright (c) 2015 NTT DOCOMO,INC.
 Released under the MIT license
 http://opensource.org/licenses/mit-license.php
 */
package org.deviceconnect.android.deviceplugin.hvc.profile;

import org.deviceconnect.android.deviceplugin.hvc.setting.HvcSettingStepsActivity;
import org.deviceconnect.android.event.EventManager;
import org.deviceconnect.android.message.MessageUtils;
import org.deviceconnect.android.profile.SystemProfile;
import org.deviceconnect.message.DConnectMessage;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

/**
 * HVC DevicePlugin, System Profile.
 * 
 * @author NTT DOCOMO, INC.
 */
public class HvcSystemProfile extends SystemProfile {

    /**
     * set setting activity.
     * 
     * @param request request
     * @param bundle bundle
     * 
     * @return setting activity
     */
    protected Class<? extends Activity> getSettingPageActivity(final Intent request, final Bundle bundle) {
        return HvcSettingStepsActivity.class;
    }

    @Override
    protected boolean onDeleteEvents(final Intent request, final Intent response, final String sessionKey) {

        if (sessionKey == null || sessionKey.length() == 0) {
            MessageUtils.setInvalidRequestParameterError(response);
        } else if (EventManager.INSTANCE.removeEvents(sessionKey)) {
            setResult(response, DConnectMessage.RESULT_OK);
        } else {
            MessageUtils.setUnknownError(response);
        }

        return true;
    }
}
