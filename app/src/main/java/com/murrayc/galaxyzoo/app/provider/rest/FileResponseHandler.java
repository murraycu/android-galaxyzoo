package com.murrayc.galaxyzoo.app.provider.rest;

import android.net.Uri;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.File;
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

    public String handleResponse(HttpResponse response)  throws IOException {
        //Write the content to the file:
        final InputStream urlStream = response.getEntity().getContent();
        final FileOutputStream fout =
                new FileOutputStream(mCacheFileUri);
        byte[] bytes = new byte[256];
        int r;
        do {
            r = urlStream.read(bytes);
            if (r >= 0) {
                fout.write(bytes, 0, r);
            }
        } while (r >= 0);

        urlStream.close();
        fout.close();

        return null; //TODO?
    }
}
