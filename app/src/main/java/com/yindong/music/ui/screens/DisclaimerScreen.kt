package com.yindong.music.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 首次启动免责声明与用户协议页面。
 * 用户必须滚动阅读并点击"同意"后才能进入应用。
 */
@Composable
fun DisclaimerScreen(onAccept: () -> Unit, onDecline: () -> Unit = {}) {
    val scrollState = rememberScrollState()
    // 滚动到底部（或接近底部）才允许点击同意
    val canAccept by remember {
        derivedStateOf {
            val maxScroll = scrollState.maxValue
            maxScroll == 0 || scrollState.value >= maxScroll - 100
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .systemBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "用户协议与免责声明",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "请仔细阅读以下内容",
                fontSize = 13.sp,
                color = Color(0xFF666666),
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 声明内容区
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .background(
                        color = Color(0xFFF5F5F5),
                        shape = RoundedCornerShape(12.dp),
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(20.dp)
                ) {
                    DisclaimerSection(
                        title = "一、软件性质说明",
                        content = "本软件（以下简称「云音乐」）是一款开源的个人学习与技术研究项目，仅供个人学习、研究和技术交流使用。本软件不以任何形式进行商业运营，不收取任何费用，不提供任何形式的会员服务。"
                    )

                    DisclaimerSection(
                        title = "二、音乐内容来源",
                        content = "本软件本身不存储、不托管、不上传任何音频文件或音乐资源。所有音乐内容均来源于第三方公开接口（API），本软件仅作为技术工具提供检索与播放功能。所有音乐版权归原权利人所有，本软件及其作者对第三方接口返回的任何内容不享有任何权利，亦不承担任何责任。"
                    )

                    DisclaimerSection(
                        title = "三、用户行为规范",
                        content = "1. 您应当在遵守所在国家或地区法律法规的前提下使用本软件。\n\n2. 您不得将本软件用于任何商业用途，包括但不限于：出售、租赁、分发或以其他方式营利。\n\n3. 您不得利用本软件从事任何侵犯他人知识产权、版权或其他合法权益的行为。\n\n4. 您不得通过本软件大规模下载、爬取或缓存受版权保护的音乐内容。\n\n5. 如果您所在地区的法律禁止使用此类软件，请立即停止使用并卸载本软件。"
                    )

                    DisclaimerSection(
                        title = "四、免责条款",
                        content = "1. 本软件按「现状」（AS IS）提供，不做任何明示或暗示的保证，包括但不限于对适销性、特定用途适用性和非侵权性的保证。\n\n2. 作者不对使用本软件产生的任何直接、间接、附带、特殊、惩罚性或后果性的损害承担责任，包括但不限于：数据丢失、利润损失、业务中断等。\n\n3. 因用户违反法律法规或本协议规定使用本软件而产生的一切法律责任和后果，由用户自行承担，与本软件作者无关。\n\n4. 第三方API的可用性、准确性和合法性由其各自的提供者负责，本软件作者不对其做出任何担保。\n\n5. 本软件可能随时停止维护或更新，作者不承担任何持续提供服务的义务。"
                    )

                    DisclaimerSection(
                        title = "五、版权与知识产权",
                        content = "1. 通过本软件检索和播放的所有音乐作品的版权归原作者、唱片公司及相关权利人所有。\n\n2. 如果您喜欢某首歌曲，请通过正规渠道（如：QQ音乐、网易云音乐、酷我音乐等）购买或支持正版。\n\n3. 若任何权利人认为本软件侵犯了其合法权益，请联系作者，作者将在核实后及时处理。"
                    )

                    DisclaimerSection(
                        title = "六、隐私保护",
                        content = "1. 本软件不收集、不上传任何用户个人信息。\n\n2. 所有用户数据（如播放历史、收藏列表等）均保存在设备本地，卸载后自动清除。\n\n3. 本软件不包含任何用户追踪、广告追踪或数据分析组件。"
                    )

                    DisclaimerSection(
                        title = "七、协议变更",
                        content = "作者保留随时修改本协议的权利。继续使用本软件即视为您同意修改后的协议内容。"
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "点击「同意并继续」即表示您已完整阅读并理解上述全部内容，且自愿承担使用本软件可能产生的一切风险和法律后果。",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE13333),
                        lineHeight = 20.sp,
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 提示滚动
            if (!canAccept) {
                Text(
                    text = "↓ 请阅读完整内容后再同意",
                    fontSize = 12.sp,
                    color = Color(0xFF999999),
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 同意按钮
            Button(
                onClick = onAccept,
                enabled = canAccept,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFE13333),
                    disabledContainerColor = Color(0xFFCCCCCC),
                ),
            ) {
                Text(
                    text = if (canAccept) "同意并继续" else "请阅读完整协议",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (canAccept) Color.White else Color(0xFF999999),
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 不同意按钮
            TextButton(
                onClick = onDecline,
                modifier = Modifier.padding(bottom = 16.dp),
            ) {
                Text(
                    text = "不同意，退出应用",
                    fontSize = 14.sp,
                    color = Color(0xFF999999),
                )
            }
        }
    }
}

@Composable
private fun DisclaimerSection(title: String, content: String) {
    Spacer(modifier = Modifier.height(12.dp))
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF1A1A1A),
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = content,
        fontSize = 14.sp,
        color = Color(0xFF444444),
        lineHeight = 22.sp,
    )
}
