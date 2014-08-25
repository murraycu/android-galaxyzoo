package com.murrayc.galaxyzoo.app.provider.rest;

import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Writes data from urls into a local file cache that can be referenced by a
 * database ID.
 */
public class FileResponseHandler implements ResponseHandler {
    private String mCacheFileUri;

    public FileResponseHandler(final String cacheFileUri) {
        mCacheFileUri = cacheFileUri;
    }

    public Boolean handleResponse(HttpResponse response)  throws IOException {
        //Write the content to the file:
        final FileOutputStream fout =
                new FileOutputStream(mCacheFileUri);
        response.getEntity().writeTo(fout);
        fout.close();

        return true; //TODO?
    }
}
