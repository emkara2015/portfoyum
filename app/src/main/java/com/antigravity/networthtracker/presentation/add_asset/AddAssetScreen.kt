package com.antigravity.networthtracker.presentation.add_asset

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import com.antigravity.networthtracker.domain.model.MetalCategory
import com.antigravity.networthtracker.domain.model.GoldInputMode
import com.antigravity.networthtracker.domain.model.GoldPieceType
import com.antigravity.networthtracker.domain.model.GoldKarat
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.antigravity.networthtracker.R
import com.antigravity.networthtracker.domain.model.AssetType
import com.antigravity.networthtracker.presentation.components.AssetTypeIcon
import com.antigravity.networthtracker.presentation.components.getLocalizedNameRes
import com.antigravity.networthtracker.presentation.theme.TextGraySecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAssetScreen(
    viewModel: AddAssetViewModel,
    onBackClick: () -> Unit,
    onSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.isSuccess) {
        if (state.isSuccess) {
            onSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (state.selectedType == null) stringResource(id = R.string.title_select_asset_type) else stringResource(id = R.string.title_add_asset),
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (state.selectedType == null) {
                                onBackClick()
                            } else {
                                viewModel.clearAssetType()
                            }
                        }
                    ) {
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
            if (state.selectedType == null) {
                AssetTypeSelectionGrid(
                    onTypeSelect = { viewModel.selectAssetType(it) }
                )
            } else {
                DynamicAssetForm(
                    state = state,
                    viewModel = viewModel
                )
            }
        }
    }
}

@Composable
fun AssetTypeSelectionGrid(
    onTypeSelect: (AssetType) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = AssetType.values()

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(types) { type ->
            AssetTypeCard(
                type = type,
                onClick = { onTypeSelect(type) }
            )
        }
    }
}

@Composable
fun AssetTypeCard(
    type: AssetType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val title = stringResource(id = type.getLocalizedNameRes())
    val description = stringResource(
        id = when (type) {
            AssetType.STOCK -> R.string.desc_cat_stock
            AssetType.CRYPTO -> R.string.desc_cat_crypto
            AssetType.METAL -> R.string.desc_cat_metal
            AssetType.FUND -> R.string.desc_cat_fund
            AssetType.EUROBOND -> R.string.desc_cat_eurobond
            AssetType.CASH -> R.string.desc_cat_cash
            AssetType.REAL_ESTATE -> R.string.desc_cat_real_estate
            AssetType.VEHICLE -> R.string.desc_cat_vehicle
            AssetType.BES -> R.string.desc_cat_bes
            AssetType.DEBT -> R.string.desc_cat_debt
        }
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clickable { onClick() }
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AssetTypeIcon(type = type)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextGraySecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DynamicAssetForm(
    state: AddAssetState,
    viewModel: AddAssetViewModel,
    modifier: Modifier = Modifier
) {
    val type = state.selectedType ?: return
    val isAutoUpdate = type == AssetType.STOCK || type == AssetType.CRYPTO || (type == AssetType.FUND && state.fundCategory == FundCategory.TEFAS)
    val isDebt = type == AssetType.DEBT
    val shouldShowFormDetails = !isAutoUpdate || state.isSymbolSelected

    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = state.purchaseDate)

    val formattedDate = remember(state.purchaseDate) {
        java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(java.util.Date(state.purchaseDate))
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            viewModel.onDateChange(it)
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

    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(id = R.string.category_label, stringResource(id = type.getLocalizedNameRes())),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 1.sp
        )
        
        Spacer(modifier = Modifier.height(16.dp))

        if (type == AssetType.FUND) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(8.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val isTefas = state.fundCategory == FundCategory.TEFAS
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (isTefas) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { viewModel.onFundCategoryChange(FundCategory.TEFAS) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "TEFAŞ Fonları",
                        fontWeight = if (isTefas) FontWeight.Bold else FontWeight.Normal,
                        color = if (isTefas) Color.Black else Color.White,
                        fontSize = 14.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(if (!isTefas) MaterialTheme.colorScheme.primary else Color.Transparent, RoundedCornerShape(6.dp))
                        .clickable { viewModel.onFundCategoryChange(FundCategory.OTHER) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Diğer",
                        fontWeight = if (!isTefas) FontWeight.Bold else FontWeight.Normal,
                        color = if (!isTefas) Color.Black else Color.White,
                        fontSize = 14.sp
                    )
                }
            }
        }

        if (!isAutoUpdate && type != AssetType.METAL) {
            // Custom Name Input (Only for static/liability assets excluding Precious Metals)
            OutlinedTextField(
                value = state.name,
                onValueChange = { viewModel.onNameChange(it) },
                label = { Text(stringResource(id = R.string.field_asset_name)) },
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isAutoUpdate) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                OutlinedTextField(
                    value = state.symbol,
                    onValueChange = { viewModel.onSymbolChange(it) },
                    label = { Text(stringResource(id = R.string.field_symbol_code)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    singleLine = true
                )

                if (state.searchSuggestions.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp)
                            .height(200.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                    ) {
                        LazyColumn(
                            contentPadding = PaddingValues(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(state.searchSuggestions) { suggestion ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.selectSuggestion(suggestion) }
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = suggestion.symbol,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                        Text(
                                            text = suggestion.name,
                                            color = TextGraySecondary,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                    }
                                    suggestion.exchange?.let { exch ->
                                        Text(
                                            text = exch,
                                            color = MaterialTheme.colorScheme.primary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier
                                                .background(
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (shouldShowFormDetails) {
            if (state.selectedType == AssetType.METAL) {
                PreciousMetalFormSection(state = state, viewModel = viewModel)
            } else if (isAutoUpdate) {
                // Optional Note Input Field for Funds & Auto-update Assets (Revealed after symbol is selected)
                OutlinedTextField(
                    value = state.note,
                    onValueChange = { viewModel.onNoteChange(it) },
                    label = { Text(stringResource(id = R.string.field_note_label)) },
                    placeholder = { Text(stringResource(id = R.string.field_note_placeholder), color = TextGraySecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(id = R.string.field_note_helper),
                    fontSize = 11.sp,
                    color = TextGraySecondary,
                    modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
                )

                // Quantity Input
                OutlinedTextField(
                    value = state.quantity,
                    onValueChange = { viewModel.onQuantityChange(it) },
                    label = { Text(stringResource(id = R.string.field_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Purchase Price Input
                OutlinedTextField(
                    value = state.price,
                    onValueChange = { viewModel.onPriceChange(it) },
                    label = { Text(stringResource(id = R.string.field_unit_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation()
                )
            } else {
                // Static Asset: Value / Total Amount Input
                OutlinedTextField(
                    value = state.price,
                    onValueChange = { viewModel.onPriceChange(it) },
                    label = { Text(stringResource(id = if (isDebt) R.string.field_debt_amount else R.string.field_value_balance)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = customTextFieldColors(),
                    singleLine = true,
                    visualTransformation = ThousandsSeparatorTransformation()
                )
            }

            val isMetalFormComplete = remember(state.selectedType, state.metalCategory, state.goldInputMode, state.selectedGoldPieceType, state.selectedGoldKarat) {
                if (state.selectedType != AssetType.METAL) true
                else when (state.metalCategory) {
                    MetalCategory.GOLD -> {
                        when (state.goldInputMode) {
                            GoldInputMode.PIECE -> state.selectedGoldPieceType != null
                            GoldInputMode.GRAM -> state.selectedGoldKarat != null
                            null -> false
                        }
                    }
                    null -> false
                    else -> true
                }
            }

            if (isMetalFormComplete) {
                Spacer(modifier = Modifier.height(16.dp))

                // Alış Tarihi (Purchase Date) Field
                Box(
                    modifier = Modifier.fillMaxWidth()
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

                Spacer(modifier = Modifier.height(20.dp))

                // Currency Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                Text(
                    text = stringResource(id = R.string.label_currency),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
                
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                ) {
                    listOf("TRY", "USD", "EUR").forEach { curr ->
                        val isSelected = state.currency == curr
                        Box(
                            modifier = Modifier
                                .clickable { viewModel.onCurrencyChange(curr) }
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = curr,
                                color = if (isSelected) Color.White else TextGraySecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val errorText = when {
                state.errorMessageResId != null -> stringResource(id = state.errorMessageResId)
                state.errorMessageText != null -> state.errorMessageText
                else -> null
            }
            if (errorText != null) {
                Text(
                    text = errorText,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Submit Button
            Button(
                onClick = { viewModel.saveAsset() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                enabled = !state.isLoading
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = stringResource(id = if (isDebt) R.string.btn_add_debt else R.string.btn_add_asset),
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.White
                    )
                }
            }
        }
    }
}
}

fun getCategoryLabel(type: AssetType): String {
    return when (type) {
        AssetType.STOCK -> "HİSSE SENEDİ (DİNAMİK)"
        AssetType.CRYPTO -> "KRİPTO PARA (DİNAMİK)"
        AssetType.METAL -> "KIYMETLİ MADEN (STATİK)"
        AssetType.FUND -> "YATIRIM FONU (STATİK)"
        AssetType.EUROBOND -> "EUROBOND (STATİK)"
        AssetType.CASH -> "NAKİT / BANKA (STATİK)"
        AssetType.REAL_ESTATE -> "GAYRİMENKUL (STATİK)"
        AssetType.VEHICLE -> "ARAÇ / TAŞIT (STATİK)"
        AssetType.BES -> "BES (STATİK)"
        AssetType.DEBT -> "BORÇ / KREDİ (STATİK)"
    }
}

@Composable
fun customTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = MaterialTheme.colorScheme.primary,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outline,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.primary,
    unfocusedLabelColor = TextGraySecondary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White
)

@Composable
fun borderStroke() = androidx.compose.foundation.BorderStroke(
    width = 1.dp,
    color = MaterialTheme.colorScheme.outline
)

class ThousandsSeparatorTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val decimalSeparatorIndex = originalText.indexOfAny(charArrayOf('.', ','))
        
        val integerPart: String
        val fractionPart: String
        
        if (decimalSeparatorIndex != -1) {
            integerPart = originalText.substring(0, decimalSeparatorIndex)
            val rawFraction = originalText.substring(decimalSeparatorIndex)
            fractionPart = "," + rawFraction.substring(1)
        } else {
            integerPart = originalText
            fractionPart = ""
        }

        val formattedIntegerBuilder = StringBuilder()
        val originalIntegerLength = integerPart.length
        val originalToTransformed = IntArray(originalText.length + 1)
        
        var dotCount = 0
        for (i in 0 until originalIntegerLength) {
            val char = integerPart[i]
            formattedIntegerBuilder.append(char)
            originalToTransformed[i] = i + dotCount
            
            val digitsRemaining = originalIntegerLength - 1 - i
            if (digitsRemaining > 0 && digitsRemaining % 3 == 0) {
                formattedIntegerBuilder.append('.')
                dotCount++
            }
        }
        originalToTransformed[originalIntegerLength] = originalIntegerLength + dotCount
        
        val transformedText = formattedIntegerBuilder.toString() + fractionPart
        
        for (i in (originalIntegerLength + 1)..originalText.length) {
            originalToTransformed[i] = originalToTransformed[originalIntegerLength] + (i - originalIntegerLength)
        }
        
        if (decimalSeparatorIndex != -1) {
            originalToTransformed[decimalSeparatorIndex] = originalToTransformed[originalIntegerLength]
            for (i in (decimalSeparatorIndex + 1)..originalText.length) {
                originalToTransformed[i] = originalToTransformed[decimalSeparatorIndex] + (i - decimalSeparatorIndex)
            }
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset < 0) return 0
                if (offset > originalText.length) return transformedText.length
                return originalToTransformed[offset]
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset < 0) return 0
                if (offset > transformedText.length) return originalText.length
                for (i in 0..originalText.length) {
                    if (originalToTransformed[i] >= offset) {
                        return i
                    }
                }
                return originalText.length
            }
        }

        return TransformedText(AnnotatedString(transformedText), offsetMapping)
    }
}

@Composable
fun PreciousMetalFormSection(
    state: AddAssetState,
    viewModel: AddAssetViewModel
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(id = R.string.metal_section_select_metal),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextGraySecondary,
            letterSpacing = 1.2.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // Metal Category Selector Row (Horizontally Scrollable)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            MetalCategory.entries.forEach { category ->
                val isSelected = category == state.metalCategory
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.selectMetalCategory(category) },
                    label = { Text(stringResource(id = category.nameRes), fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        labelColor = Color.White
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // STEP 2: Calculation Method (Revealed when Metal Category is selected)
        if (state.metalCategory == MetalCategory.GOLD) {
            Text(
                text = stringResource(id = R.string.metal_section_select_mode),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextGraySecondary,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                GoldInputMode.entries.forEach { mode ->
                    val isSelected = mode == state.goldInputMode
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { viewModel.selectGoldInputMode(mode) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = stringResource(id = mode.labelRes),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = stringResource(id = mode.subtitleRes),
                                fontSize = 10.sp,
                                color = TextGraySecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // STEP 3 & 4 for Gold: Subtype Selection & Data Inputs (Revealed when GoldInputMode is selected)
            if (state.goldInputMode == GoldInputMode.PIECE) {
                Text(
                    text = stringResource(id = R.string.metal_section_select_gold_type),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGraySecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Gold Piece Types (Horizontally Scrollable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    GoldPieceType.entries.forEach { pieceType ->
                        val isSelected = pieceType == state.selectedGoldPieceType
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGoldPieceType(pieceType) },
                            label = { Text(stringResource(id = pieceType.labelRes), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                if (state.selectedGoldPieceType != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.metalPieceCount,
                        onValueChange = { viewModel.onMetalPieceCountChange(it) },
                        label = { Text(stringResource(id = R.string.metal_field_count_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Purchase Price (Optional)
                    OutlinedTextField(
                        value = state.price,
                        onValueChange = { viewModel.onPriceChange(it) },
                        label = { Text(stringResource(id = R.string.metal_field_price_optional)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true,
                        visualTransformation = ThousandsSeparatorTransformation()
                    )
                }
            } else if (state.goldInputMode == GoldInputMode.GRAM) {
                Text(
                    text = stringResource(id = R.string.metal_section_select_gold_karat),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextGraySecondary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Gold Karat Options (Horizontally Scrollable)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                ) {
                    GoldKarat.entries.forEach { karat ->
                        val isSelected = karat == state.selectedGoldKarat
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectGoldKarat(karat) },
                            label = { Text(stringResource(id = karat.labelRes), fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White,
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                labelColor = Color.White
                            )
                        )
                    }
                }

                if (state.selectedGoldKarat != null) {
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = state.metalGramWeight,
                        onValueChange = { viewModel.onMetalGramWeightChange(it) },
                        label = { Text(stringResource(id = R.string.metal_field_gram_label)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Purchase Price (Optional)
                    OutlinedTextField(
                        value = state.price,
                        onValueChange = { viewModel.onPriceChange(it) },
                        label = { Text(stringResource(id = R.string.metal_field_price_optional)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = customTextFieldColors(),
                        singleLine = true,
                        visualTransformation = ThousandsSeparatorTransformation()
                    )
                }
            }
        } else if (state.metalCategory != null) {
            // Other Metals (Silver, Platinum, Palladium, Copper): Revealed when metal category is selected
            val catName = stringResource(id = state.metalCategory.nameRes)
            OutlinedTextField(
                value = state.metalGramWeight,
                onValueChange = { viewModel.onMetalGramWeightChange(it) },
                label = { Text(stringResource(id = R.string.metal_field_gram_spec_label, catName)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Purchase Price (Optional)
            OutlinedTextField(
                value = state.price,
                onValueChange = { viewModel.onPriceChange(it) },
                label = { Text(stringResource(id = R.string.metal_field_price_optional)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = customTextFieldColors(),
                singleLine = true,
                visualTransformation = ThousandsSeparatorTransformation()
            )
        }
    }
}
