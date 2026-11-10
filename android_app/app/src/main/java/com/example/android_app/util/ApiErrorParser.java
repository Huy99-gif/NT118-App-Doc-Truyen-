package com.example.android_app.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import retrofit2.Response;

public final class ApiErrorParser {
    private ApiErrorParser() {}

    public static String getMessage(Response<?> response, String fallback) {
        if (response.errorBody() == null) {
            return fallback;
        }

        try {
            String message = new JSONObject(response.errorBody().string()).optString("error");
            return message.isEmpty() ? fallback : message;
        } catch (IOException | JSONException exception) {
            return fallback;
        }
    }
}
