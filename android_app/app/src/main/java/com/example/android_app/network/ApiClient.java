package com.example.android_app.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "http://10.0.3.2:8080/";
    private static final AuthApi AUTH_API = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AuthApi.class);

    private ApiClient() {}

    public static AuthApi auth() {
        return AUTH_API;
    }
}
