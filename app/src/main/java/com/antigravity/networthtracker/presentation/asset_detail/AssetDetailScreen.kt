package com.antigravity.networthtracker.presentation.asset_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.res.stringResource
import com.antigravity.networthtracker.R
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.ui.platform.LocalContext
import com.antigravity.networthtracker.domain.model.getLocalizedDisplayName
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.networthtracker.domain.model.Asset
import com.antigravity.networthtracker.domain.model.CalculatedAsset
import com.antigravity.networthtracker.domain.model.Transaction
import com.antigravity.networthtracker.presentation.add_asset.customTextFieldColors
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary
import com.antigravity.networthtracker.presentation.theme.TradingViewGreen
import com.antigravity.networthtracker.presentation.theme.TradingViewRed
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailScreen(
    assetId: Long,
    viewModel: AssetDetailViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    var showDeleteAssetDialog by remember { mutableStateOf(false) }
    var transactionToEdit by remember { mutableStateOf<Transaction?>(null) }

    LaunchedEffect(assetId) {
        viewModel.loadAssetDetails(assetId)
    }

    LaunchedEffect(state.isDeleted) {
        if (state.isDeleted) {
            onBackClick()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val asset = state.asset
                    if (asset != null && asset.type == com.antigravity.networthtracker.domain.model.AssetType.FUND && !asset.symbol.isNullOrBlank()) {
                        Column {
                            Text(
                                text = asset.symbol,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = Color.White
                            )
                            if (asset.name.isNotBlank() && asset.name != asset.symbol) {
                                Text(
                                    text = asset.name,
                                    fontSize = 11.sp,
                                    color = TextGraySecondary,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    } else {
                        Text(
                            text = asset?.getLocalizedDisplayName(LocalContext.current) ?: stringResource(id = R.string.title_asset_detail),
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                    }
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
                actions = {
                    if (state.asset != null) {
                        IconButton(onClick = { showDeleteAssetDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.btn_delete_asset),
                                tint = MaterialTheme.colorScheme.error
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
                state.isLoading && state.asset == null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.errorMessage != null && state.asset == null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(text = state.errorMessage!!, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.loadAssetDetails(assetId) }) {
                            Text(stringResource(id = R.string.btn_retry))
                        }
                    }
                }
                state.asset != null -> {
                    AssetDetailContent(
                        asset = state.asset!!,
                        calculatedAsset = state.calculatedAsset,
                        transactions = state.transactions,
                        chartData = state.chartData,
                        selectedRange = state.selectedRange,
                        isChartLoading = state.isChartLoading,
                        tefasFundDetails = state.tefasFundDetails,
                        onRangeSelected = { viewModel.selectChartRange(it) },
                        onTransactionClick = { transactionToEdit = it },
                        onSwipeDelete = { viewModel.deleteTransaction(it) },
                        onSaveValue = { transaction, newValue ->
                            viewModel.updateTransaction(transaction, transaction.quantity, newValue, transaction.date)
                        }
                    )
                }
            }
        }
    }

    if (showDeleteAssetDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAssetDialog = false },
            containerColor = MaterialTheme.colorScheme.surface,
            title = { Text(stringResource(id = R.string.dialog_delete_title), color = Color.White, fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(id = R.string.dialog_delete_text), color = Color.White) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteAsset()
                        showDeleteAssetDialog = false
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_delete_confirm), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAssetDialog = false }) {
                    Text(stringResource(id = R.string.btn_cancel), color = Color.White)
                }
            }
        )
    }

    if (transactionToEdit != null) {
        EditTransactionDialog(
            transaction = transactionToEdit!!,
            isAutoUpdate = state.asset?.isAutoUpdate == true,
            onDismiss = { transactionToEdit = null },
            onSave = { qty, price, date ->
                viewModel.updateTransaction(transactionToEdit!!, qty, price, date)
                transactionToEdit = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetDetailContent(
    asset: Asset,
    calculatedAsset: CalculatedAsset?,
    transactions: List<Transaction>,
    chartData: List<Pair<Long, Double>>,
    selectedRange: ChartRange,
    isChartLoading: Boolean,
    tefasFundDetails: com.antigravity.networthtracker.domain.model.TefasFundDetails? = null,
    onRangeSelected: (ChartRange) -> Unit,
    onTransactionClick: (Transaction) -> Unit,
    onSwipeDelete: (Transaction) -> Unit,
    onSaveValue: (Transaction, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            calculatedAsset?.let { calc ->
                SummaryPanel(calc = calc)
            }
        }

        if (!asset.isAutoUpdate) {
            item {
                val firstTransaction = transactions.firstOrNull()
                if (firstTransaction != null && calculatedAsset != null) {
                    var isEditingValue by remember { mutableStateOf(false) }
                    var editedValueText by remember(calculatedAsset.currentValue) { 
                        mutableStateOf(java.math.BigDecimal(calculatedAsset.currentValue).stripTrailingZeros().toPlainString()) 
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (isEditingValue) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedTextField(
                                    value = editedValueText,
                                    onValueChange = { 
                                        editedValueText = it
                                    },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    visualTransformation = ThousandsSeparatorVisualTransformation(asset.currency),
                                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                                    colors = customTextFieldColors(),
                                    singleLine = true
                                )
                                
                                IconButton(
                                    onClick = {
                                        val newValue = editedValueText.replace(',', '.').toDoubleOrNull()
                                        if (newValue != null && newValue >= 0) {
                                            onSaveValue(firstTransaction, newValue)
                                            isEditingValue = false
                                        }
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Güncelle",
                                        tint = TradingViewGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(8.dp))
                                
                                IconButton(
                                    onClick = {
                                        isEditingValue = false
                                        editedValueText = java.math.BigDecimal(calculatedAsset.currentValue).stripTrailingZeros().toPlainString()
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "İptal",
                                        tint = TradingViewRed,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = formatAssetDetailCurrency(calculatedAsset.currentValue, asset.currency, isMetal = asset.type == com.antigravity.networthtracker.domain.model.AssetType.METAL),
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                                
                                IconButton(
                                    onClick = { 
                                        isEditingValue = true 
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Düzenle",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (asset.isAutoUpdate) {
            item {
                Text(
                    text = stringResource(id = R.string.header_tx_history),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGraySecondary,
                    modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                    letterSpacing = 1.2.sp
                )
            }

            items(transactions, key = { it.id }) { transaction ->
                val dismissState = rememberSwipeToDismissBoxState(
                    confirmValueChange = { dismissValue ->
                        if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                            onSwipeDelete(transaction)
                            true
                        } else {
                            false
                        }
                    }
                )

                SwipeToDismissBox(
                    state = dismissState,
                    backgroundContent = {
                        val color = MaterialTheme.colorScheme.error
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color)
                                .padding(horizontal = 24.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = stringResource(id = R.string.dialog_delete_confirm),
                                tint = Color.White
                            )
                        }
                    },
                    content = {
                        TransactionRow(
                            transaction = transaction,
                            currency = asset.currency,
                            assetType = asset.type,
                            onClick = { onTransactionClick(transaction) }
                        )
                    },
                    enableDismissFromStartToEnd = false
                )

                HorizontalDivider(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    thickness = 0.5.dp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        if (asset.isAutoUpdate && asset.type != com.antigravity.networthtracker.domain.model.AssetType.FUND && !asset.symbol.isNullOrBlank()) {
            item {
                val rangeChangePercent = remember(chartData, selectedRange, calculatedAsset?.dailyChangePercentage) {
                    if (selectedRange == ChartRange.ONE_DAY && calculatedAsset != null) {
                        calculatedAsset.dailyChangePercentage
                    } else if (chartData.size >= 2) {
                        val firstPrice = chartData.first().second
                        val lastPrice = chartData.last().second
                        if (firstPrice > 0) ((lastPrice - firstPrice) / firstPrice) * 100.0 else 0.0
                    } else {
                        null
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.header_price_chart),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGraySecondary,
                            letterSpacing = 1.2.sp
                        )
                        
                        if (rangeChangePercent != null) {
                            val isPositive = rangeChangePercent >= 0
                            val color = if (isPositive) TradingViewGreen else TradingViewRed
                            val sign = if (isPositive) "+" else ""
                            val formattedPercent = String.format(Locale.US, "%.2f", rangeChangePercent)
                            
                            Text(
                                text = "$sign$formattedPercent% • ${selectedRange.label}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = color,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    }
                    
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartRange.entries.forEach { range ->
                            val isSelected = range == selectedRange
                            Box(
                                modifier = Modifier
                                    .padding(start = 6.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        if (isSelected) MaterialTheme.colorScheme.primary
                                        else Color.Transparent
                                    )
                                    .clickable {
                                        onRangeSelected(range)
                                    }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = range.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else TextGraySecondary
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                TradingViewChart(
                    symbol = asset.symbol ?: "",
                    chartData = chartData,
                    isChartLoading = isChartLoading,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }

        if (asset.type == com.antigravity.networthtracker.domain.model.AssetType.FUND) {
            if (tefasFundDetails != null && tefasFundDetails.hasAnyData()) {
                item {
                    TefasFundDetailsSection(fundDetails = tefasFundDetails)
                }
            }
        }
    }
}

@Composable
fun SummaryPanel(
    calc: CalculatedAsset,
    modifier: Modifier = Modifier
) {
    val isFund = calc.asset.type == com.antigravity.networthtracker.domain.model.AssetType.FUND
    val displayValue = if (isFund) calc.netCurrentValue else calc.currentValue
    val displayProfitLoss = if (isFund) calc.netProfitLoss else calc.profitLoss
    val displayProfitPercentage = if (isFund) calc.netProfitLossPercentage else calc.profitLossPercentage

    val profitColor = if (displayProfitLoss >= 0) TradingViewGreen else TradingViewRed
    val profitSign = if (displayProfitLoss >= 0) "+" else ""

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val titleText = if (isFund && calc.stopajAmount > 0) stringResource(id = R.string.label_net_portfolio_value) else stringResource(id = R.string.label_current_value)
            Text(
                text = titleText,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextGraySecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = formatIntegerValue(displayValue, calc.asset.currency),
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val formattedProfitLoss = formatIntegerValue(displayProfitLoss, calc.asset.currency)
                val signPrefix = if (displayProfitLoss >= 0) "+" else ""
                val suffix = if (isFund && calc.stopajAmount > 0) " Net" else ""
                Text(
                    text = String.format(Locale.US, "%s%s (%s%.2f%%%s)", 
                        signPrefix, formattedProfitLoss, profitSign, displayProfitPercentage, suffix),
                    color = profitColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            if (isFund) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(12.dp))

                val taxVal = calc.taxPercent ?: 0.0
                val taxValStr = if (taxVal % 1.0 == 0.0) String.format(Locale.US, "%.0f", taxVal) else String.format(Locale.US, "%.1f", taxVal)

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.label_gross_value), fontSize = 12.sp, color = TextGraySecondary)
                        Text(formatIntegerValue(calc.currentValue, calc.asset.currency), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.label_gross_return), fontSize = 12.sp, color = TextGraySecondary)
                        val bSign = if (calc.profitLoss >= 0) "+" else "-"
                        val bColor = if (calc.profitLoss >= 0) TradingViewGreen else TradingViewRed
                        val formattedBProfit = formatIntegerValue(kotlin.math.abs(calc.profitLoss), calc.asset.currency)
                        Text("$bSign$formattedBProfit (${bSign}${String.format(Locale.US, "%.2f%%", calc.profitLossPercentage)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = bColor)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.label_withholding_tax_deduction, taxValStr), fontSize = 12.sp, color = TextGraySecondary)
                        val stopajText = if (calc.stopajAmount > 0) "-${formatIntegerValue(calc.stopajAmount, calc.asset.currency)}" else stringResource(id = R.string.label_tax_exempt, calc.asset.currency)
                        Text(stopajText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (calc.stopajAmount > 0) TradingViewRed else TextGraySecondary)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.label_net_value), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(formatIntegerValue(calc.netCurrentValue, calc.asset.currency), fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.label_net_return), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextGraySecondary)
                        val nSign = if (calc.netProfitLoss >= 0) "+" else "-"
                        val nColor = if (calc.netProfitLoss >= 0) TradingViewGreen else TradingViewRed
                        val formattedNProfit = formatIntegerValue(kotlin.math.abs(calc.netProfitLoss), calc.asset.currency)
                        Text("$nSign$formattedNProfit (${nSign}${String.format(Locale.US, "%.2f%%", calc.netProfitLossPercentage)})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = nColor)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, thickness = 0.5.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (calc.asset.isAutoUpdate) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                        Text(text = stringResource(id = R.string.box_total_quantity), fontSize = 11.sp, color = TextGraySecondary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = formatQuantity(calc.totalQuantity),
                            fontSize = 15.sp,
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
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (calc.asset.isAutoUpdate) stringResource(id = R.string.box_cost_unit) else stringResource(id = R.string.box_total_invested),
                        fontSize = 11.sp,
                        color = TextGraySecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val costValue = if (calc.asset.isAutoUpdate && calc.totalQuantity > 0) calc.totalCost / calc.totalQuantity else calc.totalCost
                    val isMetal = calc.asset.type == com.antigravity.networthtracker.domain.model.AssetType.METAL
                    Text(
                        text = formatAssetDetailCurrency(costValue, calc.asset.currency, isMetal = isMetal),
                        fontSize = 15.sp,
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
                    Text(
                        text = if (calc.asset.isAutoUpdate) stringResource(id = R.string.box_current_price) else stringResource(id = R.string.label_current_value),
                        fontSize = 11.sp,
                        color = TextGraySecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val isMetal = calc.asset.type == com.antigravity.networthtracker.domain.model.AssetType.METAL
                    Text(
                        text = formatAssetDetailCurrency(calc.currentPrice, calc.asset.currency, isMetal = isMetal),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
fun TransactionRow(
    transaction: Transaction,
    currency: String,
    assetType: com.antigravity.networthtracker.domain.model.AssetType? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formattedDate = remember(transaction.date) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(transaction.date))
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = stringResource(id = R.string.row_quantity, formatQuantity(transaction.quantity)),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(id = R.string.row_date, formattedDate),
                fontSize = 12.sp,
                color = TextGraySecondary
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            val isMetal = assetType == com.antigravity.networthtracker.domain.model.AssetType.METAL
            Text(
                text = formatAssetDetailCurrency(transaction.price, currency, isMetal = isMetal),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = stringResource(id = R.string.dialog_edit_tx_title),
                    tint = TextGraySecondary,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTransactionDialog(
    transaction: Transaction,
    isAutoUpdate: Boolean,
    onDismiss: () -> Unit,
    onSave: (quantity: Double, price: Double, date: Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var quantityText by remember { mutableStateOf(transaction.quantity.toString()) }
    var priceText by remember { mutableStateOf(transaction.price.toString()) }
    var dateTimestamp by remember { mutableStateOf(transaction.date) }
    
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = dateTimestamp)

    val formattedDate = remember(dateTimestamp) {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(dateTimestamp))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            dateTimestamp = it
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(stringResource(id = R.string.dialog_select), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(id = R.string.btn_cancel))
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = { Text(stringResource(id = R.string.dialog_edit_tx_title), color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isAutoUpdate) {
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it },
                        label = { Text(stringResource(id = R.string.field_quantity)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true
                    )
                    
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(stringResource(id = R.string.field_unit_price)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true
                    )
                } else {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it },
                        label = { Text(stringResource(id = R.string.field_total_value)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    OutlinedTextField(
                        value = formattedDate,
                        onValueChange = {},
                        label = { Text(stringResource(id = R.string.field_purchase_date)) },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors()
                    )
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable { showDatePicker = true }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val qty = quantityText.replace(',', '.').toDoubleOrNull() ?: 1.0
                    val prc = priceText.replace(',', '.').toDoubleOrNull() ?: 0.0
                    onSave(qty, prc, dateTimestamp)
                }
            ) {
                Text(stringResource(id = R.string.btn_save), color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = R.string.btn_cancel), color = Color.White)
            }
        }
    )
}

@Composable
fun TradingViewChart(
    symbol: String,
    chartData: List<Pair<Long, Double>>,
    isChartLoading: Boolean,
    modifier: Modifier = Modifier
) {
    if (chartData.isEmpty() || isChartLoading) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.dp
            )
        }
    } else {
        val chartDataJson = remember(chartData) {
            chartData.joinToString(prefix = "[", postfix = "]") { (timestamp, price) ->
                "[$timestamp, $price]"
            }
        }

        val htmlData = remember(chartDataJson) {
            """
            <!DOCTYPE html>
            <html>
            <head>
                <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
                <style>
                    html, body {
                        margin: 0;
                        padding: 0;
                        width: 100%;
                        height: 100%;
                        background-color: #121318;
                        overflow: hidden;
                    }
                    #chart {
                        width: 100%;
                        height: 100%;
                    }
                </style>
                <script src="https://cdn.jsdelivr.net/npm/apexcharts"></script>
            </head>
            <body>
                <div id="chart"></div>
                <script>
                    var options = {
                      series: [{
                        name: 'Fiyat',
                        data: $chartDataJson
                      }],
                      chart: {
                        type: 'area',
                        height: '100%',
                        width: '100%',
                        toolbar: {
                          show: false
                        },
                        zoom: {
                          enabled: false
                        },
                        animations: {
                          enabled: true,
                          easing: 'easeinout',
                          speed: 600
                        }
                      },
                      dataLabels: {
                        enabled: false
                      },
                      stroke: {
                        curve: 'smooth',
                        width: 2,
                        colors: ['#3b82f6']
                      },
                      fill: {
                        type: 'gradient',
                        gradient: {
                          shadeIntensity: 1,
                          opacityFrom: 0.35,
                          opacityTo: 0.02,
                          stops: [0, 100]
                        }
                      },
                      grid: {
                        borderColor: '#232429',
                        strokeDashArray: 4,
                        xaxis: {
                          lines: {
                            show: false
                          }
                        },
                        yaxis: {
                          lines: {
                            show: true
                          }
                        },
                        padding: {
                          top: 10,
                          right: 15,
                          bottom: 0,
                          left: 10
                        }
                      },
                      xaxis: {
                        type: 'datetime',
                        labels: {
                          style: {
                            colors: '#a1a1aa',
                            fontSize: '10px',
                            fontFamily: 'system-ui, -apple-system, sans-serif'
                          }
                        },
                        axisBorder: {
                          show: false
                        },
                        axisTicks: {
                          show: false
                        }
                      },
                      yaxis: {
                        labels: {
                          style: {
                            colors: '#a1a1aa',
                            fontSize: '10px',
                            fontFamily: 'system-ui, -apple-system, sans-serif'
                          },
                          formatter: function (val) {
                            return val.toFixed(2);
                          }
                        }
                      },
                      tooltip: {
                        theme: 'dark',
                        x: {
                          format: 'dd MMM yyyy'
                        },
                        y: {
                          formatter: function(val) {
                            return val.toFixed(2);
                          },
                          title: {
                            formatter: function() { return 'Fiyat:'; }
                          }
                        }
                      },
                      colors: ['#3b82f6']
                    };

                    var chart = new ApexCharts(document.querySelector("#chart"), options);
                    chart.render();
                </script>
            </body>
            </html>
            """.trimIndent()
        }

        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(280.dp)
                .padding(horizontal = 16.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
        ) {
            AndroidView(
                factory = { context ->
                    WebView(context).apply {
                        layoutParams = ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        webViewClient = WebViewClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://www.tradingview.com", htmlData, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

class ThousandsSeparatorVisualTransformation(val currency: String) : VisualTransformation {
    override fun filter(text: androidx.compose.ui.text.AnnotatedString): androidx.compose.ui.text.input.TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return androidx.compose.ui.text.input.TransformedText(text, androidx.compose.ui.text.input.OffsetMapping.Identity)
        }

        val isTry = currency.uppercase() in listOf("TRY", "TL")
        val thousandsSep = if (isTry) '.' else ','
        val decimalSep = if (isTry) ',' else '.'

        val normalizedText = originalText.replace(',', '.')
        val parts = normalizedText.split('.')
        val integerPart = parts[0]
        val decimalPart = if (parts.size > 1) parts[1] else null

        val formattedInteger = StringBuilder()
        val len = integerPart.length
        for (i in 0 until len) {
            formattedInteger.append(integerPart[i])
            if ((len - 1 - i) % 3 == 0 && i != len - 1) {
                formattedInteger.append(thousandsSep)
            }
        }

        val formattedText = if (decimalPart != null) {
            "$formattedInteger$decimalSep$decimalPart"
        } else if (originalText.endsWith('.') || originalText.endsWith(',')) {
            "$formattedInteger$decimalSep"
        } else {
            formattedInteger.toString()
        }

        val offsetMapping = object : androidx.compose.ui.text.input.OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                
                var separatorCount = 0
                val integerLength = integerPart.length
                val mappedOffset = if (offset <= integerLength) offset else integerLength
                
                for (i in 0 until mappedOffset) {
                    if ((integerLength - 1 - i) % 3 == 0 && i != integerLength - 1 && i < offset - 1) {
                        separatorCount++
                    }
                }
                
                return if (offset <= integerLength) {
                    offset + separatorCount
                } else {
                    integerLength + separatorCount + 1 + (offset - integerLength - 1)
                }
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                
                var originalOffset = 0
                var currentTransformed = 0
                val originalLen = originalText.length
                
                while (originalOffset < originalLen && currentTransformed < offset) {
                    val isSeparator = originalOffset < integerPart.length && 
                                      (integerPart.length - 1 - originalOffset) % 3 == 0 && 
                                      originalOffset != integerPart.length - 1
                    
                    if (isSeparator) {
                        currentTransformed++
                        if (currentTransformed >= offset) break
                    }
                    
                    originalOffset++
                    currentTransformed++
                }
                
                return originalOffset
            }
        }

        return androidx.compose.ui.text.input.TransformedText(
            androidx.compose.ui.text.AnnotatedString(formattedText),
            offsetMapping
        )
    }
}

fun formatQuantity(value: Double): String {
    if (value % 1.0 == 0.0) {
        return String.format(Locale.US, "%.0f", value)
    }
    return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString()
}

fun formatIntegerValue(amount: Double, currency: String): String {
    val isTry = currency.uppercase() in listOf("TRY", "TL")
    val locale = when {
        isTry -> java.util.Locale("tr", "TR")
        currency.uppercase() == "EUR" -> java.util.Locale.GERMANY
        else -> java.util.Locale.US
    }
    val nf = java.text.NumberFormat.getNumberInstance(locale) as java.text.DecimalFormat
    nf.applyPattern("#,##0")
    val formatted = nf.format(kotlin.math.round(amount))
    return "$formatted $currency"
}

fun formatAssetDetailCurrency(amount: Double, currency: String, isMetal: Boolean = false): String {
    val isTry = currency.uppercase() in listOf("TRY", "TL")
    val locale = when {
        isTry -> java.util.Locale("tr", "TR")
        currency.uppercase() == "EUR" -> java.util.Locale.GERMANY
        else -> java.util.Locale.US
    }
    val nf = java.text.NumberFormat.getNumberInstance(locale) as java.text.DecimalFormat
    if (isMetal || amount % 1.0 == 0.0) {
        nf.applyPattern("#,##0")
        val formatted = nf.format(kotlin.math.round(amount))
        return "$formatted $currency"
    } else {
        nf.applyPattern("#,##0.00###")
        val formatted = nf.format(amount)
        return "$formatted $currency"
    }
}

@Composable
fun TefasFundDetailsSection(
    fundDetails: com.antigravity.networthtracker.domain.model.TefasFundDetails,
    modifier: Modifier = Modifier
) {
    androidx.compose.material3.Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.label_fund_yields),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGraySecondary,
                    letterSpacing = 1.2.sp
                )

                if (fundDetails.taxPercent != null) {
                    val taxVal = fundDetails.taxPercent
                    val taxValStr = if (taxVal % 1.0 == 0.0) String.format(Locale.US, "%.0f", taxVal) else String.format(Locale.US, "%.1f", taxVal)
                    val taxText = stringResource(id = R.string.label_withholding_tax_badge, taxValStr)

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = taxText,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val yields = listOf(
                stringResource(id = R.string.yield_1m) to fundDetails.yield1m,
                stringResource(id = R.string.yield_3m) to fundDetails.yield3m,
                stringResource(id = R.string.yield_6m) to fundDetails.yield6m,
                stringResource(id = R.string.yield_1y) to fundDetails.yield1y,
                stringResource(id = R.string.yield_ytd) to fundDetails.yieldYtd
            ).filter { it.second != null }

            if (yields.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    yields.forEach { (label, value) ->
                        val valDouble = value ?: 0.0
                        val isPositive = valDouble >= 0
                        val color = if (isPositive) TradingViewGreen else TradingViewRed
                        val sign = if (isPositive) "+" else ""
                        val formattedVal = String.format(Locale.US, "%s%.2f%%", sign, valDouble)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                fontSize = 10.sp,
                                color = TextGraySecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedVal,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = color
                            )
                        }
                    }
                }
            }
        }
    }
}

