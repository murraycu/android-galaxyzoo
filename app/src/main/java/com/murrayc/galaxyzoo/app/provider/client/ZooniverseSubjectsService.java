package com.murrayc.galaxyzoo.app.provider.client;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import java.util.List;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by murrayc on 7/25/17.
 */

public interface ZooniverseSubjectsService {
    @Headers({
            HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT + ": " + HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT
    })
    @GET("groups/{group-id}/subjects")
    Call<List<ZooniverseClient.Subject>> getSubjects(@Path("group-id") String groupId, @Query("limit") int limit);
}
