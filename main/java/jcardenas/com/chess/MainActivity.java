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
package jcardenas.com.chess;



import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.squareup.picasso.Picasso;

import jcardenas.com.chess.activities.BaseActivity;
import jcardenas.com.chess.activities.ChannelsActivity;
import jcardenas.com.chess.activities.GameActivity;
import jcardenas.com.chess.activities.SignedInActivity;
import jcardenas.com.chess.utils.Constants;
import jcardenas.com.chess.utils.RoundedTransformation;
import jcardenas.com.chess.utils.Score;


public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!checkAuth()) return;

        setContentView(R.layout.activity_main);

        FirebaseAuth auth = FirebaseAuth.getInstance();
        FirebaseUser current = auth.getCurrentUser();

        TextView txt = (TextView)findViewById(R.id.welcome);
        txt.setText(current.getDisplayName());

        ImageView picture = (ImageView)findViewById(R.id.picture);
        if (current.getPhotoUrl() != null)
            Picasso.with(this).load(current.getPhotoUrl()).transform(new RoundedTransformation(30, 0)).into(picture);

        // firts time?, update score from db
        int score = Score.getScore(this);
        if (score == 0) {
            Score.retrieveDB(this, new Handler() {
                public void handleMessage(Message msg) {
                    Bundle bundle = msg.getData();
                    int score = bundle.getInt("score");

                    TextView level = (TextView)findViewById(R.id.level);
                    level.setText(Score.getLevel(score));
                }
            });
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

    }

    public void newGame(View v) {
        if (v.getId() == R.id.btn_online) {
            startActivity(createIntent(this, ChannelsActivity.class));
        } else if (v.getId() == R.id.btn_phone) {
            Intent intent = new Intent(this, GameActivity.class);
            intent.putExtra(Constants.MODE, "phone");

            startActivity(intent);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        int score = Score.getScore(this);
        TextView level = (TextView)findViewById(R.id.level);
        level.setText(Score.getLevel(score));
    }

}
