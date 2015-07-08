package com.murrayc.galaxyzoo.app.provider.client;

import com.android.volley.Response;
import com.android.volley.toolbox.StringRequest;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by murrayc on 11/24/14.
 */
class ZooStringRequest extends StringRequest {
    public ZooStringRequest(final int method, final String url, final Response.Listener<String> listener,
                            final Response.ErrorListener errorListener) {
        super(method, url, listener, errorListener);
    }

    @Override
    public Map<String, String> getHeaders(){
        final Map<String, String> headers = new HashMap<>();
        headers.put(HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT, HttpUtils.getUserAgent());

        //This is required by the newer API, so let's start asking for it already:
        //See http://docs.panoptes.apiary.io/#introduction/authentication
        headers.put(HttpUtils.HTTP_REQUEST_HEADER_PARAM_ACCEPT, HttpUtils.CONTENT_TYPE_JSON);

        return headers;
    }
}
