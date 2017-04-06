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
import android.media.AudioManager;
import android.media.SoundPool;

public class SoundPlayer {

        private SoundPool m_soundPool;
        private int m_soundId;
        private boolean m_isReady = false;
        static private boolean playSounds = true;

        public SoundPlayer(Context context, int resId) {
            m_soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
            m_soundId = m_soundPool.load(context, resId, 0);
            m_soundPool.setOnLoadCompleteListener(new SoundPool.OnLoadCompleteListener() {
                @Override
                public void onLoadComplete(SoundPool soundPool, int sampleId, int status) {
                    m_isReady = true;
                }
            });
        }

        static public void allowSounds(boolean allow) {
            playSounds = allow;
        }

        public boolean play() {
            if (playSounds && m_isReady) {
                if (m_soundPool.play(m_soundId, 1.0F, 1.0F, 0, 0, 1.0F) != 0) {
                    return true;
                }
            }
            return false;
        }

        public void release() {
            m_soundPool.release();
            m_soundPool = null;
        }
}

