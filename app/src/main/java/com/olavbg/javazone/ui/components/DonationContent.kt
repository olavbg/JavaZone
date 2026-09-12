package com.olavbg.javazone.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Coffee
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.olavbg.javazone.BuildConfig
import com.olavbg.javazone.R

private val vippsUrl: String = BuildConfig.VIPPS_BOX_URL

private val buyMeACoffeeUrl: String =
    BuildConfig.BUY_ME_A_COFFEE_USERNAME
        .takeIf { it.isNotBlank() }
        ?.let { "https://buymeacoffee.com/$it" }
        .orEmpty()

private val BuyMeACoffeeYellow = Color(0xFFFFDD00)

private const val VIPPS_PACKAGE = "no.dnb.vipps"

@Composable
fun DonationButtons(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    fun openBrowser(url: String) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }
    }

    fun openVippsBox(url: String) {
        val vippsIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(VIPPS_PACKAGE)
        }
        if (vippsIntent.resolveActivity(context.packageManager) != null) {
            runCatching { context.startActivity(vippsIntent) }
        } else {
            openBrowser(url)
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
    ) {
        if (vippsUrl.isNotBlank()) {
            Surface(
                onClick = { openVippsBox(vippsUrl) },
                shape = RoundedCornerShape(0.dp),
                color = Color.Transparent,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(260f / 44f)
            ) {
                Image(
                    painter = painterResource(R.drawable.vipps_button_nob_pill),
                    contentDescription = "Vipps",
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        if (buyMeACoffeeUrl.isNotBlank()) {
            Surface(
                onClick = { openBrowser(buyMeACoffeeUrl) },
                shape = RoundedCornerShape(50),
                color = BuyMeACoffeeYellow,
                contentColor = Color.Black,
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(260f / 44f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Coffee,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Buy me a coffee",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}