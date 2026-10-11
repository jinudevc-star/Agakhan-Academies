package com.example

import com.example.data.SeedAssetsData
import com.example.model.SopScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun seedAssets_hasAll48CampusAssets() {
        val assets = SeedAssetsData.getAllSeedAssets()
        assertEquals(48, assets.size)
        assertEquals("AKA-HYD-ELE-001", assets.first().id)
        assertEquals("AKA-HYD-WAT-011", assets.last().id)
    }

    @Test
    fun qrCodeParser_extractsAssetIdFromUrlsAndRawCodes() {
        assertEquals(
            "AKA-HYD-ELE-001",
            SopScheduleParser.extractAssetIdFromQr("http://192.168.1.20:8090/#AKA-HYD-ELE-001")
        )
        assertEquals(
            "AKA-HYD-FLS-005",
            SopScheduleParser.extractAssetIdFromQr("https://script.google.com/macros/s/abc/exec?asset=AKA-HYD-FLS-005")
        )
        assertEquals(
            "AKA-HYD-WAT-010",
            SopScheduleParser.extractAssetIdFromQr("AKA-HYD-WAT-010")
        )
    }

    @Test
    fun sopChecklistParser_parsesFrequenciesAndReadings() {
        val asset = SeedAssetsData.getAllSeedAssets().first { it.id == "AKA-HYD-ELE-001" }
        val groups = SopScheduleParser.checklist(asset)
        assertTrue(groups.isNotEmpty())
        val daily = groups.find { it.code == "D" }
        assertNotNull(daily)
        assertTrue(daily!!.items.any { it.text.contains("door closed/locked") })

        val readings = SopScheduleParser.readingsOf(asset)
        assertTrue(readings.contains("IR MΩ per circuit"))
    }
}
