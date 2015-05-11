package com.cloudant.http;

/**
 * Created by tomblench on 30/03/15.
 */
public class HttpConnectionFilterContext {

    public boolean replayRequest;
    public final HttpConnection connection;

    /**
     * Constructor
     * @param connection HttpConnection
     */
    public HttpConnectionFilterContext (HttpConnection connection) {
        this.replayRequest = false;
        this.connection = connection;
    }

    /**
     * Shallow copy constructor
     * @param other Context to copy
     */
    public HttpConnectionFilterContext (HttpConnectionFilterContext other) {
        this.replayRequest = other.replayRequest;
        this.connection = other.connection;

    }

}
