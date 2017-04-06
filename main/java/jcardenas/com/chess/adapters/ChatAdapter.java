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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jcardenas.com.chess.models.ChatMessage;
import jcardenas.com.chess.R;


public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MyViewHolder> {

    private List<ChatMessage> values;
    private Set<String> onlineNow = new HashSet<String>();

    public ChatAdapter(List<ChatMessage> values) {
        this.values = values;
    }

    public static class MyViewHolder extends RecyclerView.ViewHolder {
        TextView user;
        TextView message;
        TextView timeStamp;
        View presence;
        ChatMessage chatMsg;

        public MyViewHolder(View v) {
            super(v);

            user = (TextView) v.findViewById(R.id.user);
            message = (TextView) v.findViewById(R.id.message);
            timeStamp = (TextView) v.findViewById(R.id.time);
            presence = v.findViewById(R.id.presence);

            ScaleAnimation anim = new ScaleAnimation(0.0f, 1.0f, 0.0f, 1.0f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
            anim.setDuration(500);
            v.startAnimation(anim);

        }
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.room_row_layout, parent, false);

        return new MyViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        ChatMessage chatMsg = this.values.get(position);

        holder.user.setText(chatMsg.getDisplayName());
        holder.message.setText(chatMsg.getMessage());
        holder.timeStamp.setText(formatTimeStamp(chatMsg.getTimeStamp()));
        holder.chatMsg = chatMsg;
        holder.presence.setBackgroundDrawable( // If online show the green presence dot
                this.onlineNow.contains(chatMsg.getUsername())
                        ? null//context.getResources().getDrawable(R.drawable.online_circle)
                        : null);
    }

    @Override
    public int getItemCount() {
        return values.size();
    }

    /**
     * Method to add a single message and update the listview.
     * @param chatMsg Message to be added
     */
    public void addMessage(ChatMessage chatMsg){
        //this.values.add(chatMsg);
        this.values.add(0, chatMsg);
        this.notifyItemInserted(0);

      //  notifyDataSetChanged();
    }

    public void setMessages(List<ChatMessage> chatMsgs){
        this.values.clear();
        this.values.addAll(chatMsgs);
        notifyDataSetChanged();
    }

    public void userPresence(String user, String action){
        boolean isOnline = action.equals("join") || action.equals("state-change");
        if (!isOnline && this.onlineNow.contains(user))
            this.onlineNow.remove(user);
        else if (isOnline && !this.onlineNow.contains(user))
            this.onlineNow.add(user);

        notifyDataSetChanged();
    }

    public void setOnlineNow(Set<String> onlineNow){
        this.onlineNow = onlineNow;
        notifyDataSetChanged();
    }


    public static String formatTimeStamp(long timeStamp){
        SimpleDateFormat formatter = new SimpleDateFormat("hh:mm a");

        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timeStamp);
        return formatter.format(calendar.getTime());
    }

    public void clearMessages(){
        this.values.clear();
        notifyDataSetChanged();
    }

    public List<ChatMessage> getValues() {
        return this.values;
    }
}