package com.example.android_app.network;

import com.example.android_app.model.LoginRequest;
import com.example.android_app.model.LoginResponse;
import com.example.android_app.model.ForgotPasswordRequest;
import com.example.android_app.model.MessageResponse;
import com.example.android_app.model.RegisterRequest;
import com.example.android_app.model.RegisterResponse;
import com.example.android_app.model.ResetPasswordRequest;
import com.example.android_app.model.UserResponse;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;

public interface AuthApi {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest request);

    @POST("auth/register")
    Call<RegisterResponse> register(@Body RegisterRequest request);

    @GET("auth/me")
    Call<UserResponse> getCurrentUser(@Header("Authorization") String authorization);

    @POST("auth/forgot-password")
    Call<MessageResponse> forgotPassword(@Body ForgotPasswordRequest request);

    @POST("auth/reset-password")
    Call<MessageResponse> resetPassword(@Body ResetPasswordRequest request);
}
