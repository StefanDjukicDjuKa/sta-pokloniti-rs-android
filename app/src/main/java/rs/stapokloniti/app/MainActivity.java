package rs.stapokloniti.app;

import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.webkit.CookieManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String HOME_URL = "https://stapoklonitirs.com";
    private static final int FILE_CHOOSER_REQUEST = 4001;

    private WebView webView;
    private LinearLayout loadingOverlay;
    private LinearLayout offlineOverlay;
    private CircularProgressView circularProgress;
    private ImageView refreshIcon;
    private Button retryButton;

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    private MediaPlayer offlinePlayer;
    private ObjectAnimator offlineRefreshAnimator;

    private ValueCallback<Uri[]> filePathCallback;
    private Uri cameraImageUri;

    private boolean pageFinished = false;
    private boolean destroyed = false;
    private boolean wasOffline = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().setStatusBarColor(Color.WHITE);
        getWindow().setNavigationBarColor(Color.WHITE);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
        );

        setContentView(R.layout.activity_main);

        webView = findViewById(R.id.webView);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        offlineOverlay = findViewById(R.id.offlineOverlay);
        circularProgress = findViewById(R.id.circularProgress);
        refreshIcon = findViewById(R.id.refreshIcon);
        retryButton = findViewById(R.id.retryButton);

        setupRefreshAnimation();
        setupWebView();
        setupConnectivityMonitor();

        retryButton.setOnClickListener(v -> retryConnection());
        refreshIcon.setOnClickListener(v -> retryConnection());

        if (isOnline()) {
            showLoading(0);
            webView.loadUrl(HOME_URL);
        } else {
            showOffline();
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void setupWebView() {
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setSupportZoom(false);
        settings.setBuiltInZoomControls(false);
        settings.setDisplayZoomControls(false);
        settings.setLoadsImagesAutomatically(true);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        settings.setMediaPlaybackRequiresUserGesture(true);
        settings.setUserAgentString(settings.getUserAgentString() + " StaPoklonitiRSApp/1.0");

        webView.setVerticalScrollBarEnabled(false);
        webView.setHorizontalScrollBarEnabled(false);
        webView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        webView.setBackgroundColor(Color.WHITE);

        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, android.graphics.Bitmap favicon) {
                pageFinished = false;
                if (isOnline()) {
                    showLoading(0);
                }
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                pageFinished = true;
                circularProgress.setProgress(100);
                if (isOnline()) {
                    loadingOverlay.postDelayed(() -> {
                        if (!destroyed && pageFinished && isOnline()) {
                            hideLoading();
                        }
                    }, 220);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return handleNavigation(request.getUrl());
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request != null && request.isForMainFrame() && !isOnline()) {
                    showOffline();
                }
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                circularProgress.setProgress(newProgress);

                if (newProgress < 100 && isOnline()) {
                    loadingOverlay.setVisibility(View.VISIBLE);
                    offlineOverlay.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                }

                if (newProgress == 100 && pageFinished && isOnline()) {
                    loadingOverlay.postDelayed(() -> {
                        if (!destroyed && isOnline()) {
                            hideLoading();
                        }
                    }, 180);
                }
            }

            @Override
            public boolean onShowFileChooser(
                    WebView webView,
                    ValueCallback<Uri[]> filePathCallbackParam,
                    FileChooserParams fileChooserParams
            ) {
                if (filePathCallback != null) {
                    filePathCallback.onReceiveValue(null);
                }

                filePathCallback = filePathCallbackParam;

                Intent contentIntent;
                try {
                    contentIntent = fileChooserParams.createIntent();
                } catch (Exception e) {
                    contentIntent = new Intent(Intent.ACTION_GET_CONTENT);
                    contentIntent.setType("*/*");
                    contentIntent.addCategory(Intent.CATEGORY_OPENABLE);
                }

                contentIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);

                Intent chooser = new Intent(Intent.ACTION_CHOOSER);
                chooser.putExtra(Intent.EXTRA_INTENT, contentIntent);
                chooser.putExtra(Intent.EXTRA_TITLE, "Izaberi fotografiju ili fajl");

                if (android.os.Build.VERSION.SDK_INT >= 29) {
                    Intent cameraIntent = createCameraIntent();
                    if (cameraIntent != null) {
                        chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
                    }
                }

                try {
                    startActivityForResult(chooser, FILE_CHOOSER_REQUEST);
                    return true;
                } catch (ActivityNotFoundException e) {
                    filePathCallback = null;
                    Toast.makeText(MainActivity.this, "Nije pronađena aplikacija za izbor fajla.", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        });

        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(
                    String url,
                    String userAgent,
                    String contentDisposition,
                    String mimeType,
                    long contentLength
            ) {
                try {
                    DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
                    request.addRequestHeader("User-Agent", userAgent);
                    String cookies = CookieManager.getInstance().getCookie(url);
                    if (cookies != null) {
                        request.addRequestHeader("Cookie", cookies);
                    }
                    request.setMimeType(mimeType);
                    request.setNotificationVisibility(
                            DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED
                    );
                    request.setDestinationInExternalFilesDir(
                            MainActivity.this,
                            Environment.DIRECTORY_DOWNLOADS,
                            android.webkit.URLUtil.guessFileName(url, contentDisposition, mimeType)
                    );

                    DownloadManager dm = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        Toast.makeText(MainActivity.this, "Preuzimanje je pokrenuto.", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    openExternal(Uri.parse(url));
                }
            }
        });
    }

    private Intent createCameraIntent() {
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (cameraIntent.resolveActivity(getPackageManager()) == null) {
                return null;
            }

            android.content.ContentValues values = new android.content.ContentValues();
            values.put(MediaStore.Images.Media.DISPLAY_NAME, "sta_pokloniti_" + System.currentTimeMillis() + ".jpg");
            values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

            cameraImageUri = getContentResolver().insert(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    values
            );

            if (cameraImageUri != null) {
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, cameraImageUri);
                cameraIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }

            return cameraIntent;
        } catch (Exception e) {
            cameraImageUri = null;
            return null;
        }
    }

    private boolean handleNavigation(Uri uri) {
        if (uri == null) {
            return false;
        }

        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();

        if ("http".equals(scheme) || "https".equals(scheme)) {
            return false;
        }

        if ("intent".equals(scheme)) {
            try {
                Intent intent = Intent.parseUri(uri.toString(), Intent.URI_INTENT_SCHEME);
                if (intent.resolveActivity(getPackageManager()) != null) {
                    startActivity(intent);
                    return true;
                }

                String fallbackUrl = intent.getStringExtra("browser_fallback_url");
                if (fallbackUrl != null && !fallbackUrl.isEmpty()) {
                    webView.loadUrl(fallbackUrl);
                    return true;
                }
            } catch (Exception ignored) {
            }
            return true;
        }

        return openExternal(uri);
    }

    private boolean openExternal(Uri uri) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            startActivity(intent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void setupConnectivityMonitor() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) {
            return;
        }

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                runOnUiThread(() -> refreshConnectivityState(450));
            }

            @Override
            public void onLost(Network network) {
                runOnUiThread(() -> refreshConnectivityState(450));
            }

            @Override
            public void onCapabilitiesChanged(Network network, NetworkCapabilities networkCapabilities) {
                runOnUiThread(() -> refreshConnectivityState(250));
            }
        };

        try {
            NetworkRequest request = new NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build();
            connectivityManager.registerNetworkCallback(request, networkCallback);
        } catch (Exception ignored) {
        }
    }

    private void refreshConnectivityState(long delayMs) {
        webView.postDelayed(() -> {
            if (destroyed) {
                return;
            }

            if (isOnline()) {
                if (wasOffline || offlineOverlay.getVisibility() == View.VISIBLE) {
                    wasOffline = false;
                    stopOfflineAudio();
                    stopOfflineRefreshAnimation();
                    offlineOverlay.setVisibility(View.GONE);
                    webView.setVisibility(View.VISIBLE);
                    showLoading(0);

                    String current = webView.getUrl();
                    if (current == null || current.trim().isEmpty() || current.startsWith("about:")) {
                        webView.loadUrl(HOME_URL);
                    } else {
                        webView.reload();
                    }
                }
            } else {
                showOffline();
            }
        }, delayMs);
    }

    private boolean isOnline() {
        if (connectivityManager == null) {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        }

        if (connectivityManager == null) {
            return false;
        }

        try {
            Network network = connectivityManager.getActiveNetwork();
            if (network == null) {
                return false;
            }

            NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
            if (capabilities == null) {
                return false;
            }

            return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        } catch (Exception e) {
            return false;
        }
    }

    private void showLoading(int progress) {
        if (!isOnline()) {
            showOffline();
            return;
        }

        circularProgress.setProgress(progress);
        loadingOverlay.setVisibility(View.VISIBLE);
        offlineOverlay.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        stopOfflineAudio();
        stopOfflineRefreshAnimation();
    }

    private void hideLoading() {
        loadingOverlay.setVisibility(View.GONE);
        offlineOverlay.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
    }

    private void showOffline() {
        wasOffline = true;
        loadingOverlay.setVisibility(View.GONE);
        offlineOverlay.setVisibility(View.VISIBLE);
        webView.setVisibility(View.INVISIBLE);
        startOfflineRefreshAnimation();
        startOfflineAudio();
    }

    private void retryConnection() {
        refreshIcon.animate().cancel();
        refreshIcon.setRotation(0f);
        refreshIcon.animate()
                .rotationBy(360f)
                .setDuration(650)
                .setInterpolator(new LinearInterpolator())
                .start();

        if (isOnline()) {
            stopOfflineAudio();
            offlineOverlay.setVisibility(View.GONE);
            webView.setVisibility(View.VISIBLE);
            showLoading(0);

            String current = webView.getUrl();
            if (current == null || current.trim().isEmpty() || current.startsWith("about:")) {
                webView.loadUrl(HOME_URL);
            } else {
                webView.reload();
            }
        } else {
            Toast.makeText(this, "Internet još nije dostupan.", Toast.LENGTH_SHORT).show();
        }
    }

    private void setupRefreshAnimation() {
        offlineRefreshAnimator = ObjectAnimator.ofFloat(refreshIcon, View.ROTATION, 0f, 360f);
        offlineRefreshAnimator.setDuration(1800);
        offlineRefreshAnimator.setInterpolator(new LinearInterpolator());
        offlineRefreshAnimator.setRepeatCount(ObjectAnimator.INFINITE);
    }

    private void startOfflineRefreshAnimation() {
        if (offlineRefreshAnimator != null && !offlineRefreshAnimator.isStarted()) {
            offlineRefreshAnimator.start();
        }
    }

    private void stopOfflineRefreshAnimation() {
        if (offlineRefreshAnimator != null) {
            offlineRefreshAnimator.cancel();
            refreshIcon.setRotation(0f);
        }
    }

    private void startOfflineAudio() {
        if (offlinePlayer != null) {
            if (!offlinePlayer.isPlaying()) {
                try {
                    offlinePlayer.start();
                } catch (Exception ignored) {
                }
            }
            return;
        }

        try {
            offlinePlayer = MediaPlayer.create(this, R.raw.offline_ambient);
            if (offlinePlayer != null) {
                offlinePlayer.setLooping(true);
                offlinePlayer.setVolume(0.14f, 0.14f);
                offlinePlayer.start();
            }
        } catch (Exception ignored) {
        }
    }

    private void stopOfflineAudio() {
        if (offlinePlayer != null) {
            try {
                if (offlinePlayer.isPlaying()) {
                    offlinePlayer.pause();
                }
            } catch (Exception ignored) {
            }
        }
    }

    private void releaseOfflineAudio() {
        if (offlinePlayer != null) {
            try {
                offlinePlayer.stop();
            } catch (Exception ignored) {
            }
            try {
                offlinePlayer.release();
            } catch (Exception ignored) {
            }
            offlinePlayer = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode != FILE_CHOOSER_REQUEST || filePathCallback == null) {
            return;
        }

        Uri[] results = null;

        if (resultCode == RESULT_OK) {
            if (data == null || (data.getData() == null && data.getClipData() == null)) {
                if (cameraImageUri != null) {
                    results = new Uri[]{cameraImageUri};
                }
            } else if (data.getClipData() != null) {
                int count = data.getClipData().getItemCount();
                results = new Uri[count];
                for (int i = 0; i < count; i++) {
                    results[i] = data.getClipData().getItemAt(i).getUri();
                }
            } else if (data.getData() != null) {
                results = new Uri[]{data.getData()};
            }
        }

        filePathCallback.onReceiveValue(results);
        filePathCallback = null;
        cameraImageUri = null;
    }

    @Override
    public void onBackPressed() {
        if (webView != null && webView.getVisibility() == View.VISIBLE && webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (webView != null) {
            webView.onPause();
        }
        stopOfflineAudio();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) {
            webView.onResume();
        }
        if (offlineOverlay != null && offlineOverlay.getVisibility() == View.VISIBLE) {
            startOfflineAudio();
        }
    }

    @Override
    protected void onDestroy() {
        destroyed = true;

        if (connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Exception ignored) {
            }
        }

        if (filePathCallback != null) {
            filePathCallback.onReceiveValue(null);
            filePathCallback = null;
        }

        stopOfflineRefreshAnimation();
        releaseOfflineAudio();

        if (webView != null) {
            webView.stopLoading();
            webView.setWebChromeClient(null);
            webView.setWebViewClient(null);
            webView.destroy();
        }

        super.onDestroy();
    }
}
