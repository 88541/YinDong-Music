package com.yindong.music.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yindong.music.ui.navigation.Screen
import com.yindong.music.ui.theme.GlassDarkSurface
import com.yindong.music.ui.theme.GlassLightSurface
import com.yindong.music.ui.theme.NeumorphShadowDark
import com.yindong.music.ui.theme.PrimaryPurple
import com.yindong.music.ui.theme.CardGlassBackgroundDark
import com.yindong.music.ui.theme.CardGlassBackgroundLight
import com.yindong.music.ui.theme.CardGlassBorderDark
import com.yindong.music.ui.theme.CardGlassBorderLight
import com.yindong.music.ui.theme.isDarkTheme

private data class NavItem(val route: String, val label: String, val icon: ImageVector)

@Composable
fun BottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
) {
    val items = listOf(
        NavItem(Screen.Discover.route, "发现", Icons.Default.Explore),
        NavItem(Screen.Playlist.route, "歌单", Icons.Default.LibraryMusic),
        NavItem(Screen.Mine.route, "我的", Icons.Default.PersonOutline),
    )

    val isDark = isDarkTheme()
    val backgroundColor = if (isDark) CardGlassBackgroundDark else CardGlassBackgroundLight
    val borderColor = if (isDark) CardGlassBorderDark else CardGlassBorderLight

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // 液态玻璃底部导航容器
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(backgroundColor)
                .border(1.dp, borderColor, RoundedCornerShape(28.dp))
                .drawBehind {
                    // 顶部高光 - 模拟玻璃反光
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = if (isDark) 0.15f else 0.4f),
                                Color.White.copy(alpha = if (isDark) 0.05f else 0.1f),
                                Color.Transparent
                            ),
                            startY = 0f,
                            endY = this.size.height * 0.3f
                        )
                    )
                    // 底部阴影 - 增强立体感
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = if (isDark) 0.3f else 0.08f)
                            ),
                            startY = this.size.height * 0.7f,
                            endY = this.size.height
                        )
                    )
                    // 内部光晕 - 液态感
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.06f),
                                Color.Transparent
                            ),
                            center = androidx.compose.ui.geometry.Offset(this.size.width * 0.5f, this.size.height * 0.5f),
                            radius = this.size.maxDimension * 0.6f
                        )
                    )
                }
                .navigationBarsPadding(),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                items.forEach { item ->
                    val selected = currentRoute == item.route
                    NavItemComponent(
                        item = item,
                        selected = selected,
                        onClick = { onNavigate(item.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.NavItemComponent(
    item: NavItem,
    selected: Boolean,
    onClick: () -> Unit
) {
    val tint by animateColorAsState(
        targetValue = if (selected) PrimaryPurple else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        animationSpec = tween(300),
        label = "nav_tint"
    )

    val iconSize by animateDpAsState(
        targetValue = if (selected) 28.dp else 24.dp,
        animationSpec = tween(300),
        label = "nav_icon_size"
    )

    val verticalOffset by animateDpAsState(
        targetValue = if (selected) (-2).dp else 0.dp,
        animationSpec = tween(300),
        label = "nav_offset"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .offset(y = verticalOffset),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        // 选中时的发光背景
        if (selected) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                PrimaryPurple.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(iconSize),
                )
            }
        } else {
            Icon(
                imageVector = item.icon,
                contentDescription = item.label,
                tint = tint,
                modifier = Modifier.size(iconSize),
            )
        }

        Spacer(Modifier.height(4.dp))

        Text(
            text = item.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            letterSpacing = 0.5.sp
        )
    }
}
