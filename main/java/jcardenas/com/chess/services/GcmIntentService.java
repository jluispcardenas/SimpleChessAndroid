/*
 * Copyright 2016 Jose Luis Cardenas - jluis.pcardenas@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package jcardenas.com.chess.services;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.ExecutionException;

import jcardenas.com.chess.utils.ForegroundCheckTask;
import jcardenas.com.chess.GcmBroadcastReceiver;
import jcardenas.com.chess.R;
import jcardenas.com.chess.activities.RoomActivity;
import jcardenas.com.chess.utils.Constants;


public class GcmIntentService  extends IntentService {

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        String messageType = gcm.getMessageType(intent);
        if (!extras.isEmpty() && GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE.equals(messageType)) {
            try {
                sendNotification(intent.getExtras());
            } catch (JSONException ex) {
                ex.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    private void sendNotification(Bundle extras) throws JSONException, InterruptedException, ExecutionException {
        if (extras == null) return;

        boolean foregroud = new ForegroundCheckTask().execute(getApplicationContext()).get();

        if (foregroud) return;

        Log.i("GCM-notif",extras.toString());

        String type = extras.getString(Constants.TYPE);

        if (type != null) {
            String notifBigTex = null;
            String notifContent = null;
            PendingIntent contentIntent = null;
            if (type.equals(Constants.INVITATION_ACTION)) {
                JSONObject json = new JSONObject(extras.getString("data"));

                final String from = json.getString(Constants.GCM_INVITATION_FROM);
                final String fromname = json.getString(Constants.GCM_INVITATION_FROM_NAME);

                notifBigTex = getString(R.string.notif_title1) + " " + fromname;
                notifContent = getString(R.string.notif_desc1);

                Intent intent = new Intent(getBaseContext(), RoomActivity.class);//Constants.PUBNUB_ACTION);
                        intent.putExtra("action", Constants.INVITATION_ACTION)
                        .putExtra(Constants.GCM_INVITATION_FROM, from)
                        .putExtra(Constants.GCM_INVITATION_FROM_NAME, fromname);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            } else if (type.equals(Constants.ACCEPTED_ACTION)) {
                JSONObject json = new JSONObject(extras.getString("data"));

                final String from = json.getString(Constants.GCM_INVITATION_FROM);
                final String fromname = json.getString(Constants.GCM_INVITATION_FROM_NAME);
                final String choose = json.getString(Constants.CHOOSE);

                notifBigTex = getString(R.string.notif_title2) + fromname;
                notifContent = getString(R.string.notif_desc2);

                Intent intent = new Intent(getBaseContext(), RoomActivity.class);
                intent.putExtra("action", Constants.ACCEPTED_ACTION)
                        .putExtra(Constants.GCM_INVITATION_FROM, from)
                        .putExtra(Constants.GCM_INVITATION_FROM_NAME, fromname)
                        .putExtra(Constants.CHOOSE, choose);

                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);

                contentIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            }

            if (notifBigTex != null && contentIntent != null) {
                // Bitmap icon = BitmapFactory.decodeResource(this.getResources(),
                //       R.drawable.ic_pn_chat);
                NotificationManager mNotificationManager = (NotificationManager)
                        this.getSystemService(Context.NOTIFICATION_SERVICE);

                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(this)
                                //              .setLargeIcon(icon)
                                .setSmallIcon(android.R.drawable.star_on)
                                .setContentTitle(notifBigTex)
                                .setStyle(new NotificationCompat.BigTextStyle()
                                        .bigText(notifBigTex))
                                .setContentText(notifContent)
                                .setAutoCancel(true);

                mBuilder.setContentIntent(contentIntent);
                Notification pnNotif = mBuilder.build();
                mNotificationManager.notify(0, pnNotif);  // Set notification ID
            }
        }
    }
}
