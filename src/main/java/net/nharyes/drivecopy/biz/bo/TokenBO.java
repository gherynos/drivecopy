/*
 * Copyright 2012 Luca Zanconato
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.nharyes.drivecopy.biz.bo;

public class TokenBO implements BusinessObject {

    private String clientId;

    private String clientSecret;

    private String accessToken;

    private String refreshToken;

    public TokenBO() {

    }

    public TokenBO(String clientId, String clientSecret, String accessToken, String refreshToken) {

        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    public String getClientId() {

        return clientId;
    }

    public String getClientSecret() {

        return clientSecret;
    }

    public String getAccessToken() {

        return accessToken;
    }

    public String getRefreshToken() {

        return refreshToken;
    }
}
