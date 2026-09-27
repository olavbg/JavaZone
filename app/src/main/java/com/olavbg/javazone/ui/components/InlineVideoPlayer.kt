package com.olavbg.javazone.ui.components

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.olavbg.javazone.R

private class CustomViewInfo(
    val view: View,
    val callback: WebChromeClient.CustomViewCallback
)

private class PlaybackBridge(private val onUpdate: (Float) -> Unit) {
    private val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())

    @android.webkit.JavascriptInterface
    fun onTimeUpdate(seconds: Float) {
        mainHandler.post { onUpdate(seconds) }
    }
}

private fun buildVideoHtml(embedUrl: String, initialSeconds: Float = 0f): String = """
    <!DOCTYPE html>
    <html>
    <head>
        <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
        <style>
            * { margin: 0; padding: 0; box-sizing: border-box; }
            html, body { width: 100%; height: 100%; background: #000000; overflow: hidden; }
            iframe { width: 100%; height: 100%; border: 0; }
        </style>
    </head>
    <body>
        <iframe
            id="player"
            src="$embedUrl"
            width="100%"
            height="100%"
            frameborder="0"
            allow="autoplay; fullscreen; picture-in-picture"
            allowfullscreen>
        </iframe>
        <script>
            var iframe = document.getElementById('player');
            var initialSeconds = $initialSeconds;
            var hasSeeked = false;

            function postToPlayer(msg) {
                try {
                    if (iframe && iframe.contentWindow) {
                        iframe.contentWindow.postMessage(JSON.stringify(msg), '*');
                    }
                } catch(e) {}
            }

            function setupPlayer() {
                postToPlayer({ method: 'addEventListener', value: 'timeupdate' });
                postToPlayer({ method: 'addEventListener', value: 'pause' });
                postToPlayer({ method: 'addEventListener', value: 'finish' });
                postToPlayer({ event: 'listening' });
                if (initialSeconds > 0 && !hasSeeked) {
                    postToPlayer({ method: 'setCurrentTime', value: initialSeconds });
                    postToPlayer({ event: 'command', func: 'seekTo', args: [initialSeconds, true] });
                }
            }

            window.addEventListener('message', function(event) {
                try {
                    var data = typeof event.data === 'string' ? JSON.parse(event.data) : event.data;
                    if (!data) return;

                    if (data.event === 'ready') {
                        setupPlayer();
                        if (initialSeconds > 0 && !hasSeeked) {
                            hasSeeked = true;
                            postToPlayer({ method: 'setCurrentTime', value: initialSeconds });
                        }
                    }

                    if ((data.event === 'timeupdate' || data.event === 'playProgress') && data.data && typeof data.data.seconds === 'number') {
                        if (window.AndroidPlayback) {
                            window.AndroidPlayback.onTimeUpdate(data.data.seconds);
                        }
                    }

                    if (data.event === 'infoDelivery' && data.info && typeof data.info.currentTime === 'number') {
                        if (window.AndroidPlayback) {
                            window.AndroidPlayback.onTimeUpdate(data.info.currentTime);
                        }
                    }
                } catch(e) {}
            });

            iframe.addEventListener('load', function() {
                setupPlayer();
                setTimeout(setupPlayer, 1000);
                setTimeout(setupPlayer, 2500);
            });
        </script>
    </body>
    </html>
""".trimIndent()

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun InlineVideoPlayer(
    videoUrl: String,
    onClose: () -> Unit,
    onOpenExternal: () -> Unit,
    modifier: Modifier = Modifier,
    initialPlaybackSeconds: Float = 0f,
    onTimeUpdate: ((Float) -> Unit)? = null
) {
    val embedUrl = remember(videoUrl, initialPlaybackSeconds) {
        resolveVideoEmbedUrl(videoUrl, initialPlaybackSeconds.toInt())
    }
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var customViewInfo by remember { mutableStateOf<CustomViewInfo?>(null) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> webViewRef?.onPause()
                Lifecycle.Event.ON_RESUME -> webViewRef?.onResume()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            webViewRef?.let { wv ->
                wv.removeJavascriptInterface("AndroidPlayback")
                wv.loadUrl("about:blank")
                wv.stopLoading()
                wv.destroy()
            }
            webViewRef = null
        }
    }

    if (customViewInfo != null) {
        val info = customViewInfo!!
        Dialog(
            onDismissRequest = {
                info.callback.onCustomViewHidden()
                customViewInfo = null
            },
            properties = DialogProperties(
                usePlatformDefaultWidth = false,
                decorFitsSystemWindows = false
            )
        ) {
            BackHandler {
                info.callback.onCustomViewHidden()
                customViewInfo = null
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                AndroidView(
                    factory = {
                        (info.view.parent as? ViewGroup)?.removeView(info.view)
                        info.view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Column(modifier = modifier.clipToBounds()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        setBackgroundColor(android.graphics.Color.BLACK)
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            mediaPlaybackRequiresUserGesture = false
                            loadWithOverviewMode = true
                            useWideViewPort = true
                            allowFileAccess = false
                            allowContentAccess = false
                        }
                        if (onTimeUpdate != null) {
                            addJavascriptInterface(PlaybackBridge(onTimeUpdate), "AndroidPlayback")
                        }
                        webViewClient = object : WebViewClient() {
                            override fun onPageFinished(view: WebView?, url: String?) {
                                if (!hasError) {
                                    isLoading = false
                                }
                            }

                            override fun onReceivedError(
                                view: WebView?,
                                request: WebResourceRequest?,
                                error: WebResourceError?
                            ) {
                                val reqUrl = request?.url?.toString().orEmpty()
                                val isMainOrEmbed = request?.isForMainFrame == true ||
                                    reqUrl.startsWith("https://player.vimeo.com/video/") ||
                                    reqUrl.startsWith("https://www.youtube.com/embed/")

                                if (isLoading && isMainOrEmbed) {
                                    hasError = true
                                    isLoading = false
                                }
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onShowCustomView(view: View, callback: CustomViewCallback) {
                                customViewInfo = CustomViewInfo(view, callback)
                            }

                            override fun onHideCustomView() {
                                customViewInfo?.callback?.onCustomViewHidden()
                                customViewInfo = null
                            }
                        }
                        loadDataWithBaseURL("https://javazone.no", buildVideoHtml(embedUrl, initialPlaybackSeconds), "text/html", "UTF-8", null)
                        webViewRef = this
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (isLoading && !hasError) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }

            if (hasError) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(36.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.video_offline_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.video_offline_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            hasError = false
                            isLoading = true
                            webViewRef?.loadDataWithBaseURL("https://javazone.no", buildVideoHtml(embedUrl, initialPlaybackSeconds), "text/html", "UTF-8", null)
                        },
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(stringResource(R.string.retry), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = {
                    webViewRef?.onPause()
                    onClose()
                },
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = stringResource(R.string.close_video),
                    style = MaterialTheme.typography.labelLarge
                )
            }

            TextButton(
                onClick = onOpenExternal,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = stringResource(R.string.open_external_video),
                    style = MaterialTheme.typography.labelLarge
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.OpenInNew,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
