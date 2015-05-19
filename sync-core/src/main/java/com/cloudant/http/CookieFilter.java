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

/**
 * Created by Rhys Short on 15/05/15.
 */
public  class CookieFilter implements HttpConnectionRequestFilter, HttpConnectionResponseFilter {


    final String cookieRequestBody;
    private String cookie = null;

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
            context.replayRequest = true;
            connection.setRequestProperty("Cookie", cookie);

            context = new HttpConnectionFilterContext(context);
        }
        return context;

        }

     String getCookie(URL url){

        try {
            StringBuilder sb = new StringBuilder();
            sb.append(url.getProtocol());
            sb.append("://");
            sb.append(url.getHost());
            sb.append(":");
            sb.append(url.getPort());
            sb.append("/_session");
            URL sessionURL = new URL(sb.toString());
            HttpConnection conn = Http.POST(sessionURL, "application/json");
            conn.responseFilters.clear(); //make sure they get cleared
            conn.requestFilters.clear();
            conn.setRequestBody(cookieRequestBody.getBytes("UTF-8"));
            String cookieHeader = conn.execute().getConnection().getHeaderField("Set-Cookie");
            return cookieHeader.substring(0,cookieHeader.indexOf(";"));



        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }


}
