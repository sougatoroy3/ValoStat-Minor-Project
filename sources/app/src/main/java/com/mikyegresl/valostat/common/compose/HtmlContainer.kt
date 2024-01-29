package com.mikyegresl.valostat.common.compose

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.text.Html
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.CharacterStyle
import android.text.style.ImageSpan
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.res.ResourcesCompat
import androidx.core.text.toSpanned
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.OriginalSize
import coil.target.Target
import com.mikyegresl.valostat.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class HtmlSpannableContent(
    open val rContentSpanned: Spanned,
) {
    data class RegularTextHtmlSpannableContent(
        override val rContentSpanned: Spanned,
    ): HtmlSpannableContent(rContentSpanned)

    data class ImageHtmlSpannableContent(
        override val rContentSpanned: Spanned,
    ): HtmlSpannableContent(rContentSpanned) {

        private val imageSpan = rContentSpanned
            .getSpans(0, rContentSpanned.length, ImageSpan::class.java)
            .firstOrNull()

        val imageUrl: String? get() = imageSpan?.source
    }
}

@Composable
fun SpannableHtmlText(
    modifier: Modifier = Modifier,
    spanned: Spanned,
    textColor: Color,
    textSize: TextUnit? = null,
    isBold: Boolean,
    linkTextColor: Color = textColor,
    linesCount: Int? = null,
    lineSpacing: Float? = null,
    textAlign: TextAlign? = null,
    onClick: () -> Unit = {}
) {
    val textSizePx = textSize?.let { spToPx(it) }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                setOnClickListener { onClick() }

                setText(
                    spanned,
                    TextView.BufferType.SPANNABLE
                )
                setTextSize(
                    TypedValue.COMPLEX_UNIT_PX, textSizePx ?: resources.getDimension(R.dimen.post_text_size)
                )

                val lineSpacingMultiplier = .4.sp
                lineSpacing?.let {
                    setLineSpacing(it, lineSpacingMultiplier.value)
                }
                typeface = ResourcesCompat.getFont(
                    context,
                    if (isBold) {
                        R.font.roboto_bold
                    } else {
                        R.font.roboto_regular
                    }
                )

                val alignmentConstant = when (textAlign) {
                    TextAlign.Center -> View.TEXT_ALIGNMENT_CENTER
                    TextAlign.Start -> View.TEXT_ALIGNMENT_VIEW_START
                    TextAlign.End -> View.TEXT_ALIGNMENT_VIEW_END
                    else -> null
                }
                alignmentConstant?.let { textAlignment = it }

                setTextColor(textColor.toArgb())
                setLinkTextColor(linkTextColor.toArgb())

                isSingleLine = false
                linesCount?.let { linesCount ->
                    setLines(linesCount)
                    maxLines = linesCount
                }
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = {
            it.setOnClickListener { onClick() }

            it.setText(
                spanned,
                TextView.BufferType.SPANNABLE
            )
            it.isSingleLine = false
            linesCount?.let { linesCount ->
                it.maxLines = linesCount
                it.minLines = linesCount
            }
        }
    )
}

private fun trimSpacesFilter(content: HtmlSpannableContent): HtmlSpannableContent? {
    return when (content) {
        is HtmlSpannableContent.RegularTextHtmlSpannableContent -> {
            val newSpanned = content.rContentSpanned.trim(' ', '\n').toSpanned()
            if (newSpanned.isEmpty()) null
            else content.copy(rContentSpanned = newSpanned)
        }
        is HtmlSpannableContent.ImageHtmlSpannableContent -> {
            if (content.imageUrl.isNullOrEmpty().not()) content
            else null
        }
    }
}

@Composable
fun dpToSp(dp: Dp) = with(LocalDensity.current) { dp.toSp() }

@Composable
fun spToPx(sp: TextUnit) = with(LocalDensity.current) { sp.toPx() }

@Composable
private fun LoadImageAndResizePxAsDp(
    context: Context,
    imageUrl: String,
    resultBitmap: MutableState<Bitmap?>
) {
    val target = SizePxAsDpTarget(LocalDensity.current)
    val imageRequest = ImageRequest.Builder(context)
        .data(imageUrl)
        .size(OriginalSize)
        .target(target)
        .build()

    LaunchedEffect(imageUrl) {
        CoroutineScope(Dispatchers.IO).launch {
            val imageResponse = Coil.imageLoader(context).execute(imageRequest)

            if (imageResponse !is SuccessResult) return@launch
            val resizedBitmap = target.resizedBitmap
            withContext(Dispatchers.Main) {
                resultBitmap.value = resizedBitmap
            }
        }
    }
}

internal class SizePxAsDpTarget(private val localDensity: Density) : Target {
    var resizedBitmap: Bitmap? = null

    override fun onSuccess(result: Drawable) {
        val originalBitmap = (result as? BitmapDrawable)?.bitmap ?: return
        val resizedWidth = with(localDensity) { originalBitmap.width.dp.toPx().toInt() }
        val resizedHeight = with(localDensity) { originalBitmap.height.dp.toPx().toInt() }
        resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, resizedWidth, resizedHeight, false)
    }
}

@Composable
fun SpannableHtmlContent(
    modifier: Modifier = Modifier,
    content: String,
    textColor: Color,
    linkTextColor: Color = textColor,
    totalMaxLinesCount: Int? = null,
    textAlign: TextAlign? = null,
    textSize: TextUnit? = null,
    contentOptions: SpannableHtmlContentOptions = SpannableHtmlContentOptions(),
    onClick: (HtmlSpannableContent) -> Unit = { _ -> },
    isBold: Boolean = false,
    lineSpacing: Float? = null
) {
    val spannableContents = HtmlSpannableContentFactory.createFromHtmlString(content).let {
        var seq = it.asSequence()
        if (contentOptions.trimSpaces) {
            seq = seq.mapNotNull { trimSpacesFilter(it) }
        }
        if (contentOptions.showImages.not()) {
            seq = seq.filter { it !is HtmlSpannableContent.ImageHtmlSpannableContent }
        }
        return@let seq.toList()
    }
    Column(modifier) {
        var linesCountLeft = totalMaxLinesCount
        spannableContents.forEach { content ->
            when (content) {
                is HtmlSpannableContent.RegularTextHtmlSpannableContent -> {
                    SpannableHtmlText(
                        modifier = Modifier.fillMaxWidth(),
                        spanned = content.rContentSpanned,
                        textColor = textColor,
                        linkTextColor = linkTextColor,
                        linesCount = totalMaxLinesCount?.let { maxLines ->
                            if (linesCountLeft!! >= maxLines) maxLines else linesCountLeft
                        },
                        textSize = textSize,
                        isBold = isBold,
                        lineSpacing = lineSpacing,
                        textAlign = textAlign,
                        onClick = { onClick(content) }
                    )
                    if (linesCountLeft != null) linesCountLeft -= totalMaxLinesCount ?: 0
                }
                is HtmlSpannableContent.ImageHtmlSpannableContent -> {
                    val resultBitmap = remember { mutableStateOf<Bitmap?>(null) }
                    val imageUrl = content.imageUrl ?: return

                    LoadImageAndResizePxAsDp(LocalContext.current, imageUrl, resultBitmap)

                    resultBitmap.value?.let { bitmap ->
                        Image(
                            bitmap.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.wrapContentSize(),
                            contentScale = ContentScale.Inside,
                            alignment = contentOptions.imagesHorizontalAlignment
                        )
                    }
                }
            }
        }
    }
}

object HtmlSpannableContentFactory {

    private val spanTypesOfInterest = setOf<Class<CharacterStyle>>(
        ImageSpan::class.java as Class<CharacterStyle>,
    )

    fun createFromHtmlString(content: String): List<HtmlSpannableContent> {
        val spannedValuesWithStyles = mutableMapOf<Spanned, CharacterStyle?>()
        val fullSpanned = Html.fromHtml(content)
        var currentSpanned = fullSpanned

        fullSpanned.getSpans(0, fullSpanned.length, CharacterStyle::class.java)
            .filter { spanTypesOfInterest.contains<Class<CharacterStyle>>(it.javaClass) }
            .let { spansOfInterest ->
                val sorted = spansOfInterest.sortedBy { fullSpanned.getSpanStart(it) }
                sorted.forEach {
                    val startIndex = currentSpanned.getSpanStart(it)
                    val endIndex = currentSpanned.getSpanEnd(it)
                    spannedValuesWithStyles.put(currentSpanned.subSequence(0, startIndex).toSpanned(), null) // There was nothing interesting
                    spannedValuesWithStyles.put(currentSpanned.subSequence(startIndex, endIndex).toSpanned(), it) // There is interesting span
                    currentSpanned = currentSpanned.subSequence(endIndex, currentSpanned.length).toSpanned()
                }
                spannedValuesWithStyles.put(currentSpanned, null) // And don't forget about the remaining part
            }

        return spannedValuesWithStyles.filter { it.key.isNotEmpty() }.map { mapSpannedWithStyleToContentPojo(it) }
    }

    private fun mapSpannedWithStyleToContentPojo(source: Map.Entry<Spanned, CharacterStyle?>): HtmlSpannableContent
            = when(source.value) {
        is ImageSpan -> HtmlSpannableContent.ImageHtmlSpannableContent(source.key)
        else -> HtmlSpannableContent.RegularTextHtmlSpannableContent(source.key)
    }
}