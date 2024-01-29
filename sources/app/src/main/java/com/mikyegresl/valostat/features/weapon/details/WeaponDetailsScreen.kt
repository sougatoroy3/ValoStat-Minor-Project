package com.mikyegresl.valostat.features.weapon.details

import android.content.res.Configuration
import androidx.activity.addCallback
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Card
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.mikyegresl.valostat.R
import com.mikyegresl.valostat.base.model.ValoStatLocale
import com.mikyegresl.valostat.base.model.weapon.WeaponDto
import com.mikyegresl.valostat.base.model.weapon.skin.WeaponSkinChromaDto
import com.mikyegresl.valostat.base.model.weapon.skin.WeaponSkinDto
import com.mikyegresl.valostat.base.model.weapon.stats.WeaponDamageRangeDto
import com.mikyegresl.valostat.base.model.weapon.stats.WeaponStatsDto
import com.mikyegresl.valostat.common.compose.ShowingErrorState
import com.mikyegresl.valostat.common.compose.ShowingLoadingState
import com.mikyegresl.valostat.common.compose.TopBarBackButton
import com.mikyegresl.valostat.features.video.player.ComposableExoPlayer
import com.mikyegresl.valostat.features.video.player.exoplayer.ExoPlayerConfig
import com.mikyegresl.valostat.features.video.player.exoplayer.PreExecExoPlayerFullScreenListenerImpl
import com.mikyegresl.valostat.features.weapon.details.WeaponDetailsScreenState.WeaponDetailsErrorState
import com.mikyegresl.valostat.features.weapon.details.WeaponDetailsScreenState.WeaponDetailsInGeneralState.WeaponDetailsInDataState
import com.mikyegresl.valostat.features.weapon.details.WeaponDetailsScreenState.WeaponDetailsInGeneralState.WeaponDetailsInFullscreenState
import com.mikyegresl.valostat.features.weapon.details.WeaponDetailsScreenState.WeaponDetailsLoadingState
import com.mikyegresl.valostat.ui.dimen.ElemSize
import com.mikyegresl.valostat.ui.dimen.Padding
import com.mikyegresl.valostat.ui.theme.ValoStatTypography
import com.mikyegresl.valostat.ui.theme.playerBackgroundDark
import com.mikyegresl.valostat.ui.theme.secondaryBackgroundDark
import com.mikyegresl.valostat.ui.theme.secondaryTextDark
import com.mikyegresl.valostat.ui.theme.surfaceDark
import com.mikyegresl.valostat.ui.theme.washWhite
import com.mikyegresl.valostat.ui.widget.gradientModifier
import kotlinx.coroutines.flow.StateFlow
import kotlin.math.roundToInt

private const val TAG = "WEAPON_DETAILS_SCREEN"

data class WeaponDetailsScreenActions(
    val onVideoItemClicked: (WeaponSkinChromaDto) -> Unit,
    val onEnterFullscreen: () -> Unit,
    val onExitFullscreen: () -> Unit,
    val beforeEnterFullscreen: (Long, Boolean) -> Unit,
    val beforeExitFullscreen: (Long, Boolean) -> Unit,
    val onBackPressed: () -> Unit,
    val onSkinLeftFocus: (WeaponSkinChromaDto) -> Unit,
    val onUpdateWeaponDetails: (String, ValoStatLocale) -> Unit
)

@Composable
fun WeaponDetailsScreen(
    weaponId: String,
    state: StateFlow<WeaponDetailsScreenState>,
    locale: ValoStatLocale,
    actions: WeaponDetailsScreenActions
) {
    val detailsState = state.collectAsStateWithLifecycle()
    val exoPlayerFullScreenListener = remember {
        PreExecExoPlayerFullScreenListenerImpl(
            beforeEnterFullScreen = { playbackPosition, playOnInit ->
                actions.beforeEnterFullscreen(playbackPosition, playOnInit)
                true
            },
            beforeExitFullScreen = { playbackPosition, playOnInit ->
                actions.beforeExitFullscreen(playbackPosition, playOnInit)
                true
            },
            enterFullscreenMode = actions.onEnterFullscreen,
            exitFullscreenMode = actions.onExitFullscreen
        )
    }
    LaunchedEffect(weaponId, locale) {
        actions.onUpdateWeaponDetails(weaponId, locale)
    }
    when (val viewState = detailsState.value) {
        is WeaponDetailsLoadingState -> {
            ShowingLoadingState()
        }
        is WeaponDetailsErrorState -> {
            ShowingErrorState(errorMessage = viewState.t.message)
        }
        is WeaponDetailsScreenState.WeaponDetailsInGeneralState -> {
            val backPressedDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
            val currentOrientation = LocalConfiguration.current.orientation
            backPressedDispatcher?.addCallback {
                if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                    exoPlayerFullScreenListener.onExitFullScreen(viewState.playerConfig.playbackPosition, viewState.playerConfig.playOnInit)
                } else {
                    actions.onBackPressed()
                }
            }
            when (viewState) {
                is WeaponDetailsInDataState -> {
                    ShowingWeaponDetailsDataState(
                        modifier = Modifier,
                        state = viewState,
                        onVideoItemClick = actions.onVideoItemClicked,
                        onEnterFullscreen = { playbackPosition, playOnInit ->
                            exoPlayerFullScreenListener.onEnterFullScreen(playbackPosition, playOnInit)
                        },
                        onExitFullscreen = { playbackPosition, playOnInit ->
                            exoPlayerFullScreenListener.onExitFullScreen(playbackPosition, playOnInit)
                        },
                        onBackPressed = actions.onBackPressed,
                        onSkinLeftFocus = actions.onSkinLeftFocus
                    )
                }
                is WeaponDetailsInFullscreenState -> {
                    ShowingWeaponDetailsInFullscreenState(
                        modifier = Modifier.fillMaxSize(),
                        mediaUri = viewState.activeVideoChroma.streamedVideo,
                        playerConfig = viewState.playerConfig,
                        onEnterFullscreen = { playbackPosition, playOnInit ->
                            exoPlayerFullScreenListener.onEnterFullScreen(playbackPosition, playOnInit)
                        },
                        onExitFullscreen = { playbackPosition, playOnInit ->
                            exoPlayerFullScreenListener.onExitFullScreen(playbackPosition, playOnInit)
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ShowingWeaponDetailsDataState(
    modifier: Modifier = Modifier,
    state: WeaponDetailsInDataState,
    onVideoItemClick: (WeaponSkinChromaDto) -> Unit,
    onEnterFullscreen: (Long, Boolean) -> Unit,
    onExitFullscreen: (Long, Boolean) -> Unit,
    onBackPressed: () -> Unit,
    onSkinLeftFocus: (WeaponSkinChromaDto) -> Unit
) {
    val listState = rememberLazyListState()

    val activeVideoChroma = remember { mutableStateOf(state.activeVideoChroma) }
    val lastContinuePlaybackChroma = remember { mutableStateOf<WeaponSkinChromaDto?>(null) }
    val activeVideoChromaIndex = remember { mutableStateOf<Int?>(null) }
    val startScrollIndex = 6
    val playOnInit = if (!state.continuePlayback) true else state.playerConfig.playOnInit

    LaunchedEffect(state.activeVideoChroma) {
        val index = state.skinsWithVideo
            .indexOfFirst { videoChroma -> videoChroma == state.activeVideoChroma }
            .takeIf { it >= 0 }?.let { it + startScrollIndex }
        activeVideoChromaIndex.value = index
        activeVideoChroma.value = state.activeVideoChroma
    }

    LaunchedEffect(true) {
        activeVideoChromaIndex.value?.let { index ->
            if (state.continuePlayback && lastContinuePlaybackChroma.value != state.activeVideoChroma) {
                listState.scrollToItem(index)
                lastContinuePlaybackChroma.value = state.activeVideoChroma
            }
        }
    }

    CompositionLocalProvider(
        LocalOverscrollConfiguration provides null
    ) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                Column(
                    modifier = gradientModifier(
                        40f,
                        .65f to surfaceDark,
                        .65f to secondaryBackgroundDark
                    )
                ) {
                    WeaponDetailsTopBar(
                        title = state.details.displayName,
                        imageSrc = state.details.displayIcon,
                        onBackPressed = onBackPressed
                    )
                    WeaponDescriptionItem(
                        Modifier
                            .fillMaxWidth()
                            .padding(
                                top = Padding.Dp16,
                                start = Padding.Dp32,
                                end = Padding.Dp32
                            ),
                        state.details
                    )
                }
            }
            weaponDamageRanges(stats = state.details.stats).invoke(this)

            weaponSkins(
                chromas = state.skinsWithChromas,
            ).invoke(this)

            weaponVideos(
                state = state,
                chromas = state.skinsWithVideo,
                playOnInit = playOnInit,
                onVideoItemClick = onVideoItemClick,
                onEnterFullscreen = onEnterFullscreen,
                onExitFullscreen = onExitFullscreen,
                onSkinLeftFocus = onSkinLeftFocus
            ).invoke(this)
        }
    }
}

@Composable
fun WeaponDetailsTopBar(
    title: String,
    imageSrc: String,
    onBackPressed: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = Padding.Dp24,
                    start = Padding.Dp24,
                    end = Padding.Dp24
                ),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TopBarBackButton(onBackPressed = onBackPressed)
            Text(
                modifier = Modifier
                    .weight(1f),
                text = title,
                textAlign = TextAlign.Center,
                style = ValoStatTypography.subtitle1
            )
        }
        AsyncImage(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(Padding.Dp24),
            model = imageSrc,
            contentScale = ContentScale.FillWidth,
            contentDescription = title
        )
    }
}

@Composable
fun WeaponDescriptionItem(
    modifier: Modifier = Modifier,
    details: WeaponDto
) {
    Column(
        modifier = modifier
    ) {
        Spacer(Modifier.padding(vertical = Padding.Dp16))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.type)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = stringResource(id = details.category.titleRes),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.creds)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = details.shopData.cost.toString(),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.magazine)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = details.stats.magazineSize.toString(),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.fire_rate)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            //rounds/sec
            Text(
                text = "${details.stats.fireRate}",
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.fire_mode)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = stringResource(id = details.stats.fireMode.titleRes),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.alt_fire_mode)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = stringResource(id = details.stats.altFireType.titleRes),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(id = R.string.wall_penetration)}:",
                style = ValoStatTypography.caption.copy(color = secondaryTextDark)
            )
            Text(
                text = stringResource(id = details.stats.wallPenetration.titleRes),
                style = ValoStatTypography.subtitle2
            )
        }
        Divider(
            modifier = Modifier.padding(vertical = Padding.Dp8),
            color = washWhite,
            thickness = 1.dp
        )
    }
}

fun weaponDamageRanges(
    modifier: Modifier = Modifier,
    stats: WeaponStatsDto
) : LazyListScope.() -> Unit = {
    item {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = Padding.Dp16, horizontal = Padding.Dp32),
            text = "${stringResource(id = R.string.damage)}:",
            style = ValoStatTypography.caption.copy(color = secondaryTextDark),
            textAlign = TextAlign.Center
        )
    }
    item {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = Padding.Dp32),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            DamageRangeHeader()
            stats.damageRanges.forEach {
                Spacer(modifier = Modifier.padding(vertical = Padding.Dp4))
                DamageRangeItem(damage = it)
            }
            Divider(
                modifier = Modifier.padding(vertical = Padding.Dp8),
                color = washWhite,
                thickness = 1.dp
            )
        }
    }
}

@Composable
fun DamageRangeHeader(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.range),
            textAlign = TextAlign.Start,
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.head),
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = stringResource(id = R.string.body),
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            text = stringResource(id = R.string.legs),
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
    }
}

@Composable
fun DamageRangeItem(
    modifier: Modifier = Modifier,
    damage: WeaponDamageRangeDto
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Start,
            text = "${damage.rangeStartMeters}-${damage.rangeEndMeters}",
            style = ValoStatTypography.subtitle2
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = "${damage.headDamage.roundToInt()}",
            style = ValoStatTypography.subtitle2
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            text = "${damage.bodyDamage}",
            style = ValoStatTypography.subtitle2
        )
        Text(
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End,
            text = "${damage.legDamage.roundToInt()}",
            style = ValoStatTypography.subtitle2
        )
    }
}

fun weaponSkins(
    chromas: List<WeaponSkinDto>,
) : LazyListScope.() -> Unit = {

    item {
        Text(
            modifier = Modifier.padding(
                top = Padding.Dp16,
                bottom = Padding.Dp8,
                start = Padding.Dp32,
                end = Padding.Dp32
            ),
            textAlign = TextAlign.Start,
            text = "${stringResource(id = R.string.skins)}:",
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
    }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = Padding.Dp16),
            verticalAlignment = Alignment.CenterVertically
        ) {
            itemsIndexed(chromas) { i, item ->
                WeaponSkinCardItem(
                    skin = item
                )
                if (i != chromas.lastIndex) {
                    Spacer(modifier = Modifier.padding(horizontal = Padding.Dp8))
                }
            }
        }
        Spacer(modifier = Modifier.padding(vertical = Padding.Dp8))
    }
}

@Composable
fun WeaponSkinCardItem(
    modifier: Modifier = Modifier,
    skin: WeaponSkinDto
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    val containerMinWidth by remember {
        mutableStateOf((screenWidth / 1.9).dp)
    }
    val containerMaxWidth by remember {
        mutableStateOf((screenWidth / 1.7).dp)
    }
    var selectedWeaponImage by rememberSaveable {
        mutableStateOf(skin.iconPath)
    }

    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier
                .padding(Padding.Dp4),
            text = skin.displayName,
            style = ValoStatTypography.body1.copy(fontSize = 12.sp),
            maxLines = 1,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis
        )
        Card(
            modifier = modifier
                .widthIn(containerMinWidth, containerMaxWidth)
                .aspectRatio(1.5f),
            shape = RoundedCornerShape(Padding.Dp8),
            border = BorderStroke(1.dp, secondaryTextDark),
            backgroundColor = playerBackgroundDark
        ) {
            SubcomposeAsyncImage(
                modifier = Modifier
                    .weight(3f)
                    .padding(
                        top = Padding.Dp4,
                        start = Padding.Dp16,
                        end = Padding.Dp16
                    )
                    .alpha(75f),
                model = ImageRequest.Builder(LocalContext.current)
                    .data(selectedWeaponImage)
                    .build(),
                loading = {
                    CircularProgressIndicator(
                        modifier = Modifier.wrapContentSize(),
                        color = secondaryTextDark
                    )
                },
                contentScale = ContentScale.Inside,
                contentDescription = skin.displayName
            )
        }
        SkinChromasPanel(
            chromas = skin.chromas,
        ) {
            selectedWeaponImage = it.iconPath
        }
    }
}

@Composable
fun SkinChromasPanel(
    modifier: Modifier = Modifier,
    chromas: List<WeaponSkinChromaDto>,
    onChromaSelected: (WeaponSkinChromaDto) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = Padding.Dp4,
                end = Padding.Dp8,
                top = Padding.Dp4,
                bottom = Padding.Dp4
            ),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        chromas.forEach {
            SkinChromaChip(
                chroma = it,
                onChromaSelected = onChromaSelected
            )
        }
    }
}

@Composable
fun SkinChromaChip(
    chroma: WeaponSkinChromaDto,
    onChromaSelected: (WeaponSkinChromaDto) -> Unit
) {
    AsyncImage(
        modifier = Modifier
            .clickable {
                onChromaSelected(chroma)
            }
            .size(ElemSize.Dp24)
            .padding(Padding.Dp2)
            .clip(CircleShape),
        model = chroma.swatchPath,
        alignment = Alignment.Center,
        contentScale = ContentScale.FillBounds,
        contentDescription = chroma.displayName
    )
    Spacer(modifier = Modifier.padding(horizontal = Padding.Dp4))
}

fun weaponVideos(
    state: WeaponDetailsInDataState,
    chromas: List<WeaponSkinChromaDto>,
    playOnInit: Boolean,
    onVideoItemClick: (WeaponSkinChromaDto) -> Unit,
    onEnterFullscreen: (Long, Boolean) -> Unit,
    onExitFullscreen: (Long, Boolean) -> Unit,
    onSkinLeftFocus: (WeaponSkinChromaDto) -> Unit
): LazyListScope.() -> Unit = {
    item {
        Text(
            modifier = Modifier.padding(
                vertical = Padding.Dp8,
                horizontal = Padding.Dp32
            ),
            textAlign = TextAlign.Start,
            text = "${stringResource(id = R.string.videos)}:",
            style = ValoStatTypography.caption.copy(color = secondaryTextDark)
        )
    }
    chromas.forEachIndexed { _, chroma ->
        item {
            DisposableEffect(
                SkinVideoContainer(
                    state = state,
                    chroma = chroma,
                    playOnInit = playOnInit,
                    onVideoItemClick = onVideoItemClick,
                    onEnterFullscreen = onEnterFullscreen,
                    onExitFullscreen = onExitFullscreen
                )
            ) {
                onDispose {
                    onSkinLeftFocus(chroma)
                }
            }
            Spacer(modifier = Modifier.padding(vertical = Padding.Dp8))
        }
    }
}

@Composable
fun SkinVideoContainer(
    modifier: Modifier = Modifier,
    state: WeaponDetailsInDataState,
    playOnInit: Boolean,
    chroma: WeaponSkinChromaDto,
    onVideoItemClick: (WeaponSkinChromaDto) -> Unit,
    onEnterFullscreen: (Long, Boolean) -> Unit,
    onExitFullscreen: (Long, Boolean) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16 / 9f)
            .padding(horizontal = Padding.Dp32)
            .clickable { onVideoItemClick(chroma) },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            modifier = Modifier
                .padding(Padding.Dp4),
            text = chroma.displayName,
            style = ValoStatTypography.body1.copy(fontSize = 12.sp),
            maxLines = 1,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis
        )

        if (state.activeVideoChroma != chroma) {
            SkinItem(chroma = chroma)
        } else {
            SkinVideoItem(
                modifier = Modifier.fillMaxSize(),
                mediaUri = chroma.streamedVideo,
                playerConfig = state.playerConfig,
                playOnInit = playOnInit,
                onEnterFullscreen = onEnterFullscreen,
                onExitFullscreen = onExitFullscreen
            )
        }
    }
}

@Composable
fun SkinItem(
    modifier: Modifier = Modifier,
    chroma: WeaponSkinChromaDto
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(Padding.Dp8),
        border = BorderStroke(1.dp, secondaryTextDark),
        backgroundColor = playerBackgroundDark
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        top = Padding.Dp4,
                        start = Padding.Dp16,
                        end = Padding.Dp16
                    )
                    .alpha(75f),
                model = chroma.fullRenderPath,
                contentScale = ContentScale.Inside,
                contentDescription = chroma.displayName
            )
            Icon(
                modifier = Modifier.size(ElemSize.Dp48),
                painter = painterResource(id = R.drawable.ic_play_button),
                contentDescription = stringResource(id = R.string.videos)
            )
        }
    }
}

@Composable
fun SkinVideoItem(
    modifier: Modifier = Modifier,
    mediaUri: String,
    playerConfig: ExoPlayerConfig,
    playOnInit: Boolean,
    onEnterFullscreen: (Long, Boolean) -> Unit,
    onExitFullscreen: (Long, Boolean) -> Unit
) {
    ComposableExoPlayer(
        modifier = modifier,
        mediaUri = mediaUri,
        playerConfig = playerConfig.copy(playOnInit = playOnInit),
        onEnterFullscreen = onEnterFullscreen,
        onExitFullscreen = onExitFullscreen
    )
}

@Composable
fun ShowingWeaponDetailsInFullscreenState(
    modifier: Modifier = Modifier,
    mediaUri: String,
    playerConfig: ExoPlayerConfig,
    onEnterFullscreen: (Long, Boolean) -> Unit,
    onExitFullscreen: (Long, Boolean) -> Unit
) {
    ComposableExoPlayer(
        modifier = modifier,
        mediaUri = mediaUri,
        playerConfig = playerConfig,
        onEnterFullscreen = onEnterFullscreen,
        onExitFullscreen = onExitFullscreen
    )
}