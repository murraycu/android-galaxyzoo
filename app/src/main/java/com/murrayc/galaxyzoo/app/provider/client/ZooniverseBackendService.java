package com.murrayc.galaxyzoo.app.provider.client;

import com.murrayc.galaxyzoo.app.provider.HttpUtils;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Headers;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Created by murrayc on 7/25/17.
 */

public interface ZooniverseBackendService {
    /**
     * Gets the project details, including the list of workflows.
     *
     * @param projectSlug
     * @return
     */
    @Headers({
            HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT + ": " + HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT
    })
    @GET("projects?http_cache=true")
    Call<ZooniverseClient.ProjectsResponse> getProject(@Query("slug") String projectSlug);


    /**
     * Gets the decision tree.
     *
     * @param workflowId
     * @return
     */
    @Headers({
            HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT + ": " + HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT
    })
    @GET("workflows/{workflow_id}?http_cache=true")
    Call<ZooniverseClient.WorkflowsResponse> getWorkflow(@Path("workflow_id") String workflowId);

    /** Gets the subjects for use with a workflow.
     *
     * @param workflowId
     * @param limit
     * @return
     */
    @Headers({
            HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT + ": " + HttpUtils.HTTP_REQUEST_HEADER_PARAM_USER_AGENT
    })
    @GET("subjects/queued?http_cache=true")
    Call<ZooniverseClient.SubjectsResponse> getSubjects(@Query("workflow_id") String workflowId, @Query("limit") int limit);
}
