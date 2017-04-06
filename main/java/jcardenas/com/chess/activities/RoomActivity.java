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
package jcardenas.com.chess.activities;


import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import jcardenas.com.chess.R;
import jcardenas.com.chess.adapters.ChatAdapter;
import jcardenas.com.chess.adapters.OnlineAdapter;
import jcardenas.com.chess.models.ChatMessage;
import jcardenas.com.chess.models.User;
import jcardenas.com.chess.services.PubnubService;
import jcardenas.com.chess.utils.Constants;
import jcardenas.com.chess.utils.SoundPlayer;


public class RoomActivity extends BaseActivity {
    final static String TAG = RoomActivity.class.getSimpleName();

    private RecyclerView mRecyclerViewChats;
    private RecyclerView mRecyclerViewOnline;

    private String channel;
    private String username;
    private FirebaseUser currentUser;
    private ChatAdapter mChatAdapter;
    private OnlineAdapter mOnlineAdapter;
    private RoomBroadcastReceiver receiver;


    private Intent mServiceIntent;
    private static PubnubService mService;
    private boolean mBound = false;
    private SoundPlayer mplayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkAuth()) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();

        Intent intent = getIntent();
        if (intent.getStringExtra("action") == null)
        {
            setContentView(R.layout.activity_room);

            this.channel = intent.getStringExtra("channel");
            this.username = auth.getCurrentUser().getEmail();

            if (savedInstanceState != null) {
                this.mChatAdapter = new ChatAdapter((ArrayList<ChatMessage>)savedInstanceState.getSerializable("chats"));
                // online adapter
                this.mOnlineAdapter = new OnlineAdapter((ArrayList<User>)savedInstanceState.getSerializable("online"), this);
            } else {
                this.mChatAdapter = new ChatAdapter(new ArrayList<ChatMessage>());
                // online adapter
                this.mOnlineAdapter = new OnlineAdapter(new ArrayList<User>(), this);
            }

            this.mChatAdapter.userPresence(this.username, "join");

            this.mRecyclerViewChats = (RecyclerView) findViewById(R.id.chats);
            this.mRecyclerViewOnline = (RecyclerView) findViewById(R.id.usersOnline);

            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());

            this.mRecyclerViewChats.setLayoutManager(mLayoutManager);
            this.mRecyclerViewChats.setItemAnimator(new DefaultItemAnimator());

            RecyclerView.LayoutManager mLayoutManager2 = new LinearLayoutManager(getApplicationContext());
            this.mRecyclerViewOnline.setLayoutManager(mLayoutManager2);
            this.mRecyclerViewOnline.setItemAnimator(new DefaultItemAnimator());

           // setupAutoScroll();
            this.mRecyclerViewChats.setAdapter(mChatAdapter);
            this.mRecyclerViewOnline.setAdapter(mOnlineAdapter);

            mplayer = new SoundPlayer(this, R.raw.thin);

            Button btn = (Button) findViewById(R.id.send);
            btn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    EditText edit = (EditText) findViewById(R.id.editText);
                    String msg = edit.getText().toString();
                    edit.setText("");
                    long unixTime = System.currentTimeMillis() / 1000L;

                    if (!msg.equals("")) {

                        final ChatMessage chatMsg = new ChatMessage(currentUser.getEmail(),
                                currentUser.getDisplayName(), msg, unixTime);
                        RoomActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                RoomActivity.this.mChatAdapter.addMessage(chatMsg);
                            }
                        });

                        JSONObject json = new JSONObject();
                        try {
                            json.put(Constants.JSON_DISPLAY_NAME, currentUser.getDisplayName());
                            json.put(Constants.JSON_USER, currentUser.getEmail());
                            json.put(Constants.JSON_TIME, unixTime);
                            json.put(Constants.JSON_MSG, msg);

                            PubnubService.publish(RoomActivity.this.getChannel(), json, Constants.NEW_MSG_ACTION);

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.msg_error), Toast.LENGTH_SHORT);
                            e.printStackTrace();
                        }

                    }
                }
            });

            setupAutoScroll();
            setupListView();

            initializeServices();
        } else {
            initializeServices();

            if (intent.getStringExtra("action").equals(Constants.INVITATION_ACTION)) {

                Intent _intent = new Intent(Constants.PUBNUB_ACTION);
                _intent.putExtra("action", Constants.INVITATION_ACTION)
                        .putExtra(Constants.GCM_INVITATION_FROM, intent.getStringExtra(Constants.GCM_INVITATION_FROM))
                        .putExtra(Constants.GCM_INVITATION_FROM_NAME, intent.getStringExtra(Constants.GCM_INVITATION_FROM_NAME));
                sendBroadcast(_intent);

                setContentView(R.layout.activity_room_invitation);

                return;
            } else if (intent != null && intent.getStringExtra("action").equals(Constants.ACCEPTED_ACTION)) {
                Intent _intent = new Intent(Constants.PUBNUB_ACTION);
                _intent.putExtra("action", Constants.ACCEPTED_ACTION)
                        .putExtra(Constants.GCM_INVITATION_FROM, intent.getStringExtra(Constants.GCM_INVITATION_FROM))
                        .putExtra(Constants.GCM_INVITATION_FROM_NAME, intent.getStringExtra(Constants.GCM_INVITATION_FROM_NAME))
                        .putExtra(Constants.CHOOSE, intent.getStringExtra(Constants.CHOOSE));
                sendBroadcast(_intent);

                setContentView(R.layout.activity_room_invitation);

                return;
            }
        }

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable("chats", (Serializable) mChatAdapter.getValues());
        outState.putSerializable("online", (Serializable) mOnlineAdapter.getValues());

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        return true;
    }

    @Override
    protected void onDestroy() {
        if (mBound) {
            PubnubService.unsubscribe(this.channel);
            unbindService(mServiceConnection);
            mBound = false;
        }

        super.onDestroy();
    }

    private void initializeServices() {
        installFilters();

        mServiceIntent = new Intent(this, PubnubService.class);
        mServiceIntent.putExtra("username", this.username);
        mServiceIntent.putExtra("channel", this.channel);

        this.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    public String getChannel() {
        return channel;
    }

    public void installFilters() {
        receiver = new RoomBroadcastReceiver();
        receiver.setActivity(this);

        IntentFilter filter = new IntentFilter(Constants.PUBNUB_ACTION);

        registerReceiver(receiver, filter);
    }

    static PubnubService getService() {
        return mService;
    }

    private void setupAutoScroll(){
        this.mChatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
               // super.onChanged();
             //   mRecyclerViewChats.smoothScrollToPosition(0);//mChatAdapter.getItemCount());
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mRecyclerViewChats.smoothScrollToPosition(0);
                mplayer.play();
            }
        });
       /* this.mChatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mChatListView.setSelection(mChatAdapter.getCount() - 1);
            }
        });*/
    }

    private void setupListView(){
        /*this.mChatListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ChatMessage chat = mChatAdapter.getItem(position);

               // sendNotification(chat.getUsername());
            }
        });*/


        mOnlineAdapter.setOnItemClickListener(new OnlineAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View view) {
                User item = mOnlineAdapter.getValues().get(position);
                final String toUser = item.getUsername();
                final String toDisplayName = item.getDisplayName();

                RoomActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(RoomActivity.this);
                        builder.setTitle(getString(R.string.challenge_friend))
                                .setMessage(getString(R.string.challenge_friend_desc) + " " + toDisplayName + "?")
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        sendInvitation(toUser);
                                    }
                                })
                                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });

            }

        });
    }

    public void sendInvitation(String toUser) {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.GCM_INVITATION_FROM, currentUser.getEmail());
            json.put(Constants.GCM_INVITATION_FROM_NAME, currentUser.getDisplayName());
            json.put(Constants.GCM_INVITATION_TO, toUser);

            PubnubService.publish(toUser, json, Constants.INVITATION_ACTION, true);

        } catch (JSONException e) { e.printStackTrace(); }

    }


    public void sendAccepted(String toUser, String choose) {
        JSONObject json = new JSONObject();
        try {
            json.put(Constants.GCM_INVITATION_FROM, currentUser.getEmail());
            json.put(Constants.GCM_INVITATION_FROM_NAME, currentUser.getDisplayName());
            json.put(Constants.GCM_INVITATION_TO, toUser);
            json.put(Constants.CHOOSE, choose);

            PubnubService.publish(toUser, json, Constants.ACCEPTED_ACTION, true);

        } catch (JSONException e) { e.printStackTrace(); }
    }

    public void acceptInvitation(String rival, String rivalName, String choose) {

        Intent i = new Intent(RoomActivity.this, GameActivity.class);
        i.putExtra("mode", "online");
        i.putExtra("color", (choose.equals("white") ? "black" : "white"));
        i.putExtra("username", currentUser.getEmail());
        i.putExtra("rival", rival);
        i.putExtra("rivalName", rivalName);

        startActivity(i);
    }

    static public class RoomBroadcastReceiver extends BroadcastReceiver {
        static RoomActivity activity;

        public void setActivity(RoomActivity activity) {
            RoomBroadcastReceiver.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getStringExtra("action");
            Log.i("ONRECEIVE", action);

            if (action.equals(Constants.NEW_MSG_ROOM_ACTION)) {
                // new message action
                final ChatMessage chatMsg = (ChatMessage)intent.getSerializableExtra("chatMsg");
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.mChatAdapter.addMessage(chatMsg);
                    }
                });

            } else if (action.equals(Constants.INVITATION_ACTION)) {
                // new invitation
                final String from = intent.getStringExtra(Constants.GCM_INVITATION_FROM);
                final String fromname = intent.getStringExtra(Constants.GCM_INVITATION_FROM_NAME);
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                        builder
                                .setTitle(activity.getString(R.string.new_challenge))
                                .setMessage(activity.getString(R.string.new_challenge_desc) + " " + fromname)
                                .setIcon(android.R.drawable.ic_dialog_info)
                                .setPositiveButton(activity.getString(R.string.accept), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        activity.sendAccepted(from, "black");
                                        activity.acceptInvitation(from, fromname, "white");
                                    }
                                })
                                .setNegativeButton(activity.getString(R.string.decline), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {

                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    }
                });

            } else if (action.equals(Constants.ACCEPTED_ACTION)) {
                // accepted
                final String from = intent.getStringExtra(Constants.GCM_INVITATION_FROM);
                final String fromname = intent.getStringExtra(Constants.GCM_INVITATION_FROM_NAME);
                final String choose = intent.getStringExtra(Constants.CHOOSE);

                activity.acceptInvitation(from, fromname, choose);

            } else if (action.equals(Constants.FULL_ROOM_ACTION)) {
                // full room action
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(activity.getBaseContext(),
                                activity.getString(R.string.full_room), Toast.LENGTH_LONG).show();
                    }
                });
                activity.finish();
            } else if (action.equals(Constants.HERE_NOW_ACTION)) {
                // here now
                String jsonString = intent.getStringExtra("json");
                JSONObject json = null;

                try {
                    json = new JSONObject(jsonString);
                } catch (JSONException e) {
                    e.printStackTrace();
                    return;
                }

                try {
                    final Set<String> usersOnline = new HashSet<String>();

                    final JSONArray hereNowJSON = json.getJSONArray("uuids");

                    final ArrayList<User> listUsers = new ArrayList<User>();
                    for (int i = 0; i < hereNowJSON.length(); i++) {
                        JSONObject jobj = hereNowJSON.getJSONObject(i);
                        String uuid = jobj.getString("uuid");

                        if (uuid.equals(activity.currentUser.getEmail())) continue;

                        String displayName = null;
                        String photoUrl = null;
                        int score = 0;
                        try {
                            JSONObject state = jobj.getJSONObject("state");
                            displayName = state.getString("displayName");
                            photoUrl = state.getString("photoUrl");
                            score = state.getInt("score");
                        } catch (JSONException e) {
                            Log.i("JSON", jobj.toString());
                            e.printStackTrace();
                        }

                        usersOnline.add(uuid);
                        listUsers.add(new User(uuid, displayName, photoUrl, score));
                    }

                    final int occ = listUsers.size();

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            activity.mChatAdapter.setOnlineNow(usersOnline);
                            activity.mOnlineAdapter.setUsers(listUsers);
                            try {
                                activity.getSupportActionBar().setTitle(activity.getChannel() + " (" + occ + ")");
                            }catch (Exception e) {
                            }
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            } else if (action.equals(Constants.PRESENCE_ACTION)) {
                final String user = intent.getStringExtra("user");
                JSONObject data = null;
                try {
                    String sdata = intent.getStringExtra("_data");
                    data = new JSONObject(sdata);
                } catch (Exception e) {
                    e.printStackTrace();
                    return;
                }

                if (user.equals(activity.currentUser.getEmail())) return;

                final String _action = intent.getStringExtra("_action");
                final JSONObject _data = data;

                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.mChatAdapter.userPresence(user, _action);
                        activity.mOnlineAdapter.updateAction(user, _action, _data);

                        try {
                            int occ = activity.mOnlineAdapter.getItemCount();
                            activity.getSupportActionBar().setTitle(activity.getChannel() + " (" + occ + ")");
                        } catch (Exception e) {
                        }
                    }
                });
            }

        }
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            PubnubService.LocalBinder binder = (PubnubService.LocalBinder) service;
            mService = binder.getService();
            mBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            mBound = false;
        }
    };

}
