package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BakeryDining
import androidx.compose.material.icons.filled.BreakfastDining
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.LocalBar
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.RiceBowl
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun FoodItemIcon(
    imageUrl: String,
    category: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (imageUrl.startsWith("http://") || imageUrl.startsWith("https://") || imageUrl.startsWith("content://")) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
        )
    } else {
        val (icon, bg) = resolveFoodIconAndColor(imageUrl, category)
        Box(
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(12.dp))
                .background(bg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(size * 0.55f)
            )
        }
    }
}

@Composable
fun VegNonVegBadge(
    isVeg: Boolean,
    modifier: Modifier = Modifier
) {
    val borderColor = if (isVeg) Color(0xFF16A34A) else Color(0xFFDC2626)
    val dotColor = if (isVeg) Color(0xFF16A34A) else Color(0xFFDC2626)

    Box(
        modifier = modifier
            .size(16.dp)
            .border(1.5.dp, borderColor, RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(dotColor, CircleShape)
        )
    }
}

private fun resolveFoodIconAndColor(imageUrl: String, category: String): Pair<ImageVector, Color> {
    val tag = imageUrl.lowercase()
    val cat = category.lowercase()

    return when {
        tag.contains("pizza") || cat.contains("pizza") ->
            Pair(Icons.Default.LocalPizza, Color(0xFFEA580C))

        tag.contains("burger") || cat.contains("fast food") ->
            Pair(Icons.Default.Fastfood, Color(0xFFD97706))

        tag.contains("rice") || tag.contains("biryani") || cat.contains("rice") || cat.contains("biryani") ->
            Pair(Icons.Default.RiceBowl, Color(0xFFCA8A04))

        tag.contains("noodles") || tag.contains("pasta") ->
            Pair(Icons.Default.RamenDining, Color(0xFF0284C7))

        tag.contains("coffee") || cat.contains("coffee") ->
            Pair(Icons.Default.Coffee, Color(0xFF78350F))

        tag.contains("drink") || tag.contains("soda") || cat.contains("beverage") ->
            Pair(Icons.Default.LocalBar, Color(0xFF0D9488))

        tag.contains("dessert") || cat.contains("dessert") ->
            Pair(Icons.Default.Icecream, Color(0xFFDB2777))

        tag.contains("bread") || cat.contains("bread") ->
            Pair(Icons.Default.BakeryDining, Color(0xFF9A3412))

        tag.contains("soup") ->
            Pair(Icons.Default.SoupKitchen, Color(0xFFE11D48))

        tag.contains("starter") || cat.contains("starter") ->
            Pair(Icons.Default.BreakfastDining, Color(0xFFF97316))

        else ->
            Pair(Icons.Default.DinnerDining, Color(0xFFC2410C))
    }
}
