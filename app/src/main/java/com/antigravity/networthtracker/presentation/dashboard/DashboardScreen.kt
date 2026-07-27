package com.antigravity.networthtracker.presentation.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.KeyboardArrowDown
import kotlinx.coroutines.launch
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.antigravity.networthtracker.R
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.presentation.components.AssetItemRow
import com.antigravity.networthtracker.presentation.components.BalanceCard
import com.antigravity.networthtracker.presentation.components.CategoryHeader
import com.antigravity.networthtracker.presentation.components.ShimmerLoading
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import com.antigravity.networthtracker.presentation.theme.AccordionExpandedBg
import com.antigravity.networthtracker.presentation.components.getLocalizedNameRes

enum class SortOption {
    VALUE_DESC,    // Büyükten küçüğe değere göre
    VALUE_ASC,     // Küçükten büyüğe değere göre
    ALPHABETICAL,  // Alfabetik sıraya göre (A-Z)
    MANUAL         // Özel Sıralama
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onAddAssetClick: (AssetType?) -> Unit,
    onAssetClick: (Asset) -> Unit,
    onChartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()
    val dailyHistory by viewModel.dailyNetWorthHistory.collectAsState(initial = emptyList())
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Automatically refresh data when screen is opened or app returns to foreground
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.loadDashboardData(isRefresh = true)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val usdRate = remember(state.netWorthResult) {
        state.netWorthResult?.usdTryRate ?: 33.0
    }
    val eurRate = remember(state.netWorthResult) {
        state.netWorthResult?.eurTryRate ?: 36.0
    }

    var sortOption by remember { mutableStateOf(SortOption.VALUE_DESC) }
    var isValuesHidden by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val sortedGroupedAssets = remember(state.assets, sortOption, usdRate, eurRate) {
        val groups = state.assets.groupBy { it.asset.type }
        
        // Her grubun TRY cinsinden toplam değerini önceden hesapla
        val groupValues = groups.mapValues { (_, assetsInGroup) ->
            assetsInGroup.sumOf { calculatedAsset ->
                val asset = calculatedAsset.asset
                val rate = when (asset.currency.uppercase()) {
                    "USD" -> usdRate
                    "EUR" -> eurRate
                    else -> 1.0
                }
                calculatedAsset.currentValue * rate
            }
        }
        
        // Her grubun içindeki elemanları kendi değerlerine göre (TRY cinsine dönüştürerek) büyükten küçüğe sırala
        val sortedGroups = groups.mapValues { (_, assetsInGroup) ->
            assetsInGroup.sortedByDescending { calculatedAsset ->
                val asset = calculatedAsset.asset
                val rate = when (asset.currency.uppercase()) {
                    "USD" -> usdRate
                    "EUR" -> eurRate
                    else -> 1.0
                }
                calculatedAsset.currentValue * rate
            }
        }
        
        sortedGroups.toList().sortedWith { o1, o2 ->
            when (sortOption) {
                SortOption.VALUE_DESC -> {
                    groupValues[o2.first]!!.compareTo(groupValues[o1.first]!!)
                }
                SortOption.VALUE_ASC -> {
                    groupValues[o1.first]!!.compareTo(groupValues[o2.first]!!)
                }
                SortOption.ALPHABETICAL -> {
                    val name1 = context.getString(o1.first.getLocalizedNameRes())
                    val name2 = context.getString(o2.first.getLocalizedNameRes())
                    name1.compareTo(name2, ignoreCase = true)
                }
                SortOption.MANUAL -> 0
            }
        }
    }

    var expandedCategories by remember {
        mutableStateOf(emptySet<AssetType>())
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                actions = {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(36.dp)
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.primary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(
                            onClick = { viewModel.loadDashboardData(isRefresh = true) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = stringResource(id = R.string.desc_refresh),
                                tint = Color.White
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            when {
                !state.isInitialLoadComplete -> {
                    ShimmerLoading()
                }
                state.isLoading -> {
                    ShimmerLoading()
                }
                state.errorMessage != null -> {
                    ErrorState(
                        message = state.errorMessage!!,
                        onRetry = { viewModel.loadDashboardData(isRefresh = false) }
                    )
                }
                state.assets.isEmpty() -> {
                    EmptyState(onAddAssetClick = { onAddAssetClick(null) })
                }
                else -> {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        item {
                            state.netWorthResult?.let { result ->
                                Box(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 0.dp)) {
                                    BalanceCard(
                                        result = result,
                                        isValuesHidden = isValuesHidden,
                                        onToggleValuesHidden = { isValuesHidden = !isValuesHidden },
                                        onChartClick = onChartClick,
                                        history = dailyHistory
                                    )
                                }
                            }
                        }

                        item {
                            LazyRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp)
                                    .padding(top = 4.dp, bottom = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(AssetType.values()) { type ->
                                    val (icon, color) = when (type) {
                                        AssetType.STOCK -> Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF089981)
                                        AssetType.CRYPTO -> Icons.Default.Refresh to Color(0xFFF2A900)
                                        AssetType.METAL -> Icons.Default.Star to Color(0xFFD4AF37)
                                        AssetType.FUND -> Icons.AutoMirrored.Filled.ShowChart to Color(0xFF9C27B0)
                                        AssetType.EUROBOND -> Icons.Default.Lock to Color(0xFF3F51B5)
                                        AssetType.CASH -> Icons.Default.AccountBalance to Color(0xFF4CAF50)
                                        AssetType.REAL_ESTATE -> Icons.Default.Home to Color(0xFFFF5722)
                                        AssetType.VEHICLE -> Icons.Default.DirectionsCar to Color(0xFF607D8B)
                                        AssetType.BES -> Icons.Default.Info to Color(0xFF00BCD4)
                                        AssetType.DEBT -> Icons.Default.Warning to Color(0xFFF23645)
                                    }

                                    Card(
                                        onClick = {
                                            onAddAssetClick(type)
                                        },
                                        modifier = Modifier
                                            .width(76.dp)
                                            .height(88.dp),
                                        colors = CardDefaults.cardColors(
                                            containerColor = MaterialTheme.colorScheme.surface
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp,
                                            MaterialTheme.colorScheme.outline
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .clip(CircleShape)
                                                    .background(color.copy(alpha = 0.15f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (type == AssetType.STOCK) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_stock),
                                                        contentDescription = type.name,
                                                        tint = Color.Unspecified,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else if (type == AssetType.CRYPTO) {
                                                    Icon(
                                                        painter = painterResource(id = R.drawable.ic_crypto),
                                                        contentDescription = type.name,
                                                        tint = Color.Unspecified,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                } else {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = type.name,
                                                        tint = color,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.height(6.dp))
                                            Text(
                                                text = stringResource(id = type.getLocalizedNameRes()),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White,
                                                textAlign = TextAlign.Center,
                                                maxLines = 2,
                                                lineHeight = 11.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        item {
                            var showSortMenu by remember { mutableStateOf(false) }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(id = R.string.my_portfolio).uppercase(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextGraySecondary,
                                    letterSpacing = 1.2.sp
                                )
                                
                                Box {
                                    Row(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { showSortMenu = true }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Sort,
                                            contentDescription = stringResource(id = R.string.content_desc_sort),
                                            tint = TextGraySecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = when (sortOption) {
                                                SortOption.VALUE_DESC -> stringResource(id = R.string.sort_label_value_desc)
                                                SortOption.VALUE_ASC -> stringResource(id = R.string.sort_label_value_asc)
                                                SortOption.ALPHABETICAL -> stringResource(id = R.string.sort_label_alphabetical)
                                                SortOption.MANUAL -> stringResource(id = R.string.sort_label_manual)
                                            },
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextGraySecondary
                                        )
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = TextGraySecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    
                                    DropdownMenu(
                                        expanded = showSortMenu,
                                        onDismissRequest = { showSortMenu = false },
                                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_desc_value_desc), color = Color.White, fontSize = 13.sp) },
                                            onClick = {
                                                sortOption = SortOption.VALUE_DESC
                                                showSortMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_desc_value_asc), color = Color.White, fontSize = 13.sp) },
                                            onClick = {
                                                sortOption = SortOption.VALUE_ASC
                                                showSortMenu = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text(stringResource(id = R.string.sort_desc_alphabetical), color = Color.White, fontSize = 13.sp) },
                                            onClick = {
                                                sortOption = SortOption.ALPHABETICAL
                                                showSortMenu = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                            ) {
                                Column(modifier = Modifier.fillMaxWidth()) {
                                    sortedGroupedAssets.forEachIndexed { groupIndex, (type, assetsInGroup) ->
                                        val isExpanded = expandedCategories.contains(type)
                                        
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .animateContentSize()
                                        ) {
                                            CategoryHeader(
                                                type = type,
                                                assets = assetsInGroup,
                                                isExpanded = isExpanded,
                                                onToggle = {
                                                    expandedCategories = if (isExpanded) {
                                                        expandedCategories - type
                                                    } else {
                                                        expandedCategories + type
                                                    }
                                                },
                                                usdRate = usdRate,
                                                eurRate = eurRate,
                                                totalPortfolioAssetsTry = state.netWorthResult?.totalAssetsTry ?: 0.0,
                                                isValuesHidden = isValuesHidden
                                            )
                                            
                                            AnimatedVisibility(
                                                visible = isExpanded,
                                                enter = expandVertically() + fadeIn(),
                                                exit = shrinkVertically() + fadeOut()
                                            ) {
                                                Column(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .background(AccordionExpandedBg)
                                                ) {
                                                    assetsInGroup.forEachIndexed { index, calculatedAsset ->
                                                        AssetItemRow(
                                                            calculatedAsset = calculatedAsset,
                                                            onClick = { onAssetClick(calculatedAsset.asset) },
                                                            isValuesHidden = isValuesHidden
                                                        )
                                                        if (index < assetsInGroup.lastIndex) {
                                                            HorizontalDivider(
                                                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                                thickness = 0.5.dp,
                                                                modifier = Modifier.padding(horizontal = 16.dp)
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (groupIndex < sortedGroupedAssets.lastIndex) {
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                thickness = 0.5.dp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            
            if (state.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = Color.Transparent
                )
            }


        }
    }
}

@Composable
fun EmptyState(
    onAddAssetClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                contentDescription = null,
                tint = TextGraySecondary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(id = R.string.empty_state_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Text(
            text = stringResource(id = R.string.empty_state_desc),
            fontSize = 14.sp,
            color = TextGraySecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Button(
            onClick = onAddAssetClick,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = stringResource(id = R.string.btn_add_first_asset),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = stringResource(id = R.string.error_state_title),
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = message,
            fontSize = 14.sp,
            color = TextGraySecondary,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(text = stringResource(id = R.string.btn_retry), color = Color.White)
        }
    }
}
