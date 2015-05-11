package com.cloudant.http;

/**
 * Created by tomblench on 30/03/15.
 */

/**
 A <code>HttpConnectionFilter</code> can either be configured as a Request Filter or a Response Filter:

 A Request Filter is run before the request is made to the server.

 A Response Filter is run after the response is obtained from the
 server but before the output stream is returned to the original client. The Response
 Filter enables two main behaviours:

 - Modifying the response for every request

 - Replaying a (potentially modified) request by reacting to the
 response. For example, obtaining a cookie on receipt of a 401
 response code, modifying the "Cookie" header of the original
 request, then setting replayRequest to <code>true</code> to replay the request
 with the new "Cookie" header.

 Filters are executed in a pipeline and modify the context in a serial fashion.
 */


public interface HttpConnectionFilter {

    /**
     * Filter the request or response
     * @param context Input context
     * @return Output context
     */
    HttpConnectionFilterContext filter(HttpConnectionFilterContext context);

}
