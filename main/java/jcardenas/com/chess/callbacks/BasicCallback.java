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
package jcardenas.com.chess.callbacks;

import com.pubnub.api.Callback;
import com.pubnub.api.PubnubError;

public class BasicCallback extends Callback {

    public BasicCallback(){
    }

    @Override
    public void successCallback(String channel, Object response) {
    }

    @Override
    public void connectCallback(String channel, Object message) {

    }

    @Override
    public void errorCallback(String channel, PubnubError error) {

    }
}