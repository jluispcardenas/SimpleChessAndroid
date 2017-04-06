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
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jcardenas.com.chess.callbacks.BoardCallback;
import jcardenas.com.chess.views.BoardView;
import jcardenas.com.chess.models.Mode;
import jcardenas.com.chess.models.Move;
import jcardenas.com.chess.models.Piece;
import jcardenas.com.chess.models.PieceColor;
import jcardenas.com.chess.R;
import jcardenas.com.chess.adapters.ChatAdapter;
import jcardenas.com.chess.adapters.OnlineAdapter;
import jcardenas.com.chess.models.ChatMessage;
import jcardenas.com.chess.models.User;
import jcardenas.com.chess.services.PubnubService;
import jcardenas.com.chess.utils.Constants;
import jcardenas.com.chess.utils.Score;
import jcardenas.com.chess.utils.SoundPlayer;


public class GameActivity extends BaseActivity {

    final static String TAG = GameActivity.class.getSimpleName();

    private String username;
    private String rival;
    private String rivalName;
    private String channel;
    private BoardView biw;
    private FirebaseUser currentUser;
    private GameBroadcastReceiver receiver;

    private ChatAdapter mChatAdapter;
    private OnlineAdapter mOnlineAdapter;

    private RecyclerView mRecyclerViewChats;
    private RecyclerView mRecyclerViewOnline;

   // Intent mServiceIntent;
    private PubnubService mService;
   // boolean mBound = false;
    private static SoundPlayer[] mplayer;
    private boolean playSounds = true;

    static private boolean onGame = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkAuth()) return;

        FirebaseAuth auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        RelativeLayout layout;
        Intent myIntent = getIntent();
        String mode = myIntent.getStringExtra("mode");

        // check sound preferences
        SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        playSounds = sharedPrefs.getBoolean("sounds", true);
        SoundPlayer.allowSounds(playSounds);

        mplayer = new SoundPlayer[3];
        mplayer[0] = new SoundPlayer(this, R.raw.thin);
        mplayer[1] = new SoundPlayer(this, R.raw.base);
        mplayer[2] = new SoundPlayer(this, R.raw.goodnews);

         biw = new BoardView(getBaseContext(), this);
        if (mode.equals("online")) {
            setContentView(R.layout.activity_game);
            layout = (RelativeLayout)findViewById(R.id.game_layout);

            String color = myIntent.getStringExtra("color");
            username = myIntent.getStringExtra("username");
            rival = myIntent.getStringExtra("rival");
            rivalName = myIntent.getStringExtra("rivalName");

            this.channel = generateChannelName(username, rival);

            biw.setMainColor(color.equals("white") ? PieceColor.WHITE : PieceColor.BLACK);

            biw.setMode(Mode.HUMAN_ONLINE);

            biw.setBoardCallback(new BoardCallback() {
                @Override
                public void onMoveComplete(Move move) {
                    mplayer[1].play();
                }
                @Override
                public void onCheck(PieceColor color) {
                    if (color == biw.getMainColor()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.check), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onGameOver(PieceColor color) {
                    if (color == biw.getMainColor()) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                        builder.setTitle(getString(R.string.you_lost)).setMessage(getString(R.string.you_lost_desc_online)).setIcon(android.R.drawable.ic_delete)
                                .setNeutralButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        finish();
                                    }
                                });
                        AlertDialog dialog = builder.create();
                        dialog.show();
                    } else {
                        biw.setGameOver(true);
                        mplayer[2].play();
                        updateRating();
                    }

                }
            });

            initializeOnline();

            this.mRecyclerViewChats = (RecyclerView) findViewById(R.id.chats);
            this.mRecyclerViewOnline = (RecyclerView) findViewById(R.id.usersOnline);

            this.mChatAdapter = new ChatAdapter(new ArrayList<ChatMessage>());
            this.mOnlineAdapter = new OnlineAdapter(new ArrayList<User>(), this);

            this.mChatAdapter.userPresence(username, "join");

            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getApplicationContext());
            this.mRecyclerViewChats.setLayoutManager(mLayoutManager);
            this.mRecyclerViewChats.setItemAnimator(new DefaultItemAnimator());

            RecyclerView.LayoutManager mLayoutManager2 = new LinearLayoutManager(getApplicationContext());
            this.mRecyclerViewOnline.setLayoutManager(mLayoutManager2);
            this.mRecyclerViewOnline.setItemAnimator(new DefaultItemAnimator());

            setupAutoScroll();
            this.mRecyclerViewChats.setAdapter(mChatAdapter);
            this.mRecyclerViewOnline.setAdapter(mOnlineAdapter);
            setupListView();

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
                                currentUser.getDisplayName(),
                                msg, unixTime);
                        GameActivity.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                GameActivity.this.mChatAdapter.addMessage(chatMsg);
                            }
                        });

                        JSONObject json = new JSONObject();
                        try {
                            json.put(Constants.JSON_DISPLAY_NAME, currentUser.getDisplayName());
                            json.put(Constants.JSON_USER, currentUser.getEmail());
                            json.put(Constants.JSON_TIME, unixTime);
                            json.put(Constants.JSON_MSG, msg);

                            PubnubService.publish(GameActivity.this.getChannel(), json, Constants.NEW_MSG_ACTION);

                        } catch (Exception e) {
                            Toast.makeText(getApplicationContext(), getString(R.string.msg_error), Toast.LENGTH_SHORT);
                            e.printStackTrace();
                        }

                    }
                }
            });

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        getSupportActionBar().setTitle("vs " + rivalName);
                    } catch (Exception e) {}
                }
            });

        } else {
            setContentView(R.layout.activity_game_pc);
            layout = (RelativeLayout)findViewById(R.id.game_layout);

            biw.setBoardCallback(new BoardCallback() {
                @Override
                public void onMoveComplete(Move move) {
                    mplayer[1].play();
                }
                @Override
                public void onCheck(PieceColor color) {
                    if (color == biw.getMainColor()) {
                        Toast.makeText(getApplicationContext(), getString(R.string.check), Toast.LENGTH_SHORT).show();
                    }
                }
                @Override
                public void onGameOver(PieceColor color) {
                    int icon = android.R.drawable.star_big_on;
                    String text = getString(R.string.you_win);
                    String desc = getString(R.string.you_win_desc);
                    if (color == biw.getMainColor()) {
                        icon = android.R.drawable.ic_delete;
                        text = getString(R.string.you_lost);
                        desc = getString(R.string.you_lost_desc);
                    } else {
                        mplayer[2].play();
                    }

                    AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
                    builder.setTitle(text).setMessage(desc).setIcon(icon)
                            .setNeutralButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
            });

            biw.setMainColor(PieceColor.WHITE);

            biw.setMode(Mode.HUMAN_COMPUTER);
        }

        layout.addView(biw, 0);
    }

    private void initializeOnline() {
        installFilters();

       /* mServiceIntent = new Intent(this, PubnubService.class);
        mServiceIntent.putExtra("username", this.username);
        mServiceIntent.putExtra("channel", this.channel);

        this.bindService(mServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        */
        mService = RoomActivity.getService();

        mService.subscribeWithPresence(this.channel, false);

        GameActivity.onGame = true;
    }

    public void installFilters() {
        receiver = new GameBroadcastReceiver();
        receiver.setActivity(this);

        IntentFilter filter = new IntentFilter(Constants.PUBNUB_ACTION);

        registerReceiver(receiver, filter);
    }

    /**
     * Setup the recyclerview to scroll to bottom anytime it receives a message.
     */
    private void setupAutoScroll(){
        this.mChatAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
             //   super.onChanged();
             //   mRecyclerViewChats.smoothScrollToPosition(mChatAdapter.getItemCount());
            }
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                mRecyclerViewChats.smoothScrollToPosition(0);
                mplayer[0].play();
            }
        });
        /*
        this.mChatAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                mChatListView.setSelection(mChatAdapter.getCount() - 1);
                // mListView.smoothScrollToPosition(mChatAdapter.getCount()-1);
            }
        });*/
    }

    private void setupListView(){
        mOnlineAdapter.setOnItemClickListener(new OnlineAdapter.ClickListener() {
            @Override
            public void onItemClick(int position, View view) {

            }
        });
    }

    @Override
    protected void onDestroy() {
        /*if (mBound) {
            unbindService(mServiceConnection);
            mBound = false;
        }*/

        if (mService != null)
            mService.unsubscribe(getChannel());
        if (receiver != null)
            this.unregisterReceiver(receiver);

        super.onDestroy();


        GameActivity.onGame = false;
    }

    @Override
    public void onBackPressed() {
        if (!biw.isGameOver()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
            builder.setTitle(getString(R.string.quit_game))
                .setMessage(getString(R.string.quit_game_desc))
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(getString(R.string.accept), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish();
                    }
                })
                .setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                });
            AlertDialog dialog = builder.create();
            dialog.show();
        } else {
            super.onBackPressed();
        }
    }

    public String getChannel() { return channel; }

    public String getUsername() {
        return username;
    }

    public String getRival() {
        return rival;
    }

    static public int fix(int a)
    {
        return Math.abs(a - 7);
    }

    static String generateChannelName(String ch1, String ch2) {
        String channelName = null;
        if (ch1.compareTo(ch2) > 0) {
            channelName = ch1 + "_" + ch2;
        } else {
            channelName = ch2 + "_" + ch1;
        }
        channelName = channelName.replaceAll("[^a-zA-Z0-9_]", "");

        return channelName;
    }

    void updateRating() {
        AlertDialog.Builder builder = new AlertDialog.Builder(GameActivity.this);
        builder.setTitle(getString(R.string.you_win)).setMessage(R.string.you_win_desc_online).setIcon(android.R.drawable.star_big_on)
                .setNeutralButton(R.string.you_win_desc, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                     //   finish();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();

        Score.updateDB(25, new Handler() {
            public void handleMessage(Message msg) {
                Bundle bundle = msg.getData();
                final int score = bundle.getInt("score");
                Score.setScore(GameActivity.this, score);

                GameActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update state
                        GameActivity.this.mService.setStateLogin(GameActivity.this.getChannel());
                    }
                });

            }
        });
    }

    static public class GameBroadcastReceiver extends BroadcastReceiver {
        static GameActivity activity;

        public void setActivity(GameActivity activity) {
            this.activity = activity;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getStringExtra("action");
            if (action.equals(Constants.NEW_MSG_ACTION)) {
                // new message action
                final ChatMessage chatMsg = (ChatMessage)intent.getSerializableExtra("chatMsg");
                this.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GameBroadcastReceiver.this.activity.mChatAdapter.addMessage(chatMsg);
                    }
                });

            } else if (action.equals(Constants.MOVE_ACTION)) {
                final String username = intent.getStringExtra(Constants.JSON_USER);

                if (!username.equals(this.activity.getRival())) return;

                final String move = intent.getStringExtra(Constants.MOVE);

                String pattern = "^([0-9]+)x([0-9]+):([0-9]+)x([0-9]+)";
                Pattern r = Pattern.compile(pattern);
                final Matcher match = r.matcher(move);
                if (!match.matches()) return;

                this.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        int acol = fix(Integer.parseInt(match.group(1)));
                        int arow = fix(Integer.parseInt(match.group(2)));
                        int bcol = fix(Integer.parseInt(match.group(3)));
                        int brow = fix(Integer.parseInt(match.group(4)));
                        Piece pc;
                        if ((pc = GameBroadcastReceiver.this.activity.biw.getPiece(acol, arow)) != null)
                        {
                            if (pc.getColor() != GameBroadcastReceiver.this.activity.biw.getMainColor()) {
                                pc.move(bcol, brow);
                                GameBroadcastReceiver.this.activity.biw.invalidate();

                                Move move = new Move(arow*8+acol, brow*8+bcol, 0, -1);
                                GameBroadcastReceiver.this.activity.biw.onMove(move);
                            }
                        }
                    }
                });
            } else if (action.equals(Constants.PRESENCE_ACTION)) {
                final String user = intent.getStringExtra("user");
                JSONObject data = null;
                try {
                    String sdata = intent.getStringExtra("_data");
                    data = new JSONObject(sdata);
                } catch (Exception e) {
                    return;
                }

                final String _action = intent.getStringExtra("_action");
                final JSONObject _data = data;

                if (_action.equals("timeout") && user.equals(GameBroadcastReceiver.this.activity.getRival())) {
                    GameBroadcastReceiver.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GameBroadcastReceiver.this.activity.getApplicationContext(), GameBroadcastReceiver.this.activity.getString(R.string.connection_error), Toast.LENGTH_LONG).show();
                            GameBroadcastReceiver.this.activity.finish();
                        }
                    });
                } else if (_action.equals("leave") && user.equals(GameBroadcastReceiver.this.activity.getRival()) && !GameBroadcastReceiver.this.activity.biw.isGameOver()) {
                    GameBroadcastReceiver.this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(GameBroadcastReceiver.this.activity.getApplicationContext(), GameBroadcastReceiver.this.activity.getString(R.string.win_default), Toast.LENGTH_LONG).show();
                            GameBroadcastReceiver.this.activity.biw.onGameOver((GameBroadcastReceiver.this.activity.biw.getMainColor() == PieceColor.BLACK ? PieceColor.WHITE : PieceColor.BLACK));
                        }
                    });
                }

                this.activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        GameBroadcastReceiver.this.activity.mChatAdapter.userPresence(user, _action);
                        GameBroadcastReceiver.this.activity.mOnlineAdapter.updateAction(user, _action, _data);

                        try {
                            int occ = GameBroadcastReceiver.this.activity.mOnlineAdapter.getItemCount();
                          //  GameBroadcastReceiver.this.activity.getSupportActionBar().setTitle(GameBroadcastReceiver.this.activity.getChannel() + " (" + occ + ")");
                        }catch (Exception e) {
                        }
                    }
                });

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

                        if (uuid.equals(this.activity.currentUser.getEmail())) continue;

                        String displayName = null;
                        try {
                            JSONObject state = jobj.getJSONObject("state");
                            displayName = state.getString("displayName");
                        } catch (JSONException e) {

                        }

                        usersOnline.add(uuid);
                        listUsers.add(new User(uuid, displayName));
                    }

                    final int occ = listUsers.size();

                    this.activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            GameBroadcastReceiver.this.activity.mChatAdapter.setOnlineNow(usersOnline);
                            GameBroadcastReceiver.this.activity.mOnlineAdapter.setUsers(listUsers);
                            /*try {
                                GameBroadcastReceiver.this.activity.getSupportActionBar().setTitle(GameBroadcastReceiver.this.activity.getChannel() + " (" + occ + ")");
                            }catch (Exception e) {
                            }*/
                        }
                    });

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

/*
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
    };*/

}
