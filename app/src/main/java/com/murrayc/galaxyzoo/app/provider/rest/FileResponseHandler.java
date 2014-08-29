package com.murrayc.galaxyzoo.app.provider.rest;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.GZIPInputStream;

/**
 * Writes data from urls into a local file cache that can be referenced by a
 * database ID.
 */
public class FileResponseHandler implements ResponseHandler {
    private final String mCacheFileUri;

    public FileResponseHandler(final String cacheFileUri) {
        mCacheFileUri = cacheFileUri;
    }

    public Boolean handleResponse(HttpResponse response)  throws IOException {
        InputStream urlStream = response.getEntity().getContent();

        //Ungzip it if necessary:
        //For instance, HTML and CSS files may often be gzipped.
        final Header contentEncoding = response.getFirstHeader("Content-Encoding");
        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
            urlStream = new GZIPInputStream(urlStream);
        }

        //Write the content to the file:
        final FileOutputStream fout =
                new FileOutputStream(mCacheFileUri);

        // TODO: Find a way to use writeTo(), instead of looping ourselves,
        // while also having optional ungzipping?
        //response.getEntity().writeTo(fout);

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

        return true; //TODO?
    }
}
