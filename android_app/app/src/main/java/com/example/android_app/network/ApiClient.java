package com.example.android_app.network;

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "http://10.0.3.2:8080/";
    private static final Retrofit RETROFIT = new Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    private static final AuthApi AUTH_API = RETROFIT.create(AuthApi.class);
    private static final TruyenApi API = RETROFIT.create(TruyenApi.class);

    private ApiClient() {}

    public static TruyenApi api() {
        return API;
    }

    public static AuthApi auth() {
        return AUTH_API;
    }

    public static String authorization(android.content.Context context) {
        String token = com.example.android_app.AuthPreferences.getToken(context);
        return token == null || token.trim().isEmpty() ? null : "Bearer " + token;
    }
}
