package com.example.android_app.network;

import android.content.Context;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public final class ApiClient {
    private static final String BASE_URL = "http://10.0.3.2:8080/"; // backend local
    //private static final String BASE_URL = "http://32.194.224.117:8080/";
    private static volatile AuthApi authApi;
    private static volatile TruyenApi api;
    private static volatile SessionManager sessionManager;

    private ApiClient() {}

    public static synchronized void initialize(Context context) {
        if (context == null) {
            throw new IllegalArgumentException("Context không được null");
        }
        if (api != null) {
            return;
        }

        Context applicationContext = context.getApplicationContext();
        SessionManager manager = new SessionManager(applicationContext);
        // gắn interceptor để bắt 401 tự logout
        OkHttpClient client = new OkHttpClient.Builder()
                .addInterceptor(new AuthInterceptor(manager))
                .build();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(BASE_URL)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        sessionManager = manager;
        authApi = retrofit.create(AuthApi.class);
        api = retrofit.create(TruyenApi.class);
    }

    public static TruyenApi api() {
        TruyenApi currentApi = api;
        if (currentApi == null) {
            throw new IllegalStateException("ApiClient chưa được khởi tạo");
        }
        return currentApi;
    }

    public static AuthApi auth() {
        AuthApi currentAuthApi = authApi;
        if (currentAuthApi == null) {
            throw new IllegalStateException("ApiClient chưa được khởi tạo");
        }
        return currentAuthApi;
    }

    public static void onLoginSuccess(String token) {
        SessionManager manager = sessionManager;
        if (manager == null) {
            throw new IllegalStateException("ApiClient chưa được khởi tạo");
        }
        manager.onLoginSuccess(token);
    }

    public static String authorization(android.content.Context context) {
        String token = com.example.android_app.AuthPreferences.getToken(context);
        return token == null || token.trim().isEmpty() ? null : "Bearer " + token;
    }
}
