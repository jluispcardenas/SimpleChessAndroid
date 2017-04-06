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
package jcardenas.com.chess.models;


import java.io.Serializable;

public class User implements Serializable {
    private String username;
    private String displayName;
    private String photoUrl;
    private int score;

    public User(String username, String displayName){
        this(username, displayName, null, 0);
    }

    public User(String username, String displayName, String photoUrl, int score) {
        this.username  = username;
        this.displayName = displayName;
        this.photoUrl = photoUrl;
        this.score = score;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public void setScore(int score) { this.score = score; }

    public String getPhotoUrl() { return photoUrl; }

    public int getScore() { return score; }
}