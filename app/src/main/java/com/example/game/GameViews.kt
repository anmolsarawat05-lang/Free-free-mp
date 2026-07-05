package com.example.game

import android.graphics.PointF
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import kotlinx.coroutines.delay
import kotlin.math.*

@Composable
fun GameAppContent(viewModel: GameViewModel) {
    val stats by viewModel.playerStats.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color(0xFF0F1215) // Dark combat tactical canvas
    ) {
        Crossfade(targetState = viewModel.currentScreen, label = "ScreenTransition") { screen ->
            when (screen) {
                GameScreen.SPLASH -> SplashScreen(viewModel)
                GameScreen.LOBBY -> LobbyScreen(viewModel, stats)
                GameScreen.CUSTOMIZE -> CharacterHubScreen(viewModel, stats)
                GameScreen.STORE -> StoreScreen(viewModel, stats)
                GameScreen.BATTLEPASS -> BattlePassScreen(viewModel, stats)
                GameScreen.MATCH -> MatchScreen(viewModel)
                GameScreen.SUMMARY -> MatchSummaryScreen(viewModel, stats)
            }
        }
    }
}

@Composable
fun SplashScreen(viewModel: GameViewModel) {
    var progress by remember { mutableStateOf(0f) }

    LaunchedEffect(Unit) {
        val animationSpec = tween<Float>(durationMillis = 2800, easing = FastOutSlowInEasing)
        animate(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = animationSpec
        ) { value, _ ->
            progress = value
        }
        viewModel.setScreen(GameScreen.LOBBY)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // High quality generated banner
        Image(
            painter = painterResource(id = R.drawable.img_splash_banner_1783221483653),
            contentDescription = "Game Banner Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Dark Gradient overlay for UI legibility
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            Color(0x99000000),
                            Color(0xFF0F1215)
                        )
                    )
                )
        )

        // Title and progress
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SURVIVAL STRIKE",
                color = Color(0xFFFF6D00), // Glowing tactical orange
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("splash_title")
            )
            
            Text(
                text = "50-PLAYER BATTLE ROYALE SIMULATOR",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 4.dp, bottom = 24.dp)
            )

            // Loading bar
            Box(
                modifier = Modifier
                    .width(320.dp)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(progress)
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFFFF3D00), Color(0xFFFFD600))
                            )
                        )
                )
            }

            Text(
                text = "Optimizing shaders & populating battleground... ${(progress * 100).toInt()}%",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

@Composable
fun LobbyScreen(viewModel: GameViewModel, stats: PlayerStats) {
    var activeTab by remember { mutableStateOf("Lobby") }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF16191E),
                contentColor = Color.White,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == "Lobby",
                    onClick = { activeTab = "Lobby"; viewModel.setScreen(GameScreen.LOBBY) },
                    icon = { Icon(Icons.Filled.SportsEsports, contentDescription = "Lobby") },
                    label = { Text("Lobby") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF60A5FA),
                        selectedTextColor = Color(0xFF60A5FA),
                        indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "Characters",
                    onClick = { activeTab = "Characters"; viewModel.setScreen(GameScreen.CUSTOMIZE) },
                    icon = { Icon(Icons.Filled.People, contentDescription = "Characters") },
                    label = { Text("Characters") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF60A5FA),
                        selectedTextColor = Color(0xFF60A5FA),
                        indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "Store",
                    onClick = { activeTab = "Store"; viewModel.setScreen(GameScreen.STORE) },
                    icon = { Icon(Icons.Filled.ShoppingCart, contentDescription = "Store") },
                    label = { Text("Armory Store") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF60A5FA),
                        selectedTextColor = Color(0xFF60A5FA),
                        indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
                NavigationBarItem(
                    selected = activeTab == "BattlePass",
                    onClick = { activeTab = "BattlePass"; viewModel.setScreen(GameScreen.BATTLEPASS) },
                    icon = { Icon(Icons.Filled.Star, contentDescription = "BattlePass") },
                    label = { Text("Battle Pass") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF60A5FA),
                        selectedTextColor = Color(0xFF60A5FA),
                        indicatorColor = Color(0xFF3B82F6).copy(alpha = 0.15f),
                        unselectedIconColor = Color.White.copy(alpha = 0.5f),
                        unselectedTextColor = Color.White.copy(alpha = 0.5f)
                    )
                )
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // Header Stats bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16191E))
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Profile & Rank
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF4F46E5))))
                            .border(1.5.dp, Color.White.copy(alpha = 0.2f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "XP",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Operator_You", color = Color.White, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(Color(0xFFFF6D00).copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = stats.getRankName().uppercase(),
                                    color = Color(0xFFFFD600),
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            // Mini progress bar from design
                            Box(
                                modifier = Modifier
                                    .width(64.dp)
                                    .height(4.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.75f)
                                        .background(Color(0xFF60A5FA))
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "LV.${stats.battlePassLevel + 10}",
                                color = Color(0xFF60A5FA),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Currency & Settings container
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50.dp))
                            .background(Color.Black.copy(alpha = 0.4f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(50.dp))
                            .padding(horizontal = 14.dp, vertical = 6.dp)
                    ) {
                        // Gold
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("✦", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${stats.gold}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                        
                        // Separator
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .height(14.dp)
                                .width(1.dp)
                                .background(Color.White.copy(alpha = 0.15f))
                        )

                        // Diamonds
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("◆", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("${stats.diamonds}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))
                    
                    // Settings Button
                    IconButton(
                        onClick = {
                            viewModel.graphicsSetting = when (viewModel.graphicsSetting) {
                                "Low" -> "Medium"
                                "Medium" -> "High"
                                else -> "Low"
                            }
                            viewModel.triggerToast("Graphics preset updated to: ${viewModel.graphicsSetting}")
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(10.dp))
                    ) {
                        Icon(Icons.Filled.Settings, contentDescription = "Graphics settings", tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    }
                }
            }

            // Main body area (Split into character visual display + match stats panels)
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Left character showcase
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF1C1F26))
                        .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "EQUIPPED CHARACTER",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                viewModel.selectedCharacterId.displayName,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Character active preview (Sleek sci-fi rings)
                        Box(
                            modifier = Modifier
                                .size(140.dp)
                                .drawBehind {
                                    drawCircle(
                                        color = viewModel.activeSkinColor.copy(alpha = 0.15f),
                                        radius = size.minDimension / 2,
                                        style = Stroke(width = 8f)
                                    )
                                    drawCircle(
                                        color = viewModel.activeSkinColor,
                                        radius = size.minDimension / 2.5f,
                                        style = Stroke(
                                            width = 2f,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                                        )
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Filled.AccessibilityNew,
                                contentDescription = "Character Model",
                                tint = viewModel.activeSkinColor,
                                modifier = Modifier.size(70.dp)
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            // Skill info card
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Filled.OfflineBolt, contentDescription = "Skill", tint = Color(0xFFFF6D00), modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            viewModel.selectedCharacterId.skillName,
                                            color = Color(0xFFFF6D00),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        viewModel.selectedCharacterId.skillDesc,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                            }

                            // Map Selection sub-card from Design
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text(
                                        "MAP SELECTION",
                                        color = Color(0xFF60A5FA),
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.padding(bottom = 2.dp)
                                    )
                                    Text(
                                        "NEON FRONTIER",
                                        color = Color.White,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 12.sp,
                                        style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                                    )
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF22C55E))
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            "CLASSIC • SOLO ARENA",
                                            color = Color.White.copy(alpha = 0.5f),
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(20.dp))

                // Right tactical console & launch pad
                Column(
                    modifier = Modifier
                        .weight(1.8f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Match History Quick Stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "CAREER SURVIVAL RECORDS",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${stats.totalMatches}", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text("Matches", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${stats.booyahs}", color = Color(0xFF00E676), fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text("BOOYAHs", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("${stats.totalKills}", color = Color(0xFFFF3D00), fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text("Kills", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    val winrate = if (stats.totalMatches > 0) (stats.booyahs * 100 / stats.totalMatches) else 0
                                    Text("$winrate%", color = Color(0xFF00E5FF), fontSize = 22.sp, fontWeight = FontWeight.Black)
                                    Text("Win Rate", color = Color.White.copy(alpha = 0.6f), fontSize = 11.sp)
                                }
                            }
                        }
                    }

                    // Daily missions scrollable inside lobby
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(vertical = 14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                "ACTIVE DAILY OPERATIONS",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                items(viewModel.dailyMissions) { mission ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFF16191E))
                                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.03f)), RoundedCornerShape(8.dp))
                                            .padding(horizontal = 10.dp, vertical = 6.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(mission.description, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("Progress: ${mission.progress}/${mission.target}", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                                        }
                                        if (mission.isClaimed) {
                                            Text("CLAIMED", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        } else if (mission.progress >= mission.target) {
                                            Button(
                                                onClick = { viewModel.claimMissionReward(mission.id) },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                modifier = Modifier.height(28.dp)
                                            ) {
                                                Text("CLAIM +${mission.rewardGold}G", color = Color.Black, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                            }
                                        } else {
                                            Text("+${mission.rewardGold}G", color = Color(0xFFFFD600), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // START MATCHPAD (Massive glowing Action button!)
                    Button(
                        onClick = { viewModel.startMatchmaking() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .testTag("start_match_button")
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(Color(0xFFEAB308), Color(0xFFEA580C))
                                )
                            )
                            .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)), RoundedCornerShape(16.dp)),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent
                        ),
                        contentPadding = PaddingValues()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(Icons.Filled.PlayArrow, contentDescription = "Play", tint = Color.Black, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("BATTLE ROYALE ARENA", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Black, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                                Text("START SOLOS SIMULATION", color = Color.Black.copy(alpha = 0.8f), fontSize = 10.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CharacterHubScreen(viewModel: GameViewModel, stats: PlayerStats) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16191E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.setScreen(GameScreen.LOBBY) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("CHARACTERS LAB", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                // Gold display
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("✦", color = Color(0xFFF59E0B), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${stats.gold}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "SELECT AN OPERATOR TO DEPLOY",
                color = Color(0xFF60A5FA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Grid of characters
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Characters List Left
                Column(
                    modifier = Modifier
                        .weight(1.3f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CharacterId.values().forEach { char ->
                        val isEquipped = stats.equippedCharacter == char.idName
                        val isUnlocked = stats.unlockedCharacters.split(",").contains(char.idName)

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (isUnlocked) {
                                        viewModel.equipCharacter(char)
                                    } else {
                                        val success = viewModel.unlockCharacter(char, 2000)
                                        if (success) {
                                            viewModel.triggerToast("Unlocked operator: ${char.displayName}")
                                        } else {
                                            viewModel.triggerToast("Insufficient Gold! Need 2,000G")
                                        }
                                    }
                                }
                                .border(
                                    if (isEquipped) BorderStroke(1.5.dp, Color(0xFF60A5FA)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                    RoundedCornerShape(12.dp)
                                ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isEquipped) Color(0xFF16191E) else Color(0xFF1C1F26)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(12.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(45.dp)
                                        .clip(CircleShape)
                                        .background(if (isUnlocked) Color(0xFF3B82F6) else Color(0xFF475569)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        if (isUnlocked) Icons.Filled.Person else Icons.Filled.Lock,
                                        contentDescription = "Lock",
                                        tint = Color.White
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(char.displayName, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        if (isEquipped) "ACTIVE DUTY" else if (isUnlocked) "UNLOCKED - TAP TO EQUIP" else "LOCKED - UNLOCK FOR 2,000G",
                                        color = if (isEquipped) Color(0xFF60A5FA) else Color.White.copy(alpha = 0.5f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }
                        }
                    }
                }

                // Selected Details Card Right
                Card(
                    modifier = Modifier
                        .weight(1.7f)
                        .fillMaxHeight(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                ) {
                    Column(
                        modifier = Modifier
                            .padding(20.dp)
                            .fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "TACTICAL OVERVIEW",
                            color = Color.White.copy(alpha = 0.5f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )

                        Icon(
                            Icons.Filled.AccessibilityNew,
                            contentDescription = "Details model",
                            tint = if (stats.unlockedCharacters.split(",").contains(viewModel.selectedCharacterId.idName)) viewModel.activeSkinColor else Color.Gray,
                            modifier = Modifier.size(100.dp)
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(viewModel.selectedCharacterId.displayName, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Black)
                            Spacer(modifier = Modifier.height(6.dp))
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF16191E)),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("ACTIVE ABILITY:", color = Color(0xFFFF6D00), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        viewModel.selectedCharacterId.skillName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        viewModel.selectedCharacterId.skillDesc,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                    
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.1f)))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    Text("PASSIVE ABILITY:", color = Color(0xFF00E5FF), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(
                                        viewModel.selectedCharacterId.passiveName,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        viewModel.selectedCharacterId.passiveDesc,
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = { viewModel.setScreen(GameScreen.LOBBY) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("CONFIRM OPERATOR SELECT", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreScreen(viewModel: GameViewModel, stats: PlayerStats) {
    val weaponSkins = listOf(
        Triple("plasma_cyan", Color(0xFF00E5FF), 50),
        Triple("toxic_green", Color(0xFF00E676), 80),
        Triple("neon_orange", Color(0xFFFF6D00), 120)
    )

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16191E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.setScreen(GameScreen.LOBBY) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("TACTICAL ARMORY STORE", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                // Diamonds display
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50.dp))
                        .background(Color.Black.copy(alpha = 0.4f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)), RoundedCornerShape(50.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("◆", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${stats.diamonds}", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                }
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                "GLOWING CHROME TRACER SKINS (UNLOCK WITH DIAMONDS)",
                color = Color(0xFF60A5FA),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                weaponSkins.forEach { skin ->
                    val skinCode = skin.first
                    val skinColor = skin.second
                    val cost = skin.third
                    val isOwned = stats.ownedSkins.split(",").contains(skinCode)
                    val isEquipped = stats.activeWeaponSkin == skinCode

                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(0.85f)
                            .border(
                                if (isEquipped) BorderStroke(1.5.dp, skinColor) else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                RoundedCornerShape(16.dp)
                            ),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1F26)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(16.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                skinCode.replace("_", " ").uppercase(),
                                color = skinColor,
                                fontWeight = FontWeight.Black,
                                fontSize = 16.sp
                            )

                            // Gun visual mockup with custom tracer paint glowing effect
                            Box(
                                modifier = Modifier
                                    .size(100.dp)
                                    .drawBehind {
                                        drawRoundRect(
                                            color = skinColor.copy(alpha = 0.1f),
                                            size = size,
                                            cornerRadius = CornerRadius(20f, 20f)
                                        )
                                        drawCircle(
                                            color = skinColor,
                                            radius = 15f,
                                            center = Offset(size.width/2f, size.height/2f),
                                            style = Stroke(width = 4f)
                                        )
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Filled.OfflineBolt,
                                    contentDescription = "Gun skin",
                                    tint = skinColor,
                                    modifier = Modifier.size(50.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Bullet Tracer FX: YES", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                                Text("Gun Paint Glow: YES", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
                            }

                            Button(
                                onClick = {
                                    val success = viewModel.purchaseWeaponSkin(skinCode, skinColor, cost)
                                    if (success) {
                                        viewModel.triggerToast("Applied Skin: ${skinCode.replace("_", " ").uppercase()}")
                                    } else {
                                        viewModel.triggerToast("Insufficient Diamonds! Need $cost")
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = skinColor),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isEquipped) "EQUIPPED" else if (isOwned) "EQUIP" else "BUY ($cost 💎)",
                                    color = Color.Black,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BattlePassScreen(viewModel: GameViewModel, stats: PlayerStats) {
    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF16191E))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { viewModel.setScreen(GameScreen.LOBBY) }) {
                    Icon(Icons.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("BATTLE PASS PROGRESS", color = Color.White, fontWeight = FontWeight.Black, fontSize = 18.sp, style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic))
                Spacer(modifier = Modifier.width(40.dp))
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Level Badge
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(Color(0xFF3B82F6), Color(0xFF4F46E5))))
                    .border(BorderStroke(2.dp, Color.White.copy(alpha = 0.2f)), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LVL", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                    Text("${stats.battlePassLevel}", color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Black)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text("BATTLE PASS ROAD TO GLORY", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(
                "Earn battlepass XP in survival matches to unlock premium rank rewards.",
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )

            // Progress bar
            Spacer(modifier = Modifier.height(20.dp))
            Column(modifier = Modifier.width(360.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Current Level XP: ${stats.battlePassXP}/500", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                    Text("Milestone target: +50 💎", color = Color(0xFF60A5FA), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.1f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(stats.battlePassXP / 500f)
                            .background(Color(0xFF60A5FA))
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Level track cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                for (i in 1..4) {
                    val isUnlocked = stats.battlePassLevel >= i
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .height(100.dp)
                            .border(
                                if (isUnlocked) BorderStroke(1.5.dp, Color(0xFF60A5FA)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
                                RoundedCornerShape(12.dp)
                            ),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isUnlocked) Color(0xFF1B2A38) else Color(0xFF1C1F26)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .padding(10.dp)
                                .fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("LEVEL $i", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                            Icon(
                                if (isUnlocked) Icons.Filled.CheckCircle else Icons.Filled.Lock,
                                contentDescription = "Lock",
                                tint = if (isUnlocked) Color(0xFF60A5FA) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                when (i) {
                                    1 -> "+300 Gold"
                                    2 -> "+50 Diamonds"
                                    3 -> "+1000 Gold"
                                    else -> "Tracer Skin Unlock"
                                },
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MatchScreen(viewModel: GameViewModel) {
    val scope = rememberCoroutineScope()
    val user = viewModel.engine.players.firstOrNull { it.id == "user" } ?: return

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // PARACHUTING PHASE VIEWPORT
        if (user.isParachuting) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF263238)) // atmospheric high-altitude slate blue
            ) {
                // Render stylized terrain map underneath gliding plane
                drawRect(Color(0xFF1B5E20), size = size) // dense grass layer
                
                // Draw river ribbons
                val riverPath = Path().apply {
                    moveTo(0f, size.height * 0.4f)
                    quadraticTo(size.width * 0.5f, size.height * 0.2f, size.width, size.height * 0.7f)
                }
                drawPath(riverPath, color = Color(0xFF0277BD), style = Stroke(width = 80f))

                // Draw playzone bounds on ground
                val zoneScaleX = size.width / viewModel.engine.mapWidth
                val zoneScaleY = size.height / viewModel.engine.mapHeight
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                    radius = viewModel.engine.playzone.currentRadius * zoneScaleX,
                    center = Offset(viewModel.engine.playzone.currentX * zoneScaleX, viewModel.engine.playzone.currentY * zoneScaleY),
                    style = Stroke(width = 4f)
                )

                // Draw supply plane path line
                drawLine(
                    color = Color.White.copy(alpha = 0.4f),
                    start = Offset(100f * zoneScaleX, 100f * zoneScaleY),
                    end = Offset(1900f * zoneScaleX, 1900f * zoneScaleY),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), 0f)
                )

                // Draw aircraft if active
                if (viewModel.engine.planeActive) {
                    drawCircle(
                        color = Color(0xFFFFD54F),
                        radius = 20f,
                        center = Offset(viewModel.engine.planeX * zoneScaleX, viewModel.engine.planeY * zoneScaleY)
                    )
                }

                // Draw current parachuter landing targets
                viewModel.engine.players.forEach { p ->
                    if (p.isAlive && p.isParachuting && p.parachuteAltitude < 1000f) {
                        drawCircle(
                            color = if (p.id == "user") Color(0xFF00E676) else Color(0xFFFF3D00).copy(alpha = 0.7f),
                            radius = 8f,
                            center = Offset(p.x * zoneScaleX, p.y * zoneScaleY)
                        )
                    }
                }
            }

            // PARACHUTING OVERLAYS
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (user.parachuteAltitude >= 1000f) {
                    Text(
                        "ABOARD SUPPLY TRANSPORT PLANE",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.SansSerif
                    )
                    Text(
                        "TAP EJECT TO DEPLOY PARACHUTE OVER TARGET LANDMARKS",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp, bottom = 20.dp)
                    )

                    Button(
                        onClick = { viewModel.ejectFromPlane() },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                        modifier = Modifier
                            .height(55.dp)
                            .width(200.dp)
                            .testTag("eject_button")
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.AirplanemodeInactive, contentDescription = "Eject", tint = Color.White)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("EJECT NOW", color = Color.White, fontWeight = FontWeight.Black, fontSize = 16.sp)
                        }
                    }
                } else {
                    // Descending phase steer controls
                    Text(
                        "GLIDING TO SURFACE",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        "Altitude: ${user.parachuteAltitude.toInt()}m",
                        color = Color(0xFFFFD600),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        "Drag Screen to Steer Glide Path",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 11.sp
                    )
                }
            }
        } else {
            // GROUND COMBAT BATTLEGROUND VIEWPORT
            var cameraModeFPV by remember { mutableStateOf(false) } // FPV zoom vs Standard TPV

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            // Glide / Walk steering angle calculation
                            val angleRad = atan2(dragAmount.y, dragAmount.x)
                            viewModel.fireAngle = angleRad * (180f / PI).toFloat()
                        }
                    }
            ) {
                // Battle field rendering canvas
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val camX = user.x
                    val camY = user.y
                    
                    // Zoom multiplier depending on camera mode
                    val viewScale = if (cameraModeFPV) 1.6f else 1.0f
                    val halfW = size.width / 2f
                    val halfH = size.height / 2f

                    // Renders tactical grid
                    drawBattlegroundGrid(camX, camY, halfW, halfH, viewScale)

                    // Draw landmarks & structural zones
                    viewModel.engine.landmarks.forEach { landmark ->
                        val localX = (landmark.x - camX) * viewScale + halfW
                        val localY = (landmark.y - camY) * viewScale + halfH
                        
                        // Draw compound footprint
                        drawRect(
                            color = Color(0xFF1E2429),
                            topLeft = Offset(localX - 100f * viewScale, localY - 100f * viewScale),
                            size = Size(200f * viewScale, 200f * viewScale)
                        )
                        // Label
                        drawCircle(
                            color = landmark.color.copy(alpha = 0.3f),
                            radius = 60f * viewScale,
                            center = Offset(localX, localY)
                        )
                    }

                    // Draw Floor Loot items
                    viewModel.engine.lootItems.forEach { loot ->
                        val lx = (loot.x - camX) * viewScale + halfW
                        val ly = (loot.y - camY) * viewScale + halfH
                        
                        if (lx >= 0 && lx <= size.width && ly >= 0 && ly <= size.height) {
                            drawCircle(
                                color = when (loot.type) {
                                    LootType.WEAPON -> Color(0xFFFFD54F)
                                    LootType.MEDKIT -> Color(0xFF81C784)
                                    LootType.AMMO -> Color(0xFF64B5F6)
                                    else -> Color(0xFFBA68C8)
                                },
                                radius = 7f * viewScale,
                                center = Offset(lx, ly)
                            )
                        }
                    }

                    // Draw Gloo Wall barriers
                    viewModel.engine.glooWalls.forEach { gloo ->
                        val gx = (gloo.x - camX) * viewScale + halfW
                        val gy = (gloo.y - camY) * viewScale + halfH
                        
                        if (gx >= -50 && gx <= size.width + 50 && gy >= -50 && gy <= size.height + 50) {
                            rotate(gloo.angle, pivot = Offset(gx, gy)) {
                                drawRoundRect(
                                    color = Color(0xCC00E5FF),
                                    topLeft = Offset(gx - 25f * viewScale, gy - 6f * viewScale),
                                    size = Size(50f * viewScale, 12f * viewScale),
                                    cornerRadius = CornerRadius(4f * viewScale)
                                )
                            }
                        }
                    }

                    // Draw drivable Vehicles
                    viewModel.engine.vehicles.forEach { veh ->
                        val vx = (veh.x - camX) * viewScale + halfW
                        val vy = (veh.y - camY) * viewScale + halfH
                        
                        if (vx >= -50 && vx <= size.width + 50 && vy >= -50 && vy <= size.height + 50) {
                            rotate(veh.angle, pivot = Offset(vx, vy)) {
                                drawRect(
                                    color = veh.type.color,
                                    topLeft = Offset(vx - 22f * viewScale, vy - 12f * viewScale),
                                    size = Size(44f * viewScale, 24f * viewScale)
                                )
                                // windshield
                                drawRect(
                                    color = Color(0xCCFFFFFF),
                                    topLeft = Offset(vx + 4f * viewScale, vy - 9f * viewScale),
                                    size = Size(10f * viewScale, 18f * viewScale)
                                )
                            }
                        }
                    }

                    // Draw footprint trails if user is HUNTER (Scout)
                    val userPlayer = viewModel.engine.players.firstOrNull { it.id == "user" }
                    if (userPlayer != null && userPlayer.characterId == CharacterId.HUNTER) {
                        viewModel.engine.players.forEach { other ->
                            if (other.id != "user" && other.isAlive && !other.isParachuting) {
                                other.footprintHistory.forEachIndexed { index, pos ->
                                    val fX = (pos.first - camX) * viewScale + halfW
                                    val fY = (pos.second - camY) * viewScale + halfH
                                    if (fX >= 0 && fX <= size.width && fY >= 0 && fY <= size.height) {
                                        // Footprint glow alpha based on age
                                        val alpha = (index + 1) / 9f
                                        drawCircle(
                                            color = Color(0xFFFF3D00).copy(alpha = alpha * 0.6f),
                                            radius = 4f * viewScale,
                                            center = Offset(fX, fY)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Draw Players
                    viewModel.engine.players.forEach { p ->
                        if (p.isAlive && !p.isParachuting) {
                            val px = (p.x - camX) * viewScale + halfW
                            val py = (p.y - camY) * viewScale + halfH
                            
                            if (px >= -50 && px <= size.width + 50 && py >= -50 && py <= size.height + 50) {
                                // Facing angle line indicator
                                val rad = p.angle * (PI / 180f).toFloat()
                                drawLine(
                                    color = Color.White,
                                    start = Offset(px, py),
                                    end = Offset(px + cos(rad) * 22f * viewScale, py + sin(rad) * 22f * viewScale),
                                    strokeWidth = 3f * viewScale
                                )

                                // Player avatar dot
                                drawCircle(
                                    color = if (p.id == "user") Color(0xFF00E676) else Color(0xFFFF1744),
                                    radius = 12f * viewScale,
                                    center = Offset(px, py)
                                )

                                // Draw defensive energy shield bubble around SHIELD operators if active
                                if (p.skillActiveRemaining > 0f && p.characterId == CharacterId.SHIELD) {
                                    drawCircle(
                                        color = Color(0x2200E5FF),
                                        radius = 35f * viewScale,
                                        center = Offset(px, py)
                                    )
                                    drawCircle(
                                        color = Color(0xFF00E5FF),
                                        radius = 35f * viewScale,
                                        center = Offset(px, py),
                                        style = Stroke(width = 2.5f * viewScale)
                                    )
                                }

                                // Draw healing aura ring around MEDIC operators if active
                                if (p.skillActiveRemaining > 0f && p.characterId == CharacterId.MEDIC) {
                                    drawCircle(
                                        color = Color(0x1A00E676),
                                        radius = 60f * viewScale,
                                        center = Offset(px, py)
                                    )
                                    drawCircle(
                                        color = Color(0xAA00E676),
                                        radius = 60f * viewScale,
                                        center = Offset(px, py),
                                        style = Stroke(width = 1.5f * viewScale)
                                    )
                                }

                                // Draw speed trail circle around AERO operators if active
                                if (p.skillActiveRemaining > 0f && p.characterId == CharacterId.AERO) {
                                    drawCircle(
                                        color = Color(0x66FFEA00),
                                        radius = 18f * viewScale,
                                        center = Offset(px, py),
                                        style = Stroke(width = 3f * viewScale)
                                    )
                                }

                                // Draw Eagle Vision reticle on enemies if HUNTER active is running for user
                                if (userPlayer != null && userPlayer.characterId == CharacterId.HUNTER && userPlayer.skillActiveRemaining > 0f) {
                                    if (p.id != "user") {
                                        drawCircle(
                                            color = Color(0xFFFF1744),
                                            radius = 24f * viewScale,
                                            center = Offset(px, py),
                                            style = Stroke(width = 1f * viewScale)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Draw Bullets (Physical tracers!)
                    viewModel.engine.bullets.forEach { bullet ->
                        val bx = (bullet.currentX - camX) * viewScale + halfW
                        val by = (bullet.currentY - camY) * viewScale + halfH
                        
                        if (bx >= 0 && bx <= size.width && by >= 0 && by <= size.height) {
                            drawCircle(
                                color = bullet.color,
                                radius = 4f * viewScale,
                                center = Offset(bx, by)
                            )
                        }
                    }

                    // Draw Playzone storm edges
                    val playzoneLocalX = (viewModel.engine.playzone.currentX - camX) * viewScale + halfW
                    val playzoneLocalY = (viewModel.engine.playzone.currentY - camY) * viewScale + halfH
                    drawCircle(
                        color = Color(0x2200E5FF),
                        radius = viewModel.engine.playzone.currentRadius * viewScale,
                        center = Offset(playzoneLocalX, playzoneLocalY),
                        style = Stroke(width = 60f * viewScale)
                    )
                    drawCircle(
                        color = Color(0xFF00E5FF),
                        radius = viewModel.engine.playzone.currentRadius * viewScale,
                        center = Offset(playzoneLocalX, playzoneLocalY),
                        style = Stroke(width = 4f * viewScale)
                    )
                }

                // RADAR COMPASS HUD (Top Left)
                Box(
                    modifier = Modifier
                        .padding(16.dp)
                        .size(110.dp)
                        .clip(CircleShape)
                        .background(Color(0xCC15191C))
                        .border(1.dp, Color(0xFF22282F), CircleShape)
                        .align(Alignment.TopStart)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val centerX = size.width / 2f
                        val centerY = size.height / 2f
                        
                        // Compass rings
                        drawCircle(Color.White.copy(alpha = 0.1f), radius = centerX * 0.8f, style = Stroke(width = 1f))
                        drawCircle(Color.White.copy(alpha = 0.1f), radius = centerX * 0.5f, style = Stroke(width = 1f))
                        
                        // User indicator
                        drawCircle(Color(0xFF00E676), radius = 4f, center = Offset(centerX, centerY))

                        // Show nearby enemy blips on radar
                        viewModel.engine.players.forEach { other ->
                            if (other.isAlive && other.id != "user" && !other.isParachuting) {
                                val dx = other.x - user.x
                                val dy = other.y - user.y
                                val dist = sqrt(dx*dx + dy*dy)
                                if (dist < 400f) { // radar capacity
                                    val scale = (centerX * 0.9f) / 400f
                                    drawCircle(
                                        color = Color.Red,
                                        radius = 3f,
                                        center = Offset(centerX + dx * scale, centerY + dy * scale)
                                    )
                                }
                            }
                        }

                        // Playzone center arrow
                        val dxZone = viewModel.engine.playzone.currentX - user.x
                        val dyZone = viewModel.engine.playzone.currentY - user.y
                        val distZone = sqrt(dxZone*dxZone + dyZone*dyZone)
                        if (distZone > 0) {
                            val arrowLen = centerX * 0.45f
                            drawLine(
                                color = Color(0xFF00E5FF),
                                start = Offset(centerX, centerY),
                                end = Offset(centerX + (dxZone / distZone) * arrowLen, centerY + (dyZone / distZone) * arrowLen),
                                strokeWidth = 2f
                            )
                        }
                    }
                }

                // DUAL FLIGHT STICKS HUD
                // Left Virtual Joystick
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 50.dp, bottom = 40.dp)
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color(0x6615191C))
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    viewModel.joystickX = 0f
                                    viewModel.joystickY = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    // Limit distance to joystick radius
                                    val radLimit = 65f
                                    val newX = (viewModel.joystickX * radLimit + dragAmount.x).coerceIn(-radLimit, radLimit)
                                    val newY = (viewModel.joystickY * radLimit + dragAmount.y).coerceIn(-radLimit, radLimit)
                                    
                                    viewModel.joystickX = newX / radLimit
                                    viewModel.joystickY = newY / radLimit
                                }
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    // Joystick background rings
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .border(2.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    )
                    // Thumb pad
                    Box(
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (viewModel.joystickX * 45.dp.toPx()).toInt(),
                                    (viewModel.joystickY * 45.dp.toPx()).toInt()
                                )
                            }
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(Color(0xFFFF9100), Color(0xFFFF3D00))
                                )
                            )
                            .border(1.dp, Color.White, CircleShape)
                    )
                }

                // Right HUD shooting triggers & utilities
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 40.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    // Quick stats & weapon reload indicators
                    Row(
                        modifier = Modifier.padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Gloo wall deploy
                        Button(
                            onClick = { viewModel.deployUserGlooWall() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.Shield, contentDescription = "Gloo", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("GLOO (${viewModel.remainingGlooWalls})", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Medkit heal
                        Button(
                            onClick = { viewModel.applyUserMedkit() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E676)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.LocalHospital, contentDescription = "Heal", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("MED (${viewModel.remainingMedkits})", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Active skill
                        Button(
                            onClick = { viewModel.triggerUserSkill() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD600)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.OfflineBolt, contentDescription = "Skill", tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SKILL", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        // Drive / Exit Vehicle
                        Button(
                            onClick = { viewModel.toggleVehicleEntry() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722)),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.height(38.dp)
                        ) {
                            Icon(Icons.Filled.DirectionsCar, contentDescription = "Vehicle", tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (user.drivingVehicleId != null) "EXIT" else "DRIVE", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Firing Pad and HUD keys
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(20.dp)
                    ) {
                        // Movement states (Crouch / Sprint)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            // Sprint toggle
                            IconButton(
                                onClick = { viewModel.isSprinting = !viewModel.isSprinting },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.isSprinting) Color(0xFFFF9100) else Color(0x9915191C))
                            ) {
                                Icon(Icons.Filled.DirectionsRun, contentDescription = "Sprint", tint = Color.White)
                            }
                            
                            // Crouch toggle
                            IconButton(
                                onClick = {
                                    viewModel.isCrouching = !viewModel.isCrouching
                                    if (viewModel.isCrouching) viewModel.isCrawling = false
                                },
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(CircleShape)
                                    .background(if (viewModel.isCrouching) Color(0xFFFFD600) else Color(0x9915191C))
                            ) {
                                Icon(Icons.Filled.KeyboardDoubleArrowDown, contentDescription = "Crouch", tint = Color.White)
                            }
                        }

                        // Massive Fire Touchpad aiming trigger
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(Color(0xCCFF3D00))
                                .border(2.dp, Color.White, CircleShape)
                                .pointerInput(Unit) {
                                    detectDragGestures(
                                        onDragStart = { viewModel.isFiring = true },
                                        onDragEnd = { viewModel.isFiring = false },
                                        onDragCancel = { viewModel.isFiring = false },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            val aimRad = atan2(dragAmount.y, dragAmount.x)
                                            viewModel.fireAngle = aimRad * (180f / PI).toFloat()
                                        }
                                    )
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Whatshot, contentDescription = "Fire", tint = Color.White, modifier = Modifier.size(32.dp))
                                Text("FIRE / AIM", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // TOP BATTLE STATS & FEED OVERLAYS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Match summary feed
                    Column(modifier = Modifier.weight(1.5f)) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xAA15191C)),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.Group, contentDescription = "Players", tint = Color(0xFF00E5FF), modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("ALIVE: ${viewModel.alivePlayersCount}  |  KILLS: ${viewModel.userKills}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                // Next zone shrink time
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    if (viewModel.engine.playzone.isShrinking) "PLAYZONE STORM SHRINKING!" else "Safe Zone Shrinks in: ${viewModel.engine.playzone.secondsUntilShrink}s",
                                    color = if (viewModel.engine.playzone.isShrinking) Color(0xFFFF1744) else Color(0xFFFFD600),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Real-time Kill Feed logs
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            viewModel.engine.killFeed.take(3).forEach { event ->
                                Text(
                                    "[${event.timeFormatted}] ${event.message}",
                                    color = when (event.type) {
                                        MatchEventType.BOOYAH -> Color(0xFF00E676)
                                        MatchEventType.ELIMINATION -> Color(0xFFFF3D00)
                                        else -> Color.White.copy(alpha = 0.7f)
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .background(Color(0x99000000), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    // Weapon select HUD (Center top)
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xCC15191C))
                            .padding(6.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        user.weapons.forEachIndexed { index, weapon ->
                            val isSelected = user.activeWeaponIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) Color(0x33FF6D00) else Color(0xFF22282F))
                                    .clickable { user.activeWeaponIndex = index }
                                    .border(
                                        if (isSelected) BorderStroke(1.5.dp, Color(0xFFFF6D00)) else BorderStroke(0.dp, Color.Transparent),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(weapon.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    Text(
                                        if (weapon.category == WeaponCategory.MELEE) "MELEE" else "${weapon.currentAmmo}/${weapon.maxAmmo}",
                                        color = if (weapon.currentAmmo == 0 && weapon.category != WeaponCategory.MELEE) Color.Red else Color.White.copy(alpha = 0.6f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                    }

                    // Settings & Camera Mode toggles (Top Right)
                    Row(
                        modifier = Modifier.weight(0.8f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // FPV Camera toggle
                        IconButton(
                            onClick = { cameraModeFPV = !cameraModeFPV },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xAA15191C))
                        ) {
                            Icon(
                                if (cameraModeFPV) Icons.Filled.ZoomIn else Icons.Filled.ZoomOutMap,
                                contentDescription = "Camera Toggle",
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        // Give up match
                        IconButton(
                            onClick = {
                                viewModel.engine.matchFinished = true
                                viewModel.isMatchRunning = false
                                viewModel.setScreen(GameScreen.LOBBY)
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color(0xAA15191C))
                        ) {
                            Icon(Icons.Filled.ExitToApp, contentDescription = "Quit Match", tint = Color.Red)
                        }
                    }
                }

                // In-Game floating match toast notifications
                if (viewModel.matchToastMessage.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .offset(y = (-60).dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xEE15191C)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            viewModel.matchToastMessage,
                            color = Color(0xFFFFD600),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MatchSummaryScreen(viewModel: GameViewModel, stats: PlayerStats) {
    val placement = viewModel.engine.userPlacement
    val isVictory = placement == 1

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // High quality background
        Image(
            painter = painterResource(id = R.drawable.img_splash_banner_1783221483653),
            contentDescription = "Splash Banner Background",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Backdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xE60F1215))
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                if (isVictory) "BOOYAH!" else "ELIMINATED",
                color = if (isVictory) Color(0xFF00E676) else Color(0xFFFF3D00),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.SansSerif,
                modifier = Modifier.testTag("summary_title")
            )
            Text(
                if (isVictory) "CHAMPION OF THE ARENA" else "PLACED #$placement OUT OF 50 PLAYERS",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            // Match stats recap cards
            Row(
                modifier = Modifier.width(440.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15191C))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("TOTAL ELIMINATIONS", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text("${viewModel.userKills}", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("+${viewModel.userKills * 25} Gold Reward", color = Color(0xFFFFD600), fontSize = 11.sp)
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF15191C))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("RANK PROGRESS", color = Color.White.copy(alpha = 0.6f), fontSize = 10.sp)
                        val points = (25 - placement) + viewModel.userKills * 8 + (if (isVictory) 50 else 0)
                        Text("${if (points >= 0) "+" else ""}$points RP", color = if (points >= 0) Color(0xFF00E676) else Color.Red, fontSize = 28.sp, fontWeight = FontWeight.Black)
                        Text("Current: ${stats.rankPoints} RP", color = Color.White.copy(alpha = 0.7f), fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Exit button
            Button(
                onClick = { viewModel.setScreen(GameScreen.LOBBY) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF6D00)),
                modifier = Modifier
                    .width(220.dp)
                    .height(50.dp)
                    .testTag("summary_exit_button")
            ) {
                Text("RETURN TO LOBBY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

// Custom Draw Scope helper to render detailed grid terrain elements with coordinates
private fun DrawScope.drawBattlegroundGrid(camX: Float, camY: Float, halfW: Float, halfH: Float, scale: Float) {
    val gridSize = 120f * scale
    val gridW = size.width
    val gridH = size.height
    
    val startX = ((-camX * scale) % gridSize) + (halfW % gridSize) - gridSize
    val startY = ((-camY * scale) % gridSize) + (halfH % gridSize) - gridSize

    // Draw grid mesh lines
    var x = startX
    while (x < gridW + gridSize) {
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(x, 0f),
            end = Offset(x, gridH),
            strokeWidth = 1f
        )
        x += gridSize
    }

    var y = startY
    while (y < gridH + gridSize) {
        drawLine(
            color = Color.White.copy(alpha = 0.04f),
            start = Offset(0f, y),
            end = Offset(gridW, y),
            strokeWidth = 1f
        )
        y += gridSize
    }
}
