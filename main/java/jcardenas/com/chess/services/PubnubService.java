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


import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.pubnub.api.Callback;
import com.pubnub.api.PnGcmMessage;
import com.pubnub.api.PnMessage;
import com.pubnub.api.Pubnub;
import com.pubnub.api.PubnubError;
import com.pubnub.api.PubnubException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import jcardenas.com.chess.callbacks.BasicCallback;
import jcardenas.com.chess.models.ChatMessage;
import jcardenas.com.chess.utils.Constants;
import jcardenas.com.chess.utils.Score;


public class PubnubService extends Service {

    final static String TAG = PubnubService.class.getSimpleName();

    static Pubnub mPubNub;

  //  SignalListener mSignalListener;

    private FirebaseUser currentUser;

  //  String channel;

    private String gcmRegId;

    private GoogleCloudMessaging gcm;

    private final IBinder mBinder = new LocalBinder();
/*
    class SignalListener extends AsyncTask<String, Void, Boolean> {
        @Override
        protected Boolean doInBackground(String... params) {
            try {
                subscribeWithPresence();
            } catch (Exception e) {
                e.printStackTrace();
            }

            return Boolean.TRUE;
        }
    }*/

    public class LocalBinder extends Binder {
        public PubnubService getService() {
            return PubnubService.this;
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        FirebaseAuth auth = FirebaseAuth.getInstance();
        if ((currentUser = auth.getCurrentUser()) == null) {
            return null;
        }

        if (mPubNub == null) {
            this.mPubNub = new Pubnub(Constants.PUBLISH_KEY, Constants.SUBSCRIBE_KEY);
            this.mPubNub.setUUID(currentUser.getEmail());

            gcmRegister();
        }

        String channel = intent.getStringExtra("channel");
        if (channel != null) {
            subscribeWithPresence(channel, true);
        }
       /* if (mSignalListener == null) mSignalListener = new SignalListener();

        if (mSignalListener.getStatus() != AsyncTask.Status.RUNNING
                && mSignalListener.getStatus() != AsyncTask.Status.FINISHED) {
            mSignalListener.execute(this.channel);
        }*/

        return mBinder;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        //if (this.mPubNub != null)
        //    this.mPubNub.unsubscribeAll();

    }

    @Override
    public void onRebind(Intent intent) {
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return true;
    }

    public void subscribeWithPresence(String channel, boolean ownChannel){
        Callback subscribeCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {
                //Log.i(TAG, "PUBNUB -> Channel: " + channel + " Msg: " + message.toString());

                if (message instanceof JSONObject){
                    try {
                        JSONObject jsonObj = (JSONObject) message;
                        JSONObject json = jsonObj.getJSONObject("data");
                        String type = jsonObj.getString(Constants.TYPE);

                        if (type.equals(Constants.MOVE_ACTION)) {
                            final String username = json.getString(Constants.JSON_USER);
                            final String move = json.getString("move");

                            Intent sIntent = new Intent(Constants.PUBNUB_ACTION)
                                    .putExtra("action", Constants.MOVE_ACTION)
                                    .putExtra(Constants.JSON_USER, username)
                                    .putExtra(Constants.MOVE, move);

                            // Broadcasts the Intent to receivers in this app.
                            sendSignal(sIntent);

                        } else if (type.equals(Constants.NEW_MSG_ACTION)) {
                            String name = json.getString(Constants.JSON_USER);
                            String displayName = json.getString(Constants.JSON_DISPLAY_NAME);
                            String msg = json.getString(Constants.JSON_MSG);
                            long time = json.getLong(Constants.JSON_TIME);
                            if (name.equals(mPubNub.getUUID())) return; // Ignore own messages

                            final ChatMessage chatMsg = new ChatMessage(name, displayName, msg, time);
                            String action = channel.indexOf('_') == -1 ? Constants.NEW_MSG_ROOM_ACTION : Constants.NEW_MSG_ACTION;

                            Intent sIntent = new Intent(Constants.PUBNUB_ACTION)
                                    .putExtra("action", action)
                                    .putExtra("chatMsg", chatMsg);

                            sendSignal(sIntent);

                        } else if (type.equals(Constants.SURRENDER_ACTION)) {

                        } else {
                           // Log.i(TAG, "PUBNUB -> Tipo mensaje invalido: " + type);
                        }

                    } catch (JSONException e){ e.printStackTrace(); }
                }

               // Log.i(TAG, "PUBNUB -> Channel: " + channel + " Msg: " + message.toString());
            }

            @Override
            public void connectCallback(String channel, Object message) {
                // Log.i(TAG, "PUBNUB -> Subscribe -> Connected! " + channel  + " -- " + message.toString());

                hereNow(channel, false);
                setStateLogin(channel);
            }
        };

        // callback own channel
        Callback subscribeOwnCallback = new Callback() {
            @Override
            public void successCallback(String channel, Object message) {

                //Log.i(TAG, "PUBNUB -> Channel: " + channel + " Msg: " + message.toString());

                if (message instanceof JSONObject){
                    JSONObject jsonObj = (JSONObject)message;
                    try {
                        jsonObj = ((JSONObject) message).getJSONObject("pn_gcm");
                        jsonObj = jsonObj.getJSONObject("data");
                    } catch (JSONException e) {
                    }

                    try {
                        String type = jsonObj.getString(Constants.TYPE);
                        JSONObject json = jsonObj.getJSONObject("data");

                        if (type.equals(Constants.INVITATION_ACTION)) {
                            final String from = json.getString(Constants.GCM_INVITATION_FROM);
                            final String fromname = json.getString(Constants.GCM_INVITATION_FROM_NAME);

                            Intent sIntent = new Intent(Constants.PUBNUB_ACTION);
                            sIntent.putExtra("action", Constants.INVITATION_ACTION)
                                    .putExtra(Constants.GCM_INVITATION_FROM, from)
                                    .putExtra(Constants.GCM_INVITATION_FROM_NAME, fromname);

                           sendSignal(sIntent);

                        } else if (type.equals(Constants.ACCEPTED_ACTION)) {
                            final String from = json.getString(Constants.GCM_INVITATION_FROM);
                            final String fromname = json.getString(Constants.GCM_INVITATION_FROM_NAME);
                            final String choose = json.getString(Constants.CHOOSE);

                            Intent sIntent = new Intent(Constants.PUBNUB_ACTION);
                            sIntent.putExtra("action", Constants.ACCEPTED_ACTION)
                                    .putExtra(Constants.GCM_INVITATION_FROM, from)
                                    .putExtra(Constants.GCM_INVITATION_FROM_NAME, fromname)
                                    .putExtra(Constants.CHOOSE, choose);

                            sendSignal(sIntent);

                        }
                    } catch (JSONException e){ e.printStackTrace(); }
                }
            }

            @Override
            public void connectCallback(String channel, Object message) {
                //Log.i(TAG, "PUBNUB -> Subscribe Connected! " + channel + " " + message.toString());

            }
        };

        try {
            mPubNub.subscribe(channel, subscribeCallback);

            // suscribe own channel
            if (ownChannel) {
                mPubNub.subscribe(currentUser.getEmail(), subscribeOwnCallback);
            }

            presenceSubscribe(channel);

        } catch (PubnubException e){
            e.printStackTrace();
        }
    }

    public void hereNow(String channel, final boolean displayUsers) {

        this.mPubNub.hereNow(channel, true, true, new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                try {
                    JSONObject json = (JSONObject) response;
                    final int occ = json.getInt("occupancy");

                    // full room
                    if (occ > 50) {

                        Intent sIntent =
                                new Intent(Constants.PUBNUB_ACTION);
                        sIntent.putExtra("action", Constants.FULL_ROOM_ACTION);
                        sendSignal(sIntent);

                        return;
                    }

                    Intent sIntent =
                            new Intent(Constants.PUBNUB_ACTION)
                                    .putExtra("action", Constants.HERE_NOW_ACTION)
                                    .putExtra("json", json.toString());

                    sendSignal(sIntent);

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void setStateLogin(String channel){
        Callback callback = new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
                //Log.i("PUBNUB", "State: " + response.toString());
            }
        };
        try {
            JSONObject state = new JSONObject();
            state.put(Constants.STATE_LOGIN, System.currentTimeMillis());
            state.put(Constants.STATE_DISPLAY_NAME, currentUser.getDisplayName());
            state.put(Constants.STATE_PHOTO_URL, currentUser.getPhotoUrl());
            state.put(Constants.STATE_SCORE, Score.getScore(this));

            this.mPubNub.setState(channel, this.mPubNub.getUUID(), state, callback);
        } catch (JSONException e) { e.printStackTrace(); }
    }


    public void presenceSubscribe(String channel)  {
        Callback callback = new Callback() {
            @Override
            public void successCallback(String channel, Object response) {
               // Log.i(TAG, "PUBNUB -> Pres: " + response.toString() + " class: " + response.getClass().toString());

                if (response instanceof JSONObject){
                    JSONObject json = (JSONObject) response;
                   // Log.i(TAG, "PUBNUB -> Presence: " + json.toString());
                    JSONObject data = null;
                    try {
                        data = json.getJSONObject("data");
                    } catch (JSONException e) {}

                    try {
                        final int occ = json.getInt("occupancy");
                        final String user = json.getString("uuid");
                        final JSONObject _data = data;
                        final String action = json.getString("action");

                        Intent sIntent =
                                new Intent(Constants.PUBNUB_ACTION)
                                        .putExtra("action", Constants.PRESENCE_ACTION)
                                        .putExtra("user", user)
                                        .putExtra("_action", action)
                                        .putExtra("_data", (_data == null ? null : _data.toString()));

                        sendSignal(sIntent);

                    } catch (JSONException e){ e.printStackTrace(); }
                }
            }

            @Override
            public void errorCallback(String channel, PubnubError error) {
                //Log.i(TAG, "PUBNUB -> Presence Error: " + error.toString());
            }
        };
        try {

            this.mPubNub.presence(channel, callback);

        } catch (PubnubException e) { e.printStackTrace(); }
    }

    static public void publish(String channel, JSONObject data, String type, boolean gcm){
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
            json.put("data", data);
        } catch (JSONException e) { e.printStackTrace(); }

        if (gcm) {
            PnGcmMessage gcmMessage = new PnGcmMessage();
            try {
                gcmMessage.setData(json);

                PnMessage message = new PnMessage(
                        mPubNub,
                        channel,
                        new BasicCallback(),
                        gcmMessage);
                message.put("pn_debug",true);
                message.publish();
            }
            catch (JSONException e) { e.printStackTrace(); }
            catch (PubnubException e) { e.printStackTrace(); }
        } else {
            mPubNub.publish(channel, json, new BasicCallback());
        }

        //Log.i(TAG, "PUBNUB -> SEND TO " + channel + " : " + json.toString());
    }

    static public void publish(String channel, JSONObject data, String type) {
        publish(channel, data, type, false);
    }

    static public void unsubscribe(String channel) {
        mPubNub.unsubscribe(channel);
    }

    public void sendSignal(Intent intent) {
        //LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        sendBroadcast(intent);

    }


    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                Toast.makeText(PubnubService.this, Constants.PLAY_SERVICES_RESOLUTION_REQUEST + "", Toast.LENGTH_SHORT).show();

                Log.e("GCM-error", Constants.PLAY_SERVICES_RESOLUTION_REQUEST + "");
            } else {
                Log.e("GCM-check", "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    private void registerInBackground() {
         new RegisterTask().execute();
    }

    private void storeRegistrationId(String regId) {
        SharedPreferences prefs = getSharedPreferences(Constants.CHAT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.GCM_REG_ID, regId);
        editor.apply();
    }


    private String getRegistrationId() {
        SharedPreferences prefs = getSharedPreferences(Constants.CHAT_PREFS, Context.MODE_PRIVATE);
        return prefs.getString(Constants.GCM_REG_ID, "");
    }

    private void sendRegistrationId(String regId) {
        this.mPubNub.enablePushNotificationsOnChannel(currentUser.getEmail(), regId, new BasicCallback());
    }

    private void gcmRegister() {
        if (checkPlayServices()) {
            gcm = GoogleCloudMessaging.getInstance(this);
            try {
                gcmRegId = getRegistrationId();
            } catch (Exception e) {
                e.printStackTrace();
            }

            if (gcmRegId.isEmpty()) {
                registerInBackground();
            } else {
                // Log.i("GCMM", gcmRegId);
               // Toast.makeText(this, "Registration ID already exists: " + gcmRegId, Toast.LENGTH_SHORT).show();
            }
        } else {
            // Log.e("GCM-register", "No valid Google Play Services APK found.");
        }
    }

    private void gcmUnregister() {
        new UnregisterTask().execute();
    }

    private void removeRegistrationId() {
        SharedPreferences prefs = getSharedPreferences(Constants.CHAT_PREFS, Context.MODE_PRIVATE);

        SharedPreferences.Editor editor = prefs.edit();
        editor.remove(Constants.GCM_REG_ID);
        editor.apply();
    }

    private class RegisterTask extends AsyncTask<Void, Void, String> {
        @Override
        protected String doInBackground(Void... params) {
            String msg="";
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(PubnubService.this);
                }
                gcmRegId = gcm.register(Constants.GCM_SENDER_ID);
                msg = "Device registered, registration ID: " + gcmRegId;

                sendRegistrationId(gcmRegId);

                storeRegistrationId(gcmRegId);

                Log.i("GCM-register", msg);
            } catch (IOException e){
                e.printStackTrace();
            }
            return msg;
        }
    }

    private class UnregisterTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                if (gcm == null) {
                    gcm = GoogleCloudMessaging.getInstance(PubnubService.this);
                }

                // Unregister from GCM
                gcm.unregister();

                // Remove Registration ID from memory
                removeRegistrationId();

                // Disable Push Notification
                mPubNub.disablePushNotificationsOnChannel(currentUser.getEmail(), gcmRegId);

            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }
    }

}
