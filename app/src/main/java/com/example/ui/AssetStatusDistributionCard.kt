package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DonutLarge
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.StackedBarChart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AssetEntity
import com.example.model.SopConstants
import com.example.ui.theme.BarlowDisplayFontFamily
import com.example.ui.theme.IbmPlexMonoFontFamily
import com.example.ui.theme.LocalAkahColors
import com.example.viewmodel.QuickStatFilter
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class StatusChartMode(val label: String) {
    DONUT_AND_BARS("Donut & Bar"),
    BAR_CHART("Bar Chart"),
    BY_SYSTEM("By System")
}

data class StatusDistributionBucket(
    val key: String,
    val displayLabel: String,
    val sopStatusValue: String,
    val quickFilter: QuickStatFilter,
    val count: Int,
    val percentage: Float
)

data class SystemStatusBreakdown(
    val system: String,
    val operationalCount: Int,
    val maintenanceRequiredCount: Int,
    val outOfServiceCount: Int
) {
    val total: Int
        get() = operationalCount + maintenanceRequiredCount + outOfServiceCount
}

data class AssetStatusDistributionSummary(
    val totalAssets: Int,
    val operationalCount: Int,
    val maintenanceRequiredCount: Int,
    val outOfServiceCount: Int,
    val operationalPct: Float,
    val maintenanceRequiredPct: Float,
    val outOfServicePct: Float,
    val buckets: List<StatusDistributionBucket>,
    val systemBreakdowns: List<SystemStatusBreakdown>
)

fun calculateAssetStatusDistribution(assets: List<AssetEntity>): AssetStatusDistributionSummary {
    val total = assets.size
    var operational = 0
    var maintenanceRequired = 0
    var outOfService = 0

    assets.forEach { asset ->
        when (asset.status.trim().lowercase(Locale.US)) {
            "in service", "operational" -> operational++
            "needs attention", "maintenance required" -> maintenanceRequired++
            "out of service" -> outOfService++
            else -> operational++
        }
    }

    val safeTotal = total.coerceAtLeast(1).toFloat()
    val opPct = if (total == 0) 0f else (operational * 100f) / safeTotal
    val maintPct = if (total == 0) 0f else (maintenanceRequired * 100f) / safeTotal
    val oosPct = if (total == 0) 0f else (outOfService * 100f) / safeTotal

    val buckets = listOf(
        StatusDistributionBucket(
            key = "operational",
            displayLabel = "Operational",
            sopStatusValue = "In service",
            quickFilter = QuickStatFilter.IN_SERVICE,
            count = operational,
            percentage = opPct
        ),
        StatusDistributionBucket(
            key = "maintenance_required",
            displayLabel = "Maintenance Required",
            sopStatusValue = "Needs attention",
            quickFilter = QuickStatFilter.NEEDS_ATTENTION,
            count = maintenanceRequired,
            percentage = maintPct
        ),
        StatusDistributionBucket(
            key = "out_of_service",
            displayLabel = "Out of Service",
            sopStatusValue = "Out of service",
            quickFilter = QuickStatFilter.OUT_OF_SERVICE,
            count = outOfService,
            percentage = oosPct
        )
    )

    val systemsOrder = SopConstants.SYSTEMS
    val groupedBySystem = assets.groupBy { it.system }
    val systemBreakdowns = systemsOrder.mapNotNull { sys ->
        val sysAssets = groupedBySystem[sys].orEmpty()
        if (sysAssets.isEmpty()) return@mapNotNull null
        var sysOp = 0
        var sysMaint = 0
        var sysOos = 0
        sysAssets.forEach { a ->
            when (a.status.trim().lowercase(Locale.US)) {
                "in service", "operational" -> sysOp++
                "needs attention", "maintenance required" -> sysMaint++
                "out of service" -> sysOos++
                else -> sysOp++
            }
        }
        SystemStatusBreakdown(
            system = sys,
            operationalCount = sysOp,
            maintenanceRequiredCount = sysMaint,
            outOfServiceCount = sysOos
        )
    }

    return AssetStatusDistributionSummary(
        totalAssets = total,
        operationalCount = operational,
        maintenanceRequiredCount = maintenanceRequired,
        outOfServiceCount = outOfService,
        operationalPct = opPct,
        maintenanceRequiredPct = maintPct,
        outOfServicePct = oosPct,
        buckets = buckets,
        systemBreakdowns = systemBreakdowns
    )
}

@Composable
fun AssetStatusDistributionCard(
    allAssets: List<AssetEntity>,
    filteredAssets: List<AssetEntity>,
    isFilteringActive: Boolean,
    activeQuickFilter: QuickStatFilter,
    onToggleQuickFilter: (QuickStatFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val akah = LocalAkahColors.current
    var chartMode by remember { mutableStateOf(StatusChartMode.DONUT_AND_BARS) }
    var useFilteredScope by remember(isFilteringActive) { mutableStateOf(false) }
    var inspectedBucketKey by remember { mutableStateOf<String?>(null) }

    val sourceAssets = if (useFilteredScope && isFilteringActive) filteredAssets else allAssets
    val distribution = remember(sourceAssets) {
        calculateAssetStatusDistribution(sourceAssets)
    }

    val animProgress by animateFloatAsState(
        targetValue = if (distribution.totalAssets > 0) 1f else 0f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "status_chart_progress"
    )

    fun colorForBucket(key: String): Color = when (key) {
        "operational" -> akah.ok
        "maintenance_required" -> akah.warn
        "out_of_service" -> akah.crit
        else -> akah.ok
    }

    fun softColorForBucket(key: String): Color = when (key) {
        "operational" -> akah.okSoft
        "maintenance_required" -> akah.warnSoft
        "out_of_service" -> akah.critSoft
        else -> akah.okSoft
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, akah.line),
        tonalElevation = 1.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("asset_status_distribution_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Card Header + Chart Mode Switcher
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DonutLarge,
                            contentDescription = "Asset Status Distribution Chart",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "ASSET STATUS DISTRIBUTION",
                            fontFamily = BarlowDisplayFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            letterSpacing = 0.7.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Interactive breakdown · Operational, Maintenance Required & Out of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.muted
                    )
                }

                // Chart Mode Selector Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    StatusChartMode.entries.forEach { mode ->
                        val isSelected = chartMode == mode
                        Surface(
                            onClick = { chartMode = mode },
                            shape = CircleShape,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.background,
                            border = BorderStroke(
                                1.dp,
                                if (isSelected) MaterialTheme.colorScheme.primary else akah.line
                            ),
                            modifier = Modifier.testTag("status_chart_mode_${mode.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = when (mode) {
                                        StatusChartMode.DONUT_AND_BARS -> Icons.Default.DonutLarge
                                        StatusChartMode.BAR_CHART -> Icons.Default.BarChart
                                        StatusChartMode.BY_SYSTEM -> Icons.Default.StackedBarChart
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else akah.muted
                                )
                                Text(
                                    text = mode.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Optional Scope Toggle when active search/location/system filter is applied
            if (isFilteringActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (useFilteredScope) {
                            "Showing filtered subset (${filteredAssets.size} of ${allAssets.size} assets)"
                        } else {
                            "Showing full campus register (${allAssets.size} assets)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = akah.muted
                    )
                    Surface(
                        onClick = { useFilteredScope = !useFilteredScope },
                        shape = CircleShape,
                        color = if (useFilteredScope) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.testTag("toggle_chart_scope_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterAlt,
                                contentDescription = "Toggle chart dataset scope",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = if (useFilteredScope) "Scope: Filtered (${filteredAssets.size})" else "Scope: All (${allAssets.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // Full-Width Stacked Proportional Distribution Bar
            StackedProportionalStatusBar(
                distribution = distribution,
                animProgress = animProgress,
                colorForBucket = ::colorForBucket,
                selectedBucketKey = inspectedBucketKey,
                onSelectBucket = { bucket ->
                    inspectedBucketKey = if (inspectedBucketKey == bucket.key) null else bucket.key
                    onToggleQuickFilter(bucket.quickFilter)
                }
            )

            // Main Visualization Area based on selected mode & screen width
            when (chartMode) {
                StatusChartMode.DONUT_AND_BARS -> {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isWide = maxWidth >= 520.dp
                        if (isWide) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.width(190.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    InteractiveStatusDonutChart(
                                        distribution = distribution,
                                        animProgress = animProgress,
                                        activeQuickFilter = activeQuickFilter,
                                        inspectedBucketKey = inspectedBucketKey,
                                        colorForBucket = ::colorForBucket,
                                        onSliceTap = { bucket ->
                                            inspectedBucketKey = bucket.key
                                            onToggleQuickFilter(bucket.quickFilter)
                                        }
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    distribution.buckets.forEach { bucket ->
                                        StatusLegendAndBarRow(
                                            bucket = bucket,
                                            totalAssets = distribution.totalAssets,
                                            color = colorForBucket(bucket.key),
                                            softColor = softColorForBucket(bucket.key),
                                            isFilterActive = activeQuickFilter == bucket.quickFilter,
                                            isInspected = inspectedBucketKey == bucket.key,
                                            animProgress = animProgress,
                                            onClick = {
                                                inspectedBucketKey = bucket.key
                                                onToggleQuickFilter(bucket.quickFilter)
                                            }
                                        )
                                    }
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                InteractiveStatusDonutChart(
                                    distribution = distribution,
                                    animProgress = animProgress,
                                    activeQuickFilter = activeQuickFilter,
                                    inspectedBucketKey = inspectedBucketKey,
                                    colorForBucket = ::colorForBucket,
                                    onSliceTap = { bucket ->
                                        inspectedBucketKey = bucket.key
                                        onToggleQuickFilter(bucket.quickFilter)
                                    }
                                )
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    distribution.buckets.forEach { bucket ->
                                        StatusLegendAndBarRow(
                                            bucket = bucket,
                                            totalAssets = distribution.totalAssets,
                                            color = colorForBucket(bucket.key),
                                            softColor = softColorForBucket(bucket.key),
                                            isFilterActive = activeQuickFilter == bucket.quickFilter,
                                            isInspected = inspectedBucketKey == bucket.key,
                                            animProgress = animProgress,
                                            onClick = {
                                                inspectedBucketKey = bucket.key
                                                onToggleQuickFilter(bucket.quickFilter)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                StatusChartMode.BAR_CHART -> {
                    RechartsStyleVerticalBarChart(
                        distribution = distribution,
                        animProgress = animProgress,
                        activeQuickFilter = activeQuickFilter,
                        colorForBucket = ::colorForBucket,
                        softColorForBucket = ::softColorForBucket,
                        onBarTap = { bucket ->
                            inspectedBucketKey = bucket.key
                            onToggleQuickFilter(bucket.quickFilter)
                        }
                    )
                }

                StatusChartMode.BY_SYSTEM -> {
                    SystemStackedDistributionChart(
                        systemBreakdowns = distribution.systemBreakdowns,
                        operationalColor = akah.ok,
                        maintenanceColor = akah.warn,
                        outOfServiceColor = akah.crit
                    )
                }
            }

            // Recharts-style Interactive Tooltip / Active Filter Callout Banner
            val activeBucket = distribution.buckets.find {
                it.quickFilter == activeQuickFilter || it.key == inspectedBucketKey
            }
            AnimatedVisibility(visible = activeBucket != null) {
                activeBucket?.let { bucket ->
                    val bucketColor = colorForBucket(bucket.key)
                    val bucketSoft = softColorForBucket(bucket.key)
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = bucketSoft,
                        border = BorderStroke(1.dp, bucketColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("status_chart_active_tooltip")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(bucketColor)
                                )
                                Column {
                                    Text(
                                        text = "${bucket.displayLabel} (${bucket.sopStatusValue}) · ${bucket.count} of ${distribution.totalAssets} assets (${String.format(Locale.US, "%.1f", bucket.percentage)}%)",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = if (activeQuickFilter == bucket.quickFilter) {
                                            "Asset register below is filtered to ${bucket.displayLabel} assets"
                                        } else {
                                            "Tap segment again to filter or clear selection"
                                        },
                                        style = MaterialTheme.typography.bodySmall,
                                        color = akah.muted
                                    )
                                }
                            }

                            if (activeQuickFilter == bucket.quickFilter) {
                                OutlinedButton(
                                    onClick = {
                                        inspectedBucketKey = null
                                        onToggleQuickFilter(bucket.quickFilter)
                                    },
                                    shape = CircleShape,
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                    modifier = Modifier.testTag("clear_chart_status_filter_button")
                                ) {
                                    Text("Show All", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StackedProportionalStatusBar(
    distribution: AssetStatusDistributionSummary,
    animProgress: Float,
    colorForBucket: (String) -> Color,
    selectedBucketKey: String?,
    onSelectBucket: (StatusDistributionBucket) -> Unit
) {
    val akah = LocalAkahColors.current
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .testTag("status_stacked_progress_bar"),
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (distribution.totalAssets == 0) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(akah.line)
                )
            } else {
                distribution.buckets.forEach { bucket ->
                    if (bucket.count > 0) {
                        val fraction = (bucket.count.toFloat() / distribution.totalAssets.toFloat()) * animProgress
                        Box(
                            modifier = Modifier
                                .weight(fraction.coerceAtLeast(0.02f))
                                .fillMaxHeight()
                                .background(
                                    color = if (selectedBucketKey == null || selectedBucketKey == bucket.key) {
                                        colorForBucket(bucket.key)
                                    } else {
                                        colorForBucket(bucket.key).copy(alpha = 0.45f)
                                    }
                                )
                                .clickable { onSelectBucket(bucket) }
                        )
                    }
                }
            }
        }

        // Compact inline summary row under the proportional bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            distribution.buckets.forEach { bucket ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onSelectBucket(bucket) }
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(colorForBucket(bucket.key))
                    )
                    Text(
                        text = "${bucket.displayLabel}: ${bucket.count} (${bucket.percentage.roundToInt()}%)",
                        fontSize = 11.sp,
                        fontFamily = IbmPlexMonoFontFamily,
                        color = akah.muted
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveStatusDonutChart(
    distribution: AssetStatusDistributionSummary,
    animProgress: Float,
    activeQuickFilter: QuickStatFilter,
    inspectedBucketKey: String?,
    colorForBucket: (String) -> Color,
    onSliceTap: (StatusDistributionBucket) -> Unit
) {
    val akah = LocalAkahColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(172.dp)
            .testTag("status_donut_chart")
            .semantics {
                contentDescription = "Asset Status Distribution Donut Chart: " +
                    "Operational ${distribution.operationalCount}, " +
                    "Maintenance Required ${distribution.maintenanceRequiredCount}, " +
                    "Out of Service ${distribution.outOfServiceCount}"
            }
            .pointerInput(distribution) {
                detectTapGestures { tapOffset ->
                    if (distribution.totalAssets <= 0) return@detectTapGestures
                    val centerX = size.width / 2f
                    val centerY = size.height / 2f
                    val dx = tapOffset.x - centerX
                    val dy = tapOffset.y - centerY
                    val distance = hypot(dx, dy)
                    val outerRadius = minOf(size.width, size.height) / 2f
                    val innerRadius = outerRadius * 0.52f
                    if (distance in innerRadius..outerRadius) {
                        // Normalize angle relative to -90deg (top of circle)
                        var angleDeg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        angleDeg = (angleDeg + 90f + 360f) % 360f
                        var runningAngle = 0f
                        for (bucket in distribution.buckets) {
                            val sweep = (bucket.count.toFloat() / distribution.totalAssets.toFloat()) * 360f
                            if (angleDeg >= runningAngle && angleDeg <= runningAngle + sweep) {
                                onSliceTap(bucket)
                                break
                            }
                            runningAngle += sweep
                        }
                    }
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 26.dp.toPx()
            val diameter = minOf(size.width, size.height) - strokeWidth
            val topLeft = Offset(
                x = (size.width - diameter) / 2f,
                y = (size.height - diameter) / 2f
            )
            val arcSize = Size(diameter, diameter)

            // Background track ring
            drawArc(
                color = akah.line.copy(alpha = 0.45f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth)
            )

            if (distribution.totalAssets > 0) {
                var currentStartAngle = -90f
                val nonZeroBuckets = distribution.buckets.filter { it.count > 0 }
                val gapDegrees = if (nonZeroBuckets.size > 1) 3f else 0f

                nonZeroBuckets.forEach { bucket ->
                    val rawSweep = (bucket.count.toFloat() / distribution.totalAssets.toFloat()) * 360f * animProgress
                    val sweep = (rawSweep - gapDegrees).coerceAtLeast(1.5f)
                    val isSelected = activeQuickFilter == bucket.quickFilter || inspectedBucketKey == bucket.key
                    val sliceStroke = if (isSelected) strokeWidth + 5.dp.toPx() else strokeWidth

                    drawArc(
                        color = colorForBucket(bucket.key),
                        startAngle = currentStartAngle + (gapDegrees / 2f),
                        sweepAngle = sweep,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = sliceStroke, cap = StrokeCap.Butt)
                    )

                    // Separator tick for clarity
                    if (nonZeroBuckets.size > 1) {
                        val angleRad = Math.toRadians(currentStartAngle.toDouble())
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val rInner = (diameter / 2f) - (strokeWidth / 2f)
                        val rOuter = (diameter / 2f) + (strokeWidth / 2f)
                        drawLine(
                            color = surfaceColor,
                            start = Offset(
                                x = center.x + (rInner * cos(angleRad)).toFloat(),
                                y = center.y + (rInner * sin(angleRad)).toFloat()
                            ),
                            end = Offset(
                                x = center.x + (rOuter * cos(angleRad)).toFloat(),
                                y = center.y + (rOuter * sin(angleRad)).toFloat()
                            ),
                            strokeWidth = 2.dp.toPx()
                        )
                    }

                    currentStartAngle += rawSweep
                }
            }
        }

        // Center KPI Metric Inside Donut
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${distribution.operationalPct.roundToInt()}%",
                fontFamily = BarlowDisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp,
                lineHeight = 26.sp,
                color = akah.ok,
                modifier = Modifier.testTag("donut_operational_pct_text")
            )
            Text(
                text = "OPERATIONAL",
                fontFamily = IbmPlexMonoFontFamily,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp,
                color = akah.muted
            )
            Text(
                text = "${distribution.operationalCount}/${distribution.totalAssets} assets",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatusLegendAndBarRow(
    bucket: StatusDistributionBucket,
    totalAssets: Int,
    color: Color,
    softColor: Color,
    isFilterActive: Boolean,
    isInspected: Boolean,
    animProgress: Float,
    onClick: () -> Unit
) {
    val akah = LocalAkahColors.current
    val highlighted = isFilterActive || isInspected

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (highlighted) softColor else MaterialTheme.colorScheme.background,
        border = BorderStroke(
            width = if (highlighted) 1.5.dp else 1.dp,
            color = if (highlighted) color else akah.line
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_distribution_row_${bucket.key}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = when (bucket.key) {
                            "operational" -> Icons.Default.CheckCircle
                            "maintenance_required" -> Icons.Default.Warning
                            else -> Icons.Default.Error
                        },
                        contentDescription = bucket.displayLabel,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Column {
                        Text(
                            text = bucket.displayLabel,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "SOP Status: ${bucket.sopStatusValue}",
                            fontSize = 11.sp,
                            color = akah.muted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${bucket.count}",
                        fontFamily = BarlowDisplayFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = color,
                        modifier = Modifier.testTag("status_count_${bucket.key}")
                    )
                    Surface(
                        shape = CircleShape,
                        color = softColor,
                        border = BorderStroke(1.dp, color.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f%%", bucket.percentage),
                            fontFamily = IbmPlexMonoFontFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = color,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Recharts-inspired horizontal bar with grid track
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(CircleShape)
                    .background(akah.line.copy(alpha = 0.45f))
            ) {
                val barFraction = if (totalAssets == 0) 0f else {
                    ((bucket.count.toFloat() / totalAssets.toFloat()) * animProgress).coerceIn(0f, 1f)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth(barFraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}

@Composable
private fun RechartsStyleVerticalBarChart(
    distribution: AssetStatusDistributionSummary,
    animProgress: Float,
    activeQuickFilter: QuickStatFilter,
    colorForBucket: (String) -> Color,
    softColorForBucket: (String) -> Color,
    onBarTap: (StatusDistributionBucket) -> Unit
) {
    val akah = LocalAkahColors.current
    val maxCount = remember(distribution) {
        distribution.buckets.maxOfOrNull { it.count }?.coerceAtLeast(1) ?: 1
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_vertical_bar_chart"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.background,
            border = BorderStroke(1.dp, akah.line),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Plot Area with Cartesian dashed gridlines + vertical bars
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(170.dp)
                ) {
                    // Horizontal Cartesian Grid Lines (100%, 75%, 50%, 25%, 0%)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val steps = 4
                        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        for (i in 0..steps) {
                            val y = (size.height / steps) * i
                            drawLine(
                                color = akah.line,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.dp.toPx(),
                                pathEffect = if (i == steps) null else dashEffect
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        distribution.buckets.forEach { bucket ->
                            val barColor = colorForBucket(bucket.key)
                            val isSelected = activeQuickFilter == bucket.quickFilter
                            val heightFraction = ((bucket.count.toFloat() / maxCount.toFloat()) * animProgress)
                                .coerceIn(0.04f, 1f)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clickable { onBarTap(bucket) }
                                    .padding(horizontal = 8.dp)
                                    .testTag("vertical_bar_${bucket.key}")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = softColorForBucket(bucket.key),
                                    border = BorderStroke(
                                        1.dp,
                                        if (isSelected) barColor else barColor.copy(alpha = 0.4f)
                                    )
                                ) {
                                    Text(
                                        text = "${bucket.count} (${bucket.percentage.roundToInt()}%)",
                                        fontFamily = IbmPlexMonoFontFamily,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = barColor,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Canvas(
                                    modifier = Modifier
                                        .width(52.dp)
                                        .fillMaxHeight(heightFraction * 0.78f)
                                ) {
                                    drawRoundRect(
                                        color = barColor,
                                        topLeft = Offset.Zero,
                                        size = size,
                                        cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = akah.line, thickness = 1.dp)

                // X-Axis Labels
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    distribution.buckets.forEach { bucket ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onBarTap(bucket) }
                        ) {
                            Text(
                                text = bucket.displayLabel,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = bucket.sopStatusValue,
                                fontSize = 10.sp,
                                color = akah.muted,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SystemStackedDistributionChart(
    systemBreakdowns: List<SystemStatusBreakdown>,
    operationalColor: Color,
    maintenanceColor: Color,
    outOfServiceColor: Color
) {
    val akah = LocalAkahColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("status_system_stacked_chart"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        systemBreakdowns.forEach { item ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.background,
                border = BorderStroke(1.dp, akah.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.system,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Op: ${item.operationalCount}",
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 11.sp,
                                color = operationalColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Maint: ${item.maintenanceRequiredCount}",
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 11.sp,
                                color = maintenanceColor,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "OOS: ${item.outOfServiceCount}",
                                fontFamily = IbmPlexMonoFontFamily,
                                fontSize = 11.sp,
                                color = outOfServiceColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(akah.line.copy(alpha = 0.4f)),
                        horizontalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        val total = item.total.coerceAtLeast(1).toFloat()
                        if (item.operationalCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(item.operationalCount / total)
                                    .fillMaxHeight()
                                    .background(operationalColor)
                            )
                        }
                        if (item.maintenanceRequiredCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(item.maintenanceRequiredCount / total)
                                    .fillMaxHeight()
                                    .background(maintenanceColor)
                            )
                        }
                        if (item.outOfServiceCount > 0) {
                            Box(
                                modifier = Modifier
                                    .weight(item.outOfServiceCount / total)
                                    .fillMaxHeight()
                                    .background(outOfServiceColor)
                            )
                        }
                    }
                }
            }
        }
    }
}
