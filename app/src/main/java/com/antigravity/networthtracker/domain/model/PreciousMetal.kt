package com.antigravity.networthtracker.domain.model

import androidx.annotation.StringRes
import com.antigravity.networthtracker.R

enum class MetalCategory(@StringRes val nameRes: Int, val defaultSymbol: String) {
    GOLD(R.string.metal_cat_gold, "XAU-HAS"),
    SILVER(R.string.metal_cat_silver, "XAG-GRAM"),
    PLATINUM(R.string.metal_cat_platinum, "XPT-GRAM"),
    PALLADIUM(R.string.metal_cat_palladium, "XPD-GRAM"),
    COPPER(R.string.metal_cat_copper, "COPPER-KG")
}

enum class GoldInputMode(@StringRes val labelRes: Int, @StringRes val subtitleRes: Int) {
    PIECE(R.string.metal_mode_piece_title, R.string.metal_mode_piece_sub),
    GRAM(R.string.metal_mode_gram_title, R.string.metal_mode_gram_sub)
}

enum class GoldPieceType(
    @StringRes val labelRes: Int,
    val symbol: String,
    val gramWeight: Double,
    val karatFactor: Double,
    val premiumMultiplier: Double
) {
    CEYREK(R.string.gold_piece_ceyrek, "XAU-CEYREK", 1.75, 0.9166, 1.03),
    YARIM(R.string.gold_piece_yarim, "XAU-YARIM", 3.50, 0.9166, 1.03),
    TAM(R.string.gold_piece_tam, "XAU-TAM", 7.00, 0.9166, 1.03),
    ATA(R.string.gold_piece_ata, "XAU-ATA", 7.21, 0.9166, 1.04),
    RESAT(R.string.gold_piece_resat, "XAU-RESAT", 7.20, 0.9166, 1.04),
    GRAM_24K(R.string.gold_piece_gram, "XAU-HAS", 1.00, 1.0000, 1.00)
}

enum class GoldKarat(
    @StringRes val labelRes: Int,
    val symbol: String,
    val purityFactor: Double
) {
    K24(R.string.gold_karat_24, "XAU-HAS", 1.0000),
    K22(R.string.gold_karat_22, "XAU-22K", 0.9166),
    K18(R.string.gold_karat_18, "XAU-18K", 0.7500),
    K14(R.string.gold_karat_14, "XAU-14K", 0.5833)
}

fun Asset.getLocalizedDisplayName(context: android.content.Context, quantityOverride: Double? = null): String {
    if (type == AssetType.FUND && !symbol.isNullOrBlank()) {
        return symbol
    }
    if (type != AssetType.METAL) return name

    val sym = symbol ?: return name
    
    val qty = quantityOverride ?: run {
        val match = Regex("""\(([\d.,]+)\s*(?:gr|Adet|Pcs|g|Piezas)?\)""").find(name)
        match?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull()
    }

    val formattedQty = if (qty != null) {
        if (qty % 1.0 == 0.0) qty.toLong().toString() else String.format(java.util.Locale.US, "%.2f", qty)
    } else {
        ""
    }

    // 1. Piece Types (Quarter, Half, Full, Ata, Resat)
    if (sym == "XAU-CEYREK" || sym == "XAU-YARIM" || sym == "XAU-TAM" || sym == "XAU-ATA" || sym == "XAU-RESAT") {
        val pieceType = GoldPieceType.entries.find { it.symbol == sym }
        if (pieceType != null) {
            val label = context.getString(pieceType.labelRes)
            return if (formattedQty.isNotBlank()) {
                context.getString(R.string.metal_name_piece_format, label, formattedQty)
            } else {
                label
            }
        }
    }

    // 2. Karat Types (24K, 22K, 18K, 14K)
    val karat = GoldKarat.entries.find { it.symbol == sym }
    if (karat != null) {
        // Check if entered as piece 24K
        if (sym == "XAU-HAS" && (name.contains("Adet", ignoreCase = true) || name.contains("Pcs", ignoreCase = true) || name.contains("Piezas", ignoreCase = true))) {
            val label = context.getString(GoldPieceType.GRAM_24K.labelRes)
            return if (formattedQty.isNotBlank()) {
                context.getString(R.string.metal_name_piece_format, label, formattedQty)
            } else {
                label
            }
        }
        val label = context.getString(karat.labelRes)
        return if (formattedQty.isNotBlank()) {
            context.getString(R.string.metal_name_gram_format, label, formattedQty)
        } else {
            label
        }
    }

    // 3. Other Metal Categories (Silver, Platinum, Palladium, Copper)
    val category = MetalCategory.entries.find { it.defaultSymbol == sym }
    if (category != null) {
        val label = context.getString(category.nameRes)
        return if (formattedQty.isNotBlank()) {
            context.getString(R.string.metal_name_gram_format, label, formattedQty)
        } else {
            label
        }
    }

    return name
}

fun CalculatedAsset.getLocalizedDisplayName(context: android.content.Context): String {
    return asset.getLocalizedDisplayName(context, totalQuantity)
}
