package com.cloudant.http;

/**
 * Created by tomblench on 30/03/15.
 */

/**
 A <code>HttpConnectionFilter</code> can either be configured as a Request Filter or a Response Filter:

 A Request Filter is run before the request is made to the server. It can use headers to add support
 for other authentication methods, for example cookie authentication. See
 {@link CookieFilter#filterRequest(HttpConnectionFilterContext)} for an example.


 Filters are executed in a pipeline and modify the context in a serial fashion.
 */


public interface HttpConnectionRequestFilter {

    /**
     * Filter the request or response
     * @param context Input context
     * @return Output context
     */
    HttpConnectionFilterContext filterRequest(HttpConnectionFilterContext context);

}
