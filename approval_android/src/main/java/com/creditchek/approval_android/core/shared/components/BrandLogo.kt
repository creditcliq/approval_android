package com.creditchek.approval_android.core.shared.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import com.creditchek.approval_android.R
import com.creditchek.approval_android.core.engines.BitmapUtils.decodeBase64ToBitmap

@Composable
fun BrandLogo(
    logoSource: String?,
    contentDescription: String? = "Brand logo",
    modifier: Modifier = Modifier
) {
    val fallBackPainter = painterResource(id = R.drawable.ic_logo)
    val trimmedLogo = logoSource?.trim()

    when {
        trimmedLogo.isNullOrBlank() -> {
            Image(
                painter = fallBackPainter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }

        trimmedLogo.startsWith("http://", ignoreCase = true) || trimmedLogo.startsWith("https://", ignoreCase = true) -> {
            AsyncImage(
                model = trimmedLogo,
                placeholder = fallBackPainter,
                error = fallBackPainter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }

        trimmedLogo.startsWith("data:image", ignoreCase = true) || trimmedLogo.length > 100 -> {
            val bitmap = remember(trimmedLogo) { decodeBase64ToBitmap(trimmedLogo) }
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = modifier
                )
            } else {
                Image(
                    painter = fallBackPainter,
                    contentDescription = contentDescription,
                    contentScale = ContentScale.Fit,
                    modifier = modifier
                )
            }
        }

        else -> {
            Image(
                painter = fallBackPainter,
                contentDescription = contentDescription,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
    }
}