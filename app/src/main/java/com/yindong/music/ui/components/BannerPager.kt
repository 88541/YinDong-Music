package com.yindong.music.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.yindong.music.data.model.Banner
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BannerPager(
    banners: List<Banner>,
    onBannerClick: (Banner) -> Unit = {},
    onPlayAllClick: (Banner) -> Unit = {},
) {
    if (banners.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { banners.size })
    LaunchedEffect(banners.size) {
        while (true) {
            delay(4000L)
            if (banners.isNotEmpty()) {
                val nextPage = (pagerState.currentPage + 1) % banners.size
                pagerState.animateScrollToPage(nextPage)
            }
        }
    }

    val cardColors = listOf(
        Color(0xFFBDD6F5),
        Color(0xFFF5BDC8),
        Color(0xFFBDE8D2),
        Color(0xFFDDC8F5),
        Color(0xFFF5E0BD),
        Color(0xFFC8E8F5),
        Color(0xFFF5C8C8),
    )
    val englishTitles = listOf(
        "Daily\n30",
        "Hot\nChart",
        "New\nHits",
        "For\nYou",
        "Canto\nPop",
        "Best\nHits",
        "TikTok\nHot",
    )

    Column {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(190.dp),
            pageSize = PageSize.Fixed(175.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            pageSpacing = 10.dp,
        ) { page ->
            val banner = banners[page]
            val bgColor = cardColors[page % cardColors.size]
            val engTitle = englishTitles[page % englishTitles.size]

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(18.dp))
                    .background(bgColor)
                    .clickable { onBannerClick(banner) }
                    .padding(14.dp),
            ) {
                // ── Top: English title + Cover ──
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top,
                ) {
                    Text(
                        engTitle,
                        color = Color(0xFF1A1A2E),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        lineHeight = 24.sp,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp),
                    )
                    Box(
                        modifier = Modifier
                            .size(82.dp)
                            .shadow(6.dp, RoundedCornerShape(14.dp))
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color.White.copy(alpha = 0.40f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (banner.imageUrl.isNotEmpty()) {
                            AsyncImage(
                                model = banner.imageUrl,
                                contentDescription = banner.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop,
                            )
                        } else {
                            Icon(
                                Icons.Default.LibraryMusic,
                                contentDescription = null,
                                tint = Color(0xFF1A1A2E).copy(alpha = 0.18f),
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // ── Bottom: Title + subtitle + play ──
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            banner.title,
                            color = Color(0xFF1A1A2E),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (banner.subtitle.isNotEmpty()) {
                            Text(
                                banner.subtitle,
                                color = Color(0xFF1A1A2E).copy(alpha = 0.50f),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.85f))
                            .clickable { onPlayAllClick(banner) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color(0xFF1A1A2E).copy(alpha = 0.75f),
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        // ── Indicator dots ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.Center,
        ) {
            repeat(banners.size) { index ->
                val selected = pagerState.currentPage == index
                val dotWidth = animateDpAsState(
                    targetValue = if (selected) 16.dp else 5.dp,
                    animationSpec = tween(300),
                    label = "dot_w_$index",
                )
                val dotAlpha = animateFloatAsState(
                    targetValue = if (selected) 1f else 0.25f,
                    animationSpec = tween(300),
                    label = "dot_a_$index",
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 2.5.dp)
                        .height(4.dp)
                        .width(dotWidth.value)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0xFF1A1A2E).copy(alpha = dotAlpha.value * 0.35f)),
                )
            }
        }
    }
}
