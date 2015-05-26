/*
 * Copyright (c) 2015 IBM Corp. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */

package com.cloudant.http;

import com.cloudant.mazha.json.JSONHelper;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Rhys Short on 15/05/15.
 *
 * Adds cookie authentication support to http requests.
 *
 * It does this by adding the cookie header for CouchDB
 * using request filtering pipeline in {@link HttpConnection}.
 *
 * If a response has a response code of 401, it will fetch a cookie from
 * the server using provided credentials and tell {@link HttpConnection} to reply
 * the request by setting {@link HttpConnectionFilterContext#replayRequest} property to true.
 *
 *
 */
public  class CookieFilter implements HttpConnectionRequestFilter, HttpConnectionResponseFilter {


    private final static Logger logger = Logger.getLogger(CookieFilter.class.getCanonicalName());
    final String cookieRequestBody;
    private String cookie = null;

    /**
     * Constructs a cookie filter.
     * @param username The username to use when getting the cookie
     * @param password The password to use when getting the cookie
     */
    public CookieFilter(String username, String password){
        Map<String,String> cookieRequestMap = new HashMap<String, String>();
        cookieRequestMap.put("name",username);
        cookieRequestMap.put("password",password);
        JSONHelper helper = new JSONHelper();
        cookieRequestBody = helper.toJson(cookieRequestMap);
    }


    @Override
    public HttpConnectionFilterContext filterRequest(HttpConnectionFilterContext context) {

        HttpURLConnection connection = context.connection.getConnection();

        if(cookie == null){
            cookie = getCookie(connection.getURL());
        }
        connection.setRequestProperty("Cookie",cookie);

        return context;

    }

    @Override
    public HttpConnectionFilterContext filterResponse(HttpConnectionFilterContext context) {
        HttpURLConnection connection = context.connection.getConnection();
        if (context.connection.getResponseCode() == 401) {
            //we need to get a new cookie
            cookie = getCookie(connection.getURL());
            //don't resend request, failed to get cookie
            if(cookie != null) {
                context.replayRequest = true;
                connection.setRequestProperty("Cookie", cookie);

                context = new HttpConnectionFilterContext(context);
            } else {
                context.replayRequest = false;
                logger.severe("Cookie is unavailable, cannot reply request");
            }
        }
        return context;

        }

     private String getCookie(URL url){

        try {
            URL sessionURL = new URL(String.format("%s://%s:%d/_session",
                    url.getProtocol(),
                    url.getHost(),
                    url.getPort()));

            HttpConnection conn = Http.POST(sessionURL, "application/json");
            conn.setRequestBody(cookieRequestBody.getBytes("UTF-8"));
            String cookieHeader = conn.execute().getConnection().getHeaderField("Set-Cookie");
            return cookieHeader.substring(0,cookieHeader.indexOf(";"));

        } catch (MalformedURLException e) {
            logger.log(Level.SEVERE,"Failed to create URL for _session endpoint",e);
        } catch (UnsupportedEncodingException e) {
            logger.log(Level.SEVERE, "Failed to encode cookieRequest body", e);
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to read cookie response header", e);
        }

        return null;
    }


}
