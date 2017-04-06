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
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import jcardenas.com.chess.R;
import jcardenas.com.chess.utils.RoundedTransformation;
import jcardenas.com.chess.utils.Score;
import jcardenas.com.chess.models.User;

public class OnlineAdapter extends RecyclerView.Adapter<OnlineAdapter.MyViewHolder>{
    private List<User> values;
    private Set<String> onlineNow = new HashSet<String>();
    private static ClickListener clickListener;
    private Activity activity;

    public OnlineAdapter(List<User> values, Activity activity) {
        this.values = values;
        this.activity = activity;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener  {
        TextView user;
        ImageView photo;
        TextView desc;

        public MyViewHolder(View v) {
            super(v);

            user = (TextView) v.findViewById(R.id.user);
            photo = (ImageView) v.findViewById(R.id.photo);
            desc = (TextView) v.findViewById(R.id.desc);

            v.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            clickListener.onItemClick(getAdapterPosition(), view);
        }
    }

    public void setOnItemClickListener(ClickListener clickListener) {
        OnlineAdapter.clickListener = clickListener;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_row_users_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        User user  = this.values.get(position);

        holder.user.setText((user.getDisplayName() != null ? user.getDisplayName() : "Nuevo jugador"));
        holder.desc.setText(Score.getLevel(user.getScore()));

        Picasso.with(this.activity).load(user.getPhotoUrl()).transform(new RoundedTransformation(50, 0)).into(holder.photo);
    }

    @Override
    public int getItemCount() {
        return this.values.size();
    }

    public void addUser(User user){
        this.values.add(user);
        notifyDataSetChanged();
    }

    public void setUsers(List<User> users){
        this.values.clear();
        this.values.addAll(users);
        notifyDataSetChanged();
    }

    public void clearUsers(){
        this.values.clear();
        notifyDataSetChanged();
    }

    public void updateAction(String uuid, String action, JSONObject data) {
        if (action.equals("join")) {
         //   this.addUser(new User(uuid, ""));
        } else if (action.equals("leave") || action.equals("timeout")) {
            Iterator<User> it = this.values.iterator();
            while (it.hasNext()) {
                User user = it.next();
                if (user.getUsername().equals(uuid)) {
                    it.remove();
                    notifyDataSetChanged();
                    break;
                }
            }
        } else if (action.equals("state-change")) {
            Iterator<User> it = this.values.iterator();
            while (it.hasNext()) {
                User user = it.next();
                if (user.getUsername().equals(uuid)) {
                    boolean changed = fillUserObject(user, data);
                    if (changed) notifyDataSetChanged();

                    return;
                }
            }

            User user = new User(uuid, "");
            this.addUser(user);

            boolean changed = fillUserObject(user, data);
            if (changed)
                notifyDataSetChanged();

        }
    }

    private boolean fillUserObject(User user, JSONObject data) {
        boolean changed = false;

        String displayName;
        String photoUrl;
        int score = 0;
        try {
            if ((displayName = data.getString("displayName")) != null && data.getString("displayName").length() > 2 && !displayName.equals(user.getDisplayName())) {
                user.setDisplayName(displayName);
                changed = true;
            }
            if ((photoUrl = data.getString("photoUrl")) != null && photoUrl.length() > 10 && !photoUrl.equals(user.getPhotoUrl())) {
                user.setPhotoUrl(photoUrl);
                changed = true;
            }
            if ((score = data.getInt("score")) > 0 && score != user.getScore()) {
                user.setScore(score);
                changed = true;
            }
        } catch(JSONException e) {
            e.printStackTrace();
        }

        return changed;
    }

    public List<User> getValues() {
        return this.values;
    }

    public interface ClickListener {
        void onItemClick(int position, View v);
    }
}