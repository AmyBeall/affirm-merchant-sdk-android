package com.affirm.android;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

import com.affirm.android.model.CardDetails;
import com.affirm.android.model.Checkout;
import com.affirm.android.model.CheckoutResponse;

import java.io.IOException;
import java.io.InputStream;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class VcnCheckoutActivity extends CheckoutCommonActivity implements AffirmWebChromeClient.Callbacks, VcnCheckoutWebViewClient.Callbacks {

    public static final String CREDIT_DETAILS = "credit_details";

    static void startActivity(@NonNull Activity activity, int requestCode, @NonNull Checkout checkout) {
        final Intent intent = new Intent(activity, VcnCheckoutActivity.class);
        intent.putExtra(CHECKOUT_EXTRA, checkout);
        activity.startActivityForResult(intent, requestCode);
    }

    @Override
    void startCheckout() {
        new VcnCheckoutTask(checkout, new CheckoutCallback() {
            @Override
            public void onError(Exception exception) {
                onWebViewError(exception);
            }

            @Override
            public void onSuccess(CheckoutResponse response) {
                final String html = initialHtml(response);
                final Uri uri = Uri.parse(response.redirectUrl());
                webView.loadDataWithBaseURL("https://" + uri.getHost(), html, "text/html", "utf-8", null);
            }
        }).execute();
    }

    @Override
    void setupWebView() {
        AffirmUtils.debuggableWebView(this);
        webView.setWebViewClient(
                new VcnCheckoutWebViewClient(AffirmPlugins.get().gson(), this));
        webView.setWebChromeClient(new AffirmWebChromeClient(this));
        clearCookies();
    }

    private String initialHtml(CheckoutResponse response) {
        String html;
        try {
            final InputStream ins = getResources().openRawResource(R.raw.vcn_checkout);
            html = AffirmUtils.readInputStream(ins);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        final HashMap<String, String> map = new HashMap<>();

        map.put("URL", response.redirectUrl());
        map.put("URL2", response.redirectUrl());
        map.put("JS_CALLBACK_ID", response.jsCallbackId());
        map.put("CONFIRM_CB_URL", AffirmWebViewClient.AFFIRM_CONFIRMATION_URL);
        map.put("CANCELLED_CB_URL", AffirmWebViewClient.AFFIRM_CANCELLATION_URL);
        return AffirmUtils.replacePlaceholders(html, map);
    }

    @Override
    public void onWebViewConfirmation(@NonNull CardDetails cardDetails) {
        final Intent intent = new Intent();
        intent.putExtra(CREDIT_DETAILS, cardDetails);
        setResult(RESULT_OK, intent);
        finish();
    }

    private static class VcnCheckoutTask extends AsyncTask<Void, Void, CheckoutResponseWrapper> {
        @NonNull
        private final Checkout checkout;
        @NonNull
        private final WeakReference<CheckoutCallback> mCallbackRef;

        VcnCheckoutTask(@NonNull final Checkout checkout,
                        @Nullable final CheckoutCallback callback) {
            this.checkout = checkout;
            this.mCallbackRef = new WeakReference<>(callback);
        }

        @Override
        protected CheckoutResponseWrapper doInBackground(Void... params) {
            try {
                return new CheckoutResponseWrapper(AffirmApiHandler.executeVcnCheckout(checkout), null);
            } catch (IOException e) {
                return new CheckoutResponseWrapper(null, e);
            }
        }

        @Override
        protected void onPostExecute(CheckoutResponseWrapper result) {
            if (result.response != null && mCallbackRef.get() != null) {
                mCallbackRef.get().onSuccess(result.response);
            } else {
                mCallbackRef.get().onError(result.exception);
            }
        }
    }
}
