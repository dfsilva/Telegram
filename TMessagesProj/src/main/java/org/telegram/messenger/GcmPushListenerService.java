/*
 * This is the source code of Telegram for Android v. 3.x.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2017.
 */

package org.telegram.messenger;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;

import com.google.android.gms.gcm.GcmListenerService;

import org.json.JSONObject;
import org.telegram.tgnet.ConnectionsManager;

public class GcmPushListenerService extends GcmListenerService {

    public static final int NOTIFICATION_ID = 1;

    @Override
    public void onMessageReceived(String from, final Bundle bundle) {
        FileLog.d("GCM received bundle: " + bundle + " from: " + from);
        AndroidUtilities.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                ApplicationLoader.postInitApplication();

                try {
                    String key = bundle.getString("loc_key");
                    if ("DC_UPDATE".equals(key)) {
                        String data = bundle.getString("custom");
                        JSONObject object = new JSONObject(data);
                        int dc = object.getInt("dc");
                        String addr = object.getString("addr");
                        String[] parts = addr.split(":");
                        if (parts.length != 2) {
                            return;
                        }
                        String ip = parts[0];
                        int port = Integer.parseInt(parts[1]);
                        ConnectionsManager.getInstance().applyDatacenterAddress(dc, ip, port);
                    } else {
                        if (ApplicationLoader.mainInterfacePaused) {
                            int value = bundle.getInt("badge", -1);
                            if (value == -1) {
                                ConnectivityManager connectivityManager = (ConnectivityManager) ApplicationLoader.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
                                NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
                                if (netInfo == null || !netInfo.isConnected()) {
                                    NotificationsController.getInstance().showSingleBackgroundNotification();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(e);
                }

                ConnectionsManager.onInternalPushReceived();
                ConnectionsManager.getInstance().resumeNetworkMaybe();
            }
        });
    }
}
