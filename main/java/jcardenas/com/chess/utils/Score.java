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
package jcardenas.com.chess.utils;


import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class Score {
    static FirebaseDatabase database = FirebaseDatabase.getInstance();
    static DatabaseReference mRef = database.getReference("users");
    static FirebaseAuth auth = FirebaseAuth.getInstance();

    static int score = 0;

    static public int getScore(Context context) {
        if (auth.getCurrentUser() == null) return 0;

        if (score == 0) {
            SharedPreferences prefs = context.getSharedPreferences(Constants.CHAT_PREFS, Context.MODE_PRIVATE);
            score = prefs.getInt(Constants.SCORE, 0);
        }

        return score;
    }

    static public void setScore(Context context, int score) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.CHAT_PREFS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.SCORE, score);
        editor.apply();

        Score.score = score;
    }

    static public String getLevel(int score) {
        String str = "Lvl ?";
        int inc = 100;
        for (int i = 1; i < 100; i++) {
            if (score < inc) {
                str = "Lvl " + i + " (+" + score + ")";
                break;
            }
            inc *= 2;
        }
        return str;
    }

    static public void retrieveDB(Context _context, final Handler handler) {
        final Context context = _context;

        mRef.child(auth.getCurrentUser().getUid()).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        Object value = dataSnapshot.child("score").getValue();
                        Bundle data = new Bundle();
                        if (value != null) {
                            score = Integer.parseInt(value.toString());
                        } else {
                            // usuario no existente
                            Map<String, Integer> userMap = new HashMap<String, Integer>();
                            userMap.put("score", 0);
                            Map<String, Object> users = new HashMap<String, Object>();
                            users.put(auth.getCurrentUser().getUid(), userMap);
                            mRef.updateChildren(users);

                            score = 0;
                        }

                        setScore(context, score);
                        data.putInt("score", score);

                        Message msg = new Message();
                        msg.setData(data);
                        handler.sendMessage(msg);
                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {
                    }
                });
    }

    static public void updateDB(final int increment, final Handler handler) {
        mRef.child(auth.getCurrentUser().getUid()).child("score").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData currentData) {
                if(currentData.getValue() == null) {
                    currentData.setValue(0);
                } else {
                    currentData.setValue((Long) currentData.getValue() + increment);
                }
                return Transaction.success(currentData); //we can also abort by calling Transaction.abort()
            }

            @Override
            public void onComplete(DatabaseError databaseError, boolean b, DataSnapshot dataSnapshot) {
                Bundle data = new Bundle();
                data.putInt("score", Integer.parseInt(dataSnapshot.getValue().toString()));
                Message msg = new Message();
                msg.setData(data);
                handler.sendMessage(msg);
            }
        });
    }
}
