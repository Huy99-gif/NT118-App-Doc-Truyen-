package com.example.android_app.network;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

public final class AuthInterceptor implements Interceptor {
    private final SessionManager sessionManager;

    public AuthInterceptor(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        String authorization = request.header("Authorization");
        String path = request.url().encodedPath();

        if (response.code() == 401
                && hasAuthorizationToken(authorization)
                && !isPublicEndpoint(path)) {
            sessionManager.handleUnauthorized(authorization);
        }

        return response;
    }

    private boolean hasAuthorizationToken(String authorization) {
        if (authorization == null) {
            return false;
        }
        String value = authorization.trim();
        return !value.isEmpty() && !"Bearer".equalsIgnoreCase(value);
    }

    private boolean isPublicEndpoint(String path) {
        return "/auth/login".equals(path)
                || "/auth/register".equals(path)
                || "/auth/forgot-password".equals(path)
                || "/auth/reset-password".equals(path);
    }
}
