package com.example.caats.network;

import android.content.Context;
import android.util.Log;

import com.example.caats.utils.PreferenceManager;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class SupabaseClient {

    public static final String SUPABASE_URL = "https://dqrfgdwzpxaajdqenuqf.supabase.co/";
    public static final String API_KEY = "xxxxx";

    private static Retrofit authRetrofit = null;
    private static Retrofit restRetrofit = null;

    private static OkHttpClient restOkHttpClient = null;

    public static Retrofit getAuthClient() {
        if (authRetrofit == null) {
            authRetrofit = buildClient(SUPABASE_URL + "auth/v1/", false, null);
        }
        return authRetrofit;
    }

    public static Retrofit getRestClient(Context context) {
        if (restRetrofit == null) {
            restRetrofit = buildClient(SUPABASE_URL + "rest/v1/", true, context);
        }
        return restRetrofit;
    }

    public static OkHttpClient getOkHttpClientInstance(Context context) {
        if (restOkHttpClient == null) {
            getRestClient(context);
        }
        return restOkHttpClient;
    }

    private static Retrofit buildClient(String baseUrl, boolean isRest, Context context) {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        clientBuilder.addInterceptor(logging);

        clientBuilder.addInterceptor(chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder()
                    .header("apikey", API_KEY);

            if (isRest && context != null) {
                String token = PreferenceManager.getToken(context.getApplicationContext());
                if (token != null && !token.isEmpty()) {
                    builder.header("Authorization", "Bearer " + token);
                }
            }
            return chain.proceed(builder.build());
        });

        OkHttpClient okHttpClient = clientBuilder.build();
        if (isRest) {
            restOkHttpClient = okHttpClient;
        }

        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
}