package com.murrayc.galaxyzoo.app.provider.client;

import android.content.Context;
import android.util.Base64;

import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.Utils;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by murrayc on 10/10/14.
 */
public class ZooniverseClient {
    private final Context mContext;
    private final String mServerBaseUri;

    //This is an attempt to reduce the amount of Network and Disk IO
    //that the system does, because even when using a Thread (with Thread.MIN_PRIORITY) instead of
    //AsyncTask, the UI is non responsive during this work.
    //For instance, buttons appear to be pressed, but their clicked listeners are not called.
    private static final int MAXIMUM_DOWNLOAD_ITEMS = 10;

    public ZooniverseClient(final Context context, final String serverBaseUri) {
        mContext = context;
        mServerBaseUri = serverBaseUri;
    }

    private String getQueryMoreItemsUri() {
        /**
         * REST uri for querying items.
         * Like, the Galaxy-Zoo website's code, this hard-codes the Group ID for the Sloan survey:
         */
        return mServerBaseUri + "groups/50251c3b516bcb6ecb000002/subjects?limit="; //Should have a number, such as 5, appended.
    }

    private String getPostUploadUri() {
        return mServerBaseUri + "workflows/50251c3b516bcb6ecb000002/classifications";
    }

    private String getLoginUri() {
        return mServerBaseUri + "login";
    }

    public LoginUtils.LoginResult loginSync(final String username, final String password) {
        final HttpURLConnection conn = HttpUtils.openConnection(getLoginUri());
        if (conn == null) {
            return null;
        }

        final List<NameValuePair> nameValuePairs = new ArrayList<>();
        nameValuePairs.add(new BasicNameValuePair("username", username));
        nameValuePairs.add(new BasicNameValuePair("password", password));

        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);

            if (!writeParamsToHttpPost(conn, nameValuePairs)) {
                return null;
            }

            conn.connect();
        } catch (final IOException e) {
            Log.error("loginSync(): exception during HTTP connection", e);

            return null;
        }

        //Get the response:
        InputStream in = null;
        try {
            in = conn.getInputStream();
            if (conn.getResponseCode() != HttpURLConnection.HTTP_OK) {
                Log.error("loginSync(): response code: " + conn.getResponseCode());
                return null;
            }

            return LoginUtils.parseLoginResponseContent(in);
        } catch (final IOException e) {
            Log.error("loginSync(): exception during HTTP connection", e);

            return null;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.error("loginSync(): exception while closing in", e);
                }
            }
        }
    }

    private static boolean writeParamsToHttpPost(final HttpURLConnection conn, final List<NameValuePair> nameValuePairs) {
        OutputStream out = null;
        try {
            out = conn.getOutputStream();

            BufferedWriter writer = null;
            try {
                writer = new BufferedWriter(
                        new OutputStreamWriter(out, Utils.STRING_ENCODING));
                writer.write(getPostDataBytes(nameValuePairs));
                writer.flush();
            } catch (final IOException e) {
                Log.error("writeParamsToHttpPost(): Exception: ", e);
                return false;
            } finally {
                if (writer != null) {
                    writer.close();
                }
            }
        } catch (final IOException e) {
            Log.error("writeParamsToHttpPost(): Exception: ", e);
            return false;
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (final IOException e) {
                    Log.error("writeParamsToHttpPost(): Exception while closing out", e);
                }
            }
        }

        return true;
    }

    private static String getPostDataBytes(final List<NameValuePair> nameValuePairs) {
        final StringBuilder result = new StringBuilder();
        boolean first = true;

        for (final NameValuePair pair : nameValuePairs) {
            if (first) {
                first = false;
            } else {
                result.append("&");
            }

            try {
                result.append(URLEncoder.encode(pair.getName(), Utils.STRING_ENCODING));
                result.append("=");
                result.append(URLEncoder.encode(pair.getValue(), Utils.STRING_ENCODING));
            } catch (UnsupportedEncodingException e) {
                Log.error("getPostDataBytes(): Exception", e);
                return null;
            }
        }

        return result.toString();
    }

    private String generateAuthorizationHeader(final String authName, final String authApiKey) {
        //See the similar code in Zooniverse's user.coffee source code:
        //https://github.com/zooniverse/Zooniverse/blob/master/src/models/user.coffee#L49
        final String str = authName + ":" + authApiKey;
        byte[] asBytes = null;
        try {
            asBytes = str.getBytes(Utils.STRING_ENCODING);
        } catch (UnsupportedEncodingException e) {
            Log.error("generateAuthorizationHeader(): String.getBytes() failed", e);
            return null;
        }

        return "Basic " + Base64.encodeToString(asBytes, Base64.DEFAULT);
    }

    /** This will not always provide as many items as requested.
     *
     * @param count
     * @return
     */
    public List<Subject> requestMoreItemsSync(int count) {
        throwIfNoNetwork();

        //Avoid suddenly doing too much network and disk IO
        //as we download too many images.
        if (count > MAXIMUM_DOWNLOAD_ITEMS) {
            count = MAXIMUM_DOWNLOAD_ITEMS;
        }

        //TODO: Can we use Java 7's try-with-resources to automatically close()
        //the InputStream even though none of the code here should throw an
        //exception?
        //Not that the server often won't give us as many as we want,
        //so a subsequent request might be needed to get all of them.
        //We don't need to check how many we get, and ask again,
        //because we already just call queueRegularTasks() again if necessary.
        final InputStream in = HttpUtils.httpGetRequest(getQueryUri(count));
        if (in == null) {
            return null;
        }

        final List<Subject> result = MoreItemsJsonParser.parseMoreItemsResponseContent(in);
        try {
            in.close();
        } catch (IOException e) {
            Log.error("requestMoreItemsSync(): Can't close input stream", e);
        }

        return result;
    }

    private String getQueryUri(final int count) {
        return getQueryMoreItemsUri() + Integer.toString(count); //TODO: Is Integer.toString() locale-dependent?
    }

    private void throwIfNoNetwork() {
        HttpUtils.throwIfNoNetwork(getContext());
    }

    private Context getContext() {
        return mContext;
    }

    public boolean uploadClassificationSync(final String authName, final String authApiKey, final List<NameValuePair> nameValuePairs) {
        final HttpURLConnection conn = HttpUtils.openConnection(getPostUploadUri());
        if (conn == null) {
            return false;
        }

        try {
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setDoInput(true);
        } catch (final IOException e) {
            Log.error("uploadClassificationSync: exception during HTTP connection", e);

            return false;
        }

        //Add the authentication details to the headers;
        //Be careful: The server still returns OK_CREATED even if we provide the wrong Authorization here.
        //There doesn't seem to be any way to know if it's correct other than checking your recent
        //classifications in your profile.
        if ((authName != null) && (authApiKey != null)) {
            conn.setRequestProperty("Authorization", generateAuthorizationHeader(authName, authApiKey));
        }

        if (!writeParamsToHttpPost(conn, nameValuePairs)) {
            return false;
        }

        //TODO: Is this necessary? conn.connect();

        //Get the response:
        InputStream in = null;
        try {
            in = conn.getInputStream();
            final int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_CREATED) {
                Log.error("uploadClassificationSync: Did not receive the 201 Created status code: " + conn.getResponseCode());
                return false;
            }

            return true;
        } catch (final IOException e) {
            Log.error("uploadClassificationSync: exception during HTTP connection", e);

            return false;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (final IOException e) {
                    Log.error("uploadClassificationSync: exception while closing in", e);
                }
            }
        }
    }

    /**
     * This class is meant to be immutable.
     * It only returns references to immutable Strings.
     */
    public final static class Subject {
        private final String mSubjectId;
        private final String mZooniverseId;
        private final String mLocationStandard;
        private final String mLocationThumbnail;
        private final String mLocationInverted;

        public Subject(final String subjectId, final String zooniverseId, final String locationStandard, final String locationThumbnail, final String locationInverted) {
            this.mSubjectId = subjectId;
            this.mZooniverseId = zooniverseId;
            this.mLocationStandard = locationStandard;
            this.mLocationThumbnail = locationThumbnail;
            this.mLocationInverted = locationInverted;
        }

        public String getSubjectId() {
            return mSubjectId;
        }

        public String getZooniverseId() {
            return mZooniverseId;
        }

        public String getLocationStandard() {
            return mLocationStandard;
        }

        public String getLocationThumbnail() {
            return mLocationThumbnail;
        }

        public String getLocationInverted() {
            return mLocationInverted;
        }

    }
}
