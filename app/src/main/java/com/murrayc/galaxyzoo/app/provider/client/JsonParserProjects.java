package com.murrayc.galaxyzoo.app.provider.client;

import android.support.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonParserProjects {
    /** A custom GSON deserializer,
     * so we can create Project objects using the constructor.
     * We want to do so Project can remain an immutable class.
     */
    static class ProjectsResponseDeserializer implements JsonDeserializer<ZooniverseClient.ProjectsResponse> {
        public ZooniverseClient.ProjectsResponse deserialize(final JsonElement json, final Type typeOfT, final JsonDeserializationContext context)
                throws JsonParseException {
            final JsonObject jsonObject = json.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final JsonArray jsonSubjects = jsonObject.getAsJsonArray("projects");
            if (jsonSubjects == null) {
                return null;
            }

            // Parse each project (usually just one):
            final List<ZooniverseClient.Project> projects = new ArrayList<>();
            for (final JsonElement jsonSubject : jsonSubjects) {
                final JsonObject asObject = jsonSubject.getAsJsonObject();
                final ZooniverseClient.Project workflow = deserializeProjectFromJsonObject(asObject);
                projects.add(workflow);
            }

            final ZooniverseClient.ProjectsResponse result = new ZooniverseClient.ProjectsResponse(projects);
            return result;
        }

        @Nullable
        private ZooniverseClient.Project deserializeProjectFromJsonObject(JsonObject jsonObject) {
            final String id = JsonUtils.getString(jsonObject, "id");
            final String displayName =  JsonUtils.getString(jsonObject, "display_name");

            List<String> workflowIds = null;
            List<String> activeWorkflowIds = null;

            final JsonObject jsonObjectLinks = jsonObject.getAsJsonObject("links");
            if (jsonObjectLinks != null) {
                workflowIds = JsonUtils.listOfStringsFromJsonArray(jsonObjectLinks, "workflows");
                activeWorkflowIds = JsonUtils.listOfStringsFromJsonArray(jsonObjectLinks, "active_workflows");
            }

            return new ZooniverseClient.Project(id, displayName, workflowIds, activeWorkflowIds);
        }

        private List<ZooniverseClient.Task> deserializeTasksFromJsonElement(final JsonElement jsonElement) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final Set<Map.Entry<String, JsonElement>> jsonEntrySet = jsonObject.entrySet();
            if (jsonEntrySet == null) {
                return null;
            }

            // Parse each task:
            final List<ZooniverseClient.Task> tasks = new ArrayList<>();
            for (final Map.Entry<String, JsonElement> jsonEntry : jsonEntrySet) {
                final String id = jsonEntry.getKey();

                final JsonElement jsonValue = jsonEntry.getValue();
                if (jsonValue == null) {
                    continue;
                }

                final JsonObject asObject = jsonValue.getAsJsonObject();
                if (asObject == null) {
                    continue;
                }

                final ZooniverseClient.Task task = deserializeTaskFromJsonElement(asObject, id);
                tasks.add(task);
            }

            return tasks;
        }

        private ZooniverseClient.Task deserializeTaskFromJsonElement(final JsonElement jsonElement, final String id) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final String type = JsonUtils.getString(jsonObject, "type");
            final String question = JsonUtils.getString(jsonObject, "question");
            final String help =  JsonUtils.getString(jsonObject, "help");
            final boolean required = JsonUtils.getBoolean(jsonObject, "required");

            List<ZooniverseClient.Answer> answers = null;
            final JsonElement jsonElementAnswers = jsonObject.get("answers");
            if (jsonElementAnswers != null) {
                answers = deserializeAnswersFromJsonElement(jsonElementAnswers);
            }

            return new ZooniverseClient.Task(id, type, question, help, answers, required);
        }

        private List<ZooniverseClient.Answer> deserializeAnswersFromJsonElement(final JsonElement jsonElement) {
            final JsonArray jsonArray = jsonElement.getAsJsonArray();
            if (jsonArray == null) {
                return null;
            }

            // Parse each answer:
            final List<ZooniverseClient.Answer> answers = new ArrayList<>();
            for (final JsonElement jsonAnswer : jsonArray) {
                if (jsonAnswer == null) {
                    continue;
                }

                final ZooniverseClient.Answer answer = deserializeAnswerFromJsonElement(jsonAnswer);
                answers.add(answer);
            }

            return answers;
        }

        private ZooniverseClient.Answer deserializeAnswerFromJsonElement(final JsonElement jsonElement) {
            final JsonObject jsonObject = jsonElement.getAsJsonObject();
            if (jsonObject == null) {
                return null;
            }

            final String label = JsonUtils.getString(jsonObject, "label");
            final String next = JsonUtils.getString(jsonObject, "next");

            return new ZooniverseClient.Answer(label, next);
        }
    }
}
