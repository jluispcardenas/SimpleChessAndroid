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

import android.content.Intent;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;


import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import jcardenas.com.chess.R;
import jcardenas.com.chess.adapters.ChannelsAdapter;
import jcardenas.com.chess.models.Channel;


public class ChannelsActivity extends BaseActivity {
    final static String TAG = ChannelsActivity.class.getSimpleName();

    private DatabaseReference mRef;
    private ValueEventListener mConnectedListener;
    private ChannelsAdapter mChannelsAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkAuth()) return;

        setContentView(R.layout.activity_channels);

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        mRef = database.getReference("channels");

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    @Override
    public void onStart() {
        super.onStart();

        try {
            String newTitle = (String)getString(R.string.room_availables) + " - " + getString(R.string.app_name);
            getSupportActionBar().setTitle(newTitle);
        } catch (Exception e) {

        }

        final ListView listView = (ListView)findViewById(R.id.listview);

        mChannelsAdapter = new ChannelsAdapter(mRef.limitToFirst(20), this, R.layout.channel_list);

        listView.setAdapter(mChannelsAdapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapter, View view, int position, long l) {
                Channel item = (Channel)adapter.getItemAtPosition(position);
                Intent intent = new Intent(ChannelsActivity.this, RoomActivity.class);
                intent.putExtra("channel", item.getTitle());

                startActivity(intent);
            }

        });

        mChannelsAdapter.registerDataSetObserver(new DataSetObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                listView.setSelection(mChannelsAdapter.getCount() - 1);
            }
        });

        mConnectedListener = mRef.getRoot().child(".info/connected").addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                boolean connected = (Boolean) dataSnapshot.getValue();
                if (connected) {
                 //   Toast.makeText(ChannelsActivity.this, "Connected to Firebase", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(ChannelsActivity.this, getString(R.string.connect), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // No-op
            }
        });
    }

}
