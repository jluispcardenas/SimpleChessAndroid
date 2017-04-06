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
package jcardenas.com.chess.adapters;

import android.app.Activity;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.Query;
import com.squareup.picasso.Picasso;

import jcardenas.com.chess.models.Channel;
import jcardenas.com.chess.R;

public class ChannelsAdapter extends FirebaseListAdapter<Channel> {
    public Activity activity;

    public ChannelsAdapter(Query ref, Activity activity, int layout) {
        super(ref, Channel.class, layout, activity);
        this.activity = activity;

    }

    @Override
    protected void populateView(View view, Channel channel) {

        final String title = channel.getTitle();
        TextView titleText = (TextView) view.findViewById(R.id.title);
        titleText.setText(title);

        ImageView picture = (ImageView)view.findViewById(R.id.picture);
        Picasso.with(this.activity).load(channel.getPicture()).into(picture);

        ((TextView) view.findViewById(R.id.description)).setText(channel.getDescription());

    }
}