/*
 * Copyright (C) 2014 Murray Cumming
 *
 * This file is part of android-galaxyzoo
 *
 * android-galaxyzoo is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * android-galaxyzoo is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with android-galaxyzoo.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.murrayc.galaxyzoo.app.provider.client;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.murrayc.galaxyzoo.app.Log;
import com.murrayc.galaxyzoo.app.LoginUtils;
import com.murrayc.galaxyzoo.app.provider.Config;
import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.security.SecureRandom;
import java.util.List;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by murrayc on 10/10/14.
 */
public class ZooniverseClient {
    private final Context mContext;
    private final String mServerBaseUri;
    private final ZooniverseSubjectsService mRetrofitService;

    public ZooniverseClient(final Context context, final String serverBaseUri) {
        mContext = context;
        mServerBaseUri = serverBaseUri;

        final Retrofit retrofit = createRetrofit(mServerBaseUri);
        mRetrofitService = retrofit.create(ZooniverseSubjectsService.class);
    }

    private static Retrofit createRetrofit(final String baseUrl) {
        final Gson gson = createGson();
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
    }

    @NonNull
    public static Gson createGson() {
        // Register our custom GSON deserializer for use by Retrofit.
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Subject.class, new SubjectDeserializer());
        return gsonBuilder.create();
    }

    /** Return a group ID selected at random.
     *
     * @return
     */
    private static String getGroupIdForNextQuery() {

        //Get a list of only the groups that should be used for new queries.
        //TODO: Avoid doing this each time?
        final List<String> groupIds = Config.getSubjectGroupsToUseForNewQueries();
        if(groupIds.size() == 1) {
            return groupIds.get(0);
        }

        final Object[] values = groupIds.toArray();
        final int idx = new SecureRandom().nextInt(values.length);
        return (String)values[idx];
    }

    private String getPostUploadUri(final String groupId) {
        return mServerBaseUri + "workflows/" + groupId + "/classifications";
    }

    private String getLoginUri() {
        return mServerBaseUri + "login";
    }

    @Nullable
    public LoginUtils.LoginResult loginSync(final String username, final String password) throws LoginException {
        HttpUtils.throwIfNoNetwork(getContext(),
                false); //Ignore the wifi-only setting because this will be when the user is explicitly requesting a login.

        final RequestBody formBody = new FormBody.Builder()
                .add("username", username)
                .add("password", password)
                .build();
        final Request request = new Request.Builder()
                .url(getLoginUri())
                .post(formBody)
                .build();

        final okhttp3.Call call = HttpUtils.getHttpClient().newCall(request);

        okhttp3.Response response = null;
        try {
            response = call.execute();
        } catch (final IOException e) {
            Log.error("loginSync(): exception during request.", e);
            return null;
        }

        if (response == null) {
            Log.error("loginSync(): response is null.");
            return null;
        }

        if (!response.isSuccessful()) {
            Log.error("loginSync(): response not successful.");
            response.close();
            return null;
        }

        //Get the response:
        try {
            final LoginUtils.LoginResult result = LoginUtils.parseLoginResponseContent(response.body().byteStream());
            response.close();
            return result;
        } catch (final IOException e) {
            Log.error("loginSync(): parseLoginResponseContent failed.", e);
            throw new LoginException("Could not parse response.", e);
        }
    }

    /** This will not always provide as many items as requested.
     *
     * @param count
     * @return
     */
    public List<Subject> requestMoreItemsSync(int count) throws RequestMoreItemsException {
        throwIfNoNetwork();

        //Avoid suddenly doing too much network and disk IO
        //as we download too many images.
        if (count > Config.MAXIMUM_DOWNLOAD_ITEMS) {
            count = Config.MAXIMUM_DOWNLOAD_ITEMS;
        }

        Response<List<Subject>> response = null;

        try {
            final Call<List<Subject>> call = callGetSubjects(count);
            response = call.execute();
        } catch (final IOException e) {
            Log.error("requestMoreItemsSync(): request failed.", e);
            throw new RequestMoreItemsException("Exception from request.", e);
        }

        //Presumably this happens when onFailure() is called.
        if (response == null) {
            Log.error("requestMoreItemsSync(): response is null.");
            throw new RequestMoreItemsException("Response is null.");
        }

        if (!response.isSuccessful()) {
            Log.error("requestMoreItemsSync(): request failed with error code: " + response.message());
            throw new RequestMoreItemsException("Request failed with error code: " + response.message());
        }

        final List<Subject> result = response.body();
        if (result == null || result.isEmpty()) {
            throw new RequestMoreItemsException("requestMoreItemsSync(): response contained no subjects.");
        }

        return result;
    }

    public void requestMoreItemsAsync(final int count, final Callback<List<Subject>> callback) {
        throwIfNoNetwork();

        Log.info("requestMoreItemsAsync(): count=" + count);

        final Call<List<Subject>> call = callGetSubjects(count);
        call.enqueue(callback);
    }

    private Call<List<Subject>> callGetSubjects(final int count) {
        return mRetrofitService.getSubjects(getGroupIdForNextQuery(), count);
    }

    private void throwIfNoNetwork() {
        HttpUtils.throwIfNoNetwork(getContext());
    }

    private Context getContext() {
        return mContext;
    }

    public boolean uploadClassificationSync(final String authName, final String authApiKey, final String groupId, final List<HttpUtils.NameValuePair> nameValuePairs) throws UploadException {
        throwIfNoNetwork();
        final RequestBody body = HttpUtils.getPostFormBody(nameValuePairs);
        Request.Builder builder = new Request.Builder()
                .url(getPostUploadUri(groupId))
                .post(body);

        //Add the authentication details to the headers;
        //Be careful: The server still returns OK_CREATED even if we provide the wrong Authorization here.
        //There doesn't seem to be any way to know if it's correct other than checking your recent
        //classifications in your profile.
        //See https://github.com/zooniverse/Galaxy-Zoo/issues/184
        if ((authName != null) && (authApiKey != null)) {
            builder = builder.header("Authorization", HttpUtils.generateAuthorizationHeader(authName, authApiKey));
        }

        final Request request = builder.build();

        //This will be required by the newer API, but we don't ask for it already
        //because the current server wants it to be "application/x-www-form-urlencoded" instead,
        //See http://docs.panoptes.apiary.io/#introduction/authentication
        //conn.setRequestProperty(HttpUtils.HTTP_REQUEST_HEADER_PARAM_CONTENT_TYPE, HttpUtils.CONTENT_TYPE_JSON);

        final okhttp3.Call call = HttpUtils.getHttpClient().newCall(request);

        okhttp3.Response response = null;
        try {
            response = call.execute();
        } catch (final IOException e) {
            Log.error("uploadClassificationSync(): exception during request.", e);
            throw new UploadException("uploadClassificationSync(): exception during request.", e);
        }

        if (response == null) {
            Log.error("uploadClassificationSync(): response is null.");
            return false;
        }

        if (!response.isSuccessful()) {
            Log.error("uploadClassificationSync(): response not successful.");
            response.close();
            return false;
        }

        response.close();
        return true;
    }

    /**
     * This class is meant to be immutable.
     * It only returns references to immutable Strings.
     */
    public static final class Subject {
        private final String mId;
        private final String mZooniverseId;
        private final String mGroupId;
        private final String mLocationStandard;
        private final String mLocationThumbnail;
        private final String mLocationInverted;

        public Subject(final String id, final String zooniverseId, final String groupId, final String locationStandard, final String locationThumbnail, final String locationInverted) {
            this.mId = id;
            this.mZooniverseId = zooniverseId;
            this.mGroupId = groupId;
            this.mLocationStandard = locationStandard;
            this.mLocationThumbnail = locationThumbnail;
            this.mLocationInverted = locationInverted;
        }

        public String getId() {
            return mId;
        }

        public String getZooniverseId() {
            return mZooniverseId;
        }

        public String getGroupId() {
            return mGroupId;
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

    /** A custom GSON deserializer,
     * so we can create Subject objects using the constructor.
     * We want to do so Subject can remain an immutable class.
     */
    static class SubjectDeserializer implements JsonDeserializer<Subject> {
        public Subject deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final String id = getString(jsonObject, "id");
            final String zooniverseId = getString(jsonObject, "zooniverse_id");
            final String groupId = getString(jsonObject, "group_id");

            final JsonElement jsonElementLocations = jsonObject.get("location");
            if (jsonElementLocations == null) {
                return null;
            }

            final JsonObject jsonObjectLocations = jsonElementLocations.getAsJsonObject();
            if (jsonObjectLocations == null) {
                return null;
            }

            final String locationStandard = getString(jsonObjectLocations, "standard");
            final String locationThumbnail = getString(jsonObjectLocations, "thumbnail");
            final String locationInverted = getString(jsonObjectLocations, "inverted");

            return new Subject(id, zooniverseId, groupId, locationStandard, locationThumbnail, locationInverted);
        }

        private static String getString(JsonObject jsonObject, final String name) {
            final JsonElement jsonElementId = jsonObject.get(name);
            return jsonElementId.getAsString();
        }
    }

    public static class LoginException extends Exception {
        LoginException(final String detail, final Exception cause) {
            super(detail, cause);
        }
    }

    public static class UploadException extends Exception {
        UploadException(final String detail, final Exception cause) {
            super(detail, cause);
        }
    }

    public static class RequestMoreItemsException extends Exception {
        RequestMoreItemsException(final String detail, final Exception cause) {
            super(detail, cause);
        }

        public RequestMoreItemsException(final String detail) {
            super(detail);
        }
    }
}
