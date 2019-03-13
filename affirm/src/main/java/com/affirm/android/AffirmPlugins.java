package com.affirm.android;

import android.os.Build;

import com.affirm.android.model.MyAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

class AffirmPlugins {

    private static final Object LOCK = new Object();
    private static AffirmPlugins instance;
    private final Affirm.Configuration configuration;

    private AffirmHttpClient restClient;
    private Gson gson;

    private final Object lock = new Object();
    private final AtomicInteger mLocalLogCounter = new AtomicInteger();

    AffirmPlugins(Affirm.Configuration configuration) {
        this.configuration = configuration;
    }

    static void initialize(Affirm.Configuration configuration) {
        AffirmPlugins.set(new AffirmPlugins(configuration));
    }

    private static void set(AffirmPlugins plugins) {
        synchronized (LOCK) {
            if (instance != null) {
                throw new IllegalStateException("AffirmPlugins is already initialized");
            }
            instance = plugins;
        }
    }

    public static AffirmPlugins get() {
        synchronized (LOCK) {
            return instance;
        }
    }

    static void reset() {
        synchronized (LOCK) {
            instance = null;
        }
    }

    String publicKey() {
        return configuration.publicKey;
    }

    String name() {
        return configuration.name;
    }

    Affirm.Environment environment() {
        return configuration.environment;
    }

    String environmentName() {
        return configuration.environment.name();
    }

    String baseUrl() {
        return configuration.environment.baseUrl;
    }

    String trackerBaseUrl() {
        return configuration.environment.trackerBaseUrl;
    }

    Gson gson() {
        if (gson == null) {
            gson = new GsonBuilder().registerTypeAdapterFactory(MyAdapterFactory.create()).create();
        }
        return gson;
    }

    AffirmHttpClient restClient() {
        synchronized (lock) {
            if (restClient == null) {
                OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
                //add it as the first interceptor
                clientBuilder.interceptors().add(0, new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        final Request request = chain.request()
                            .newBuilder()
                            .addHeader("Accept", "application/json")
                            .addHeader("Content-Type", "application/json")
                            .addHeader("Affirm-User-Agent", "Affirm-Android-SDK")
                            .addHeader("Affirm-User-Agent-Version", BuildConfig.VERSION_NAME)
                            .build();

                        return chain.proceed(request);
                    }
                });
                clientBuilder.connectTimeout(5, TimeUnit.SECONDS);
                clientBuilder.readTimeout(30, TimeUnit.SECONDS);
                clientBuilder.followRedirects(false);
                restClient = AffirmHttpClient.createClient(clientBuilder);
            }
            return restClient;
        }
    }

    void addTrackingData(@NonNull String eventName,
                         @NonNull JsonObject data,
                         @NonNull AffirmTracker.TrackingLevel level) {
        final long timeStamp = System.currentTimeMillis();
        // Set the log counter and then increment the logCounter
        data.addProperty("local_log_counter", mLocalLogCounter.getAndIncrement());
        data.addProperty("ts", timeStamp);
        data.addProperty("app_id", "Android SDK");
        data.addProperty("release", BuildConfig.VERSION_NAME);
        data.addProperty("android_sdk", Build.VERSION.SDK_INT);
        data.addProperty("device_name", Build.MODEL);
        data.addProperty("merchant_key", publicKey());
        data.addProperty("environment", environmentName().toLowerCase());
        data.addProperty("event_name", eventName);
        data.addProperty("level", level.getLevel());
    }
}
