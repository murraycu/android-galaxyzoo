package com.murrayc.galaxyzoo.app.provider.rest;
//Based on: package com.finchframework.finch.rest;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.provider.ItemsContentProvider;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;

/**
 * Provides a runnable that uses an HttpClient to asynchronously load a given
 * URI.  After the network content is loaded, the task delegates handling of the
 * request to a ResponseHandler specialized to handle the given content.
 */
public class UriRequestTask implements Runnable {
    private HttpUriRequest mRequest;
    private ResponseHandler mHandler;
    private String handlerResult;

    protected Context mAppContext;

    private ItemsContentProvider mSiteProvider;
    private String mRequestTag;

    public UriRequestTask(HttpUriRequest request,
                          ResponseHandler handler, Context appContext) {
        this(null, null, request, handler, appContext);
    }

    public UriRequestTask(String requestTag,
                          ItemsContentProvider siteProvider,
                          HttpUriRequest request,
                          ResponseHandler handler, Context appContext) {
        mRequestTag = requestTag;
        mSiteProvider = siteProvider;
        mRequest = request;
        mHandler = handler;
        mAppContext = appContext;
    }

    /**
     * Carries out the request on the complete URI as indicated by the protocol,
     * host, and port contained in the configuration, and the URI supplied to
     * the constructor.
     */
    public void run() {
        HttpResponse response;

        try {
            response = execute(mRequest);
            handlerResult = (String)mHandler.handleResponse(response);
        } catch (IOException e) {
            Log.error("exception processing async request", e);
        } finally {
            /*
            if (mSiteProvider != null) {
                mSiteProvider.requestComplete(mRequestTag);
            }
            */
        }

        //This is null on success.
        //Otherwise it describes an error.
        //See our GalaxyZooResponseHandler.
        if(!TextUtils.isEmpty(handlerResult)) {
            Log.error("Error processing async request: ", handlerResult);
        }
    }

    private HttpResponse execute(HttpUriRequest mRequest) throws IOException {
        HttpClient client = new DefaultHttpClient();
        return client.execute(mRequest);
    }

    public Uri getUri() {
        return Uri.parse(mRequest.getURI().toString());
    }
}
