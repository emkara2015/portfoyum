package com.antigravity.networthtracker.presentation.summary

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.data.local.entity.DailyNetWorthEntity
import com.antigravity.networthtracker.presentation.dashboard.DashboardViewModel
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import com.antigravity.networthtracker.presentation.theme.TradingViewGreen
import com.antigravity.networthtracker.presentation.theme.TradingViewRed
import com.antigravity.networthtracker.presentation.components.formatCurrency
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetWorthSummaryScreen(
    viewModel: DashboardViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val history by viewModel.dailyNetWorthHistory.collectAsState(initial = emptyList())
    var selectedCurrency by remember { mutableStateOf("TRY") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.title_net_worth_summary),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(id = R.string.desc_back),
                            tint = Color.White
                        )
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
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(id = R.string.error_not_enough_data),
                        color = TextGraySecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                        fontSize = 14.sp
                    )
                }
            } else {
                val values = history.map {
                    if (selectedCurrency == "TRY") it.netWorthTry else it.netWorthUsd
                }

                val startingValue = values.firstOrNull() ?: 0.0
                val currentValue = values.lastOrNull() ?: 0.0
                val totalChange = currentValue - startingValue
                val changePercentage = if (startingValue != 0.0) (totalChange / startingValue) * 100.0 else 0.0
                
                val profitColor = if (totalChange >= 0) TradingViewGreen else TradingViewRed
                val profitSign = if (totalChange >= 0) "+" else ""

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Currency Selector Tab
                    TabRow(
                        selectedTabIndex = if (selectedCurrency == "TRY") 0 else 1,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        indicator = { tabPositions ->
                            if (tabPositions.isNotEmpty()) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[if (selectedCurrency == "TRY") 0 else 1]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .padding(vertical = 12.dp)
                    ) {
                        Tab(
                            selected = selectedCurrency == "TRY",
                            onClick = { selectedCurrency = "TRY" },
                            text = { Text("TL", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        )
                        Tab(
                            selected = selectedCurrency == "USD",
                            onClick = { selectedCurrency = "USD" },
                            text = { Text("USD", fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Stats Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = R.string.label_current_net_worth),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGraySecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatCurrency(currentValue, selectedCurrency),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = String.format(
                                        Locale.US,
                                        "%s%s (%s%.2f%%)",
                                        profitSign,
                                        formatCurrency(totalChange, selectedCurrency),
                                        profitSign,
                                        changePercentage
                                    ),
                                    color = profitColor,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.label_initial_net_worth), fontSize = 11.sp, color = TextGraySecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatCurrency(startingValue, selectedCurrency),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .height(30.dp)
                                        .width(1.dp)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                                    Text(text = stringResource(id = R.string.label_start_date), fontSize = 11.sp, color = TextGraySecondary)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = formatDate(history.firstOrNull()?.date ?: ""),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Line Chart Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.label_net_worth_change),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextGraySecondary,
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            if (values.size < 2) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.error_need_more_days),
                                        color = TextGraySecondary,
                                        fontSize = 12.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            } else {
                                NetWorthLineChart(
                                    values = values,
                                    modifier = Modifier.fillMaxSize()
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
fun NetWorthLineChart(
    values: List<Double>,
    modifier: Modifier = Modifier
) {
    val maxVal = remember(values) { (values.maxOrNull() ?: 1.0) * 1.05 }
    val minVal = remember(values) { (values.minOrNull() ?: 0.0) * 0.95 }
    val valueRange = maxVal - minVal

    val primaryColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val paddingLeft = 60.dp.toPx()
        val paddingRight = 10.dp.toPx()
        val paddingTop = 10.dp.toPx()
        val paddingBottom = 30.dp.toPx()

        val chartWidth = width - paddingLeft - paddingRight
        val chartHeight = height - paddingTop - paddingBottom

        // Draw grid lines and labels
        val gridCount = 4
        for (i in 0..gridCount) {
            val ratio = i.toFloat() / gridCount
            val y = paddingTop + chartHeight * (1 - ratio)
            
            // Draw horizontal grid line
            drawLine(
                color = gridColor,
                start = Offset(paddingLeft, y),
                end = Offset(width - paddingRight, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Plot points
        val points = mutableListOf<Offset>()
        for (index in values.indices) {
            val xRatio = if (values.size > 1) index.toFloat() / (values.size - 1) else 0f
            val yRatio = if (valueRange > 0) ((values[index] - minVal) / valueRange).toFloat() else 0.5f

            val x = paddingLeft + chartWidth * xRatio
            val y = paddingTop + chartHeight * (1 - yRatio)
            points.add(Offset(x, y))
        }

        // Draw Area Gradient (under the line)
        if (points.isNotEmpty()) {
            val fillPath = Path().apply {
                moveTo(points.first().x, paddingTop + chartHeight)
                for (offset in points) {
                    lineTo(offset.x, offset.y)
                }
                lineTo(points.last().x, paddingTop + chartHeight)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.3f),
                        Color.Transparent
                    ),
                    startY = paddingTop,
                    endY = paddingTop + chartHeight
                )
            )

            // Draw Line path
            val linePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    lineTo(points[i].x, points[i].y)
                }
            }

            drawPath(
                path = linePath,
                color = primaryColor,
                style = Stroke(width = 2.dp.toPx())
            )

            // Draw latest point dot
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = points.last()
            )
            drawCircle(
                color = Color.White,
                radius = 2.5.dp.toPx(),
                center = points.last()
            )
        }
    }
}

private fun formatDate(dateStr: String): String {
    if (dateStr.isBlank()) return ""
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd MMM yyyy", Locale.forLanguageTag("tr-TR"))
        val date = parser.parse(dateStr)
        if (date != null) formatter.format(date) else dateStr
    } catch (e: Exception) {
        dateStr
    }
}
