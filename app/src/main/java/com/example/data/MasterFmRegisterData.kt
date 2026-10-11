package com.example.data

import com.example.model.AssetEntity
import com.example.model.SopJson
import com.example.model.SubUnitAssetItem

object MasterFmRegisterData {

    fun getAllMasterSeededAssets(): List<AssetEntity> {
        val coreEnriched = SeedAssetsData.getAllSeedAssets().map { enrichCoreAsset(it) }
        val additional = getAdditionalPlantAssets() + MasterFmResidentialData.getResidentialAndFurnitureAssets()
        return (coreEnriched + additional).sortedBy { it.id }
    }

    private fun enrichCoreAsset(asset: AssetEntity): AssetEntity {
        return when (asset.id) {
            "AKA-HYD-PWR-001" -> asset.copy(
                buildingName = "Sub station",
                location = "Sub station · DG yard",
                make = "Cummins / Kirloskar",
                model = "SGM750PR / JSP500K",
                serial = "N10K21664, CJK15030668",
                capacity = "750 kVA & 500 kVA",
                quantity = 2,
                purchaseYear = 2010,
                lifeCycleYears = 25,
                installed = "2010-06-01",
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("PWR-001-U1", 1, "Sub station", "DG yard", "", "DG 750KVA", "SGM750PR", "N10K21664", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("PWR-001-U2", 2, "Sub station", "DG yard", "", "DG 500KVA", "JSP500K", "CJK15030668", "", "", 1, 2015, 25, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-005" -> asset.copy(
                buildingName = "Sub station",
                location = "Sub station · HT ROOM",
                roomNo = "HT ROOM",
                make = "Kirloskar / Voltamp",
                model = "3CRT-RD-18579/B",
                serial = "JNC-18579/1, JNC-18579/2, JNC-18579/3",
                capacity = "3 × 750 kVA",
                quantity = 3,
                purchaseYear = 2010,
                lifeCycleYears = 20,
                installed = "2010-06-01",
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-005-U1", 3, "Sub station", "HT ROOM", "HT ROOM", "Transformer 750 KVA #1", "3CRT-RD-18579/B", "JNC-18579/1", "", "", 1, 2010, 20, "Needs attention"),
                        SubUnitAssetItem("ELE-005-U2", 4, "Sub station", "HT ROOM", "HT ROOM", "Transformer 750 KVA #2", "3CRT-RD-18579/B", "JNC-18579/2", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("ELE-005-U3", 5, "Sub station", "HT ROOM", "HT ROOM", "Transformer 750 KVA #3", "3CRT-RD-18579/B", "JNC-18579/3", "", "", 1, 2010, 20, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-006" -> asset.copy(
                buildingName = "Sub station",
                location = "Sub station · LT ROOM",
                roomNo = "LT ROOM",
                model = "300KVAR Panel-1, 2, 3",
                capacity = "3 × 300 kVAR",
                quantity = 3,
                purchaseYear = 2010,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-006-U1", 6, "Sub station", "LT ROOM", "LT ROOM", "300KVAR Panel-1", "300KVAR APFC", "CAP-300-1", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("ELE-006-U2", 7, "Sub station", "LT ROOM", "LT ROOM", "300KVAR Panel-2", "300KVAR APFC", "CAP-300-2", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("ELE-006-U3", 8, "Sub station", "LT ROOM", "LT ROOM", "300KVAR Panel-3", "300KVAR APFC", "CAP-300-3", "", "", 1, 2010, 20, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-003" -> asset.copy(
                buildingName = "Sub station & AB",
                location = "Sub station HT/LT ROOM & AB FF Ele Room",
                make = "ABB / L&T",
                model = "ABB-VMAX (630A), L&T (800A–1250A), ABB-SACE PR 121/P (1250A)",
                serial = "VM10/6069..6072, AU454321, JT440020..HT437302, RA8HAX17A..RA8ROX17A",
                capacity = "630A / 800A / 1000A / 1250A",
                quantity = 22,
                purchaseYear = 2010,
                lifeCycleYears = 30,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-003-U1", 38, "Sub station", "HT", "HT room", "630A BREAKER", "ABB-VMAX", "VM10/6069", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U2", 39, "Sub station", "HT", "HT room", "630A BREAKER", "ABB-VMAX", "VM10/6070", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U3", 40, "Sub station", "HT", "HT room", "630A BREAKER", "ABB-VMAX", "VM10/6072", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U4", 41, "Sub station", "HT", "HT room", "630A BREAKER", "ABB-VMAX", "VM10/6071", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U5", 42, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "AU454321", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U6", 43, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "CN-CS125051", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U7", 44, "Sub station", "LT ROOM", "LT ROOM", "1000A BREAKER", "L&T", "JT440020", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U8", 45, "Sub station", "LT ROOM", "LT ROOM", "1000A BREAKER", "L&T", "GT435277", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U9", 46, "Sub station", "LT ROOM", "LT ROOM", "800A BREAKER", "L&T", "KW556349", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U10", 47, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "HT437077", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U11", 48, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "JT441875", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U12", 49, "Sub station", "LT ROOM", "LT ROOM", "1000A BREAKER", "L&T", "FT432372", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U13", 50, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "JT441876", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U14", 51, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "JT441877", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U15", 52, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "L&T", "HT437076", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U16", 53, "AB", "FF", "Ele Room", "1000A BREAKER", "L&T", "HT437302", "", "", 1, 2010, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U17", 54, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RA8HAX17A", "", "", 1, 2015, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U18", 55, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RA7AXX17A", "", "", 1, 2015, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U19", 56, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RA81EX17A", "", "", 1, 2015, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U20", 57, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RRH4BQ17A", "", "", 1, 2015, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U21", 58, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RA88NX17A", "", "", 1, 2015, 30, "In service"),
                        SubUnitAssetItem("ELE-003-U22", 59, "Sub station", "LT ROOM", "LT ROOM", "1250A BREAKER", "ABB-SACE PR 121/P", "RA8ROX17A", "", "", 1, 2015, 30, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-002" -> asset.copy(
                buildingName = "All Campus Buildings",
                location = "Sub station, COMMONS, AB, SA1–3, JS1–3, SR1–12B, Facility, Nursery, DFR1, FR1, PR, WTP, Pump Room, STP",
                roomNo = "Ele Room GF / FF",
                quantity = 27,
                purchaseYear = 2010,
                lifeCycleYears = 25,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-002-U1", 66, "Sub station", "Sub station", "", "LT Panels (Substation)", "LT Panel", "", "", "", 10, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U2", 67, "COMMONS", "Ele Room", "GF", "LT Panels (COMMONS)", "LT Panel", "", "", "", 6, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U3", 68, "AB", "Ele Room", "FF", "LT Panels (AB)", "LT Panel", "", "", "", 4, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U4", 69, "SA1–SA3", "Ele Room", "GF", "LT Panels (Senior Academy SA1, SA2, SA3)", "LT Panel", "", "", "", 5, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U5", 72, "JS1–JS3", "Ele Room", "GF", "LT Panels (Junior School JS1, JS2, JS3)", "LT Panel", "", "", "", 4, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U6", 75, "SR1–SR4", "Ele Room", "GF", "LT Panels (Student Residences SR1–SR4)", "LT Panel", "", "", "", 8, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U7", 79, "SR9&10A/B, SR11&12A/B", "Ele Room", "GF", "LT Panels (SR9&10A/B, SR11&12A/B)", "LT Panel", "", "", "", 8, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U8", 83, "Facility, Nursery, DFR1, FR1, PR", "Ele Room", "GF", "LT Panels (Facility, Nursery, DFR1R/L, FR1R/L, PR)", "LT Panel", "", "", "", 7, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-002-U9", 90, "WTP, Pump Room, STP", "Plant Room", "GF", "LT Panels (WTP, Pump Room, STP)", "LT Panel", "", "", "", 9, 2010, 25, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-007" -> asset.copy(
                buildingName = "Campus Outdoor Ring",
                location = "Main gate, Nursery, Commons, SS-1, FR, SR, Near SR9&10A, PCM Back Side",
                roomNo = "GF Outdoor",
                quantity = 8,
                purchaseYear = 2010,
                lifeCycleYears = 25,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-007-U1", 96, "Main gate Right side", "Main gate Right side", "GF", "Feeder Pillars 1", "Outdoor Feeder Pillar", "FP-01", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U2", 97, "Nursery back side", "Nursery back side", "GF", "Feeder Pillars 2", "Outdoor Feeder Pillar", "FP-02", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U3", 98, "Commons Back Side", "Commons Back Side", "GF", "Feeder Pillars 3", "Outdoor Feeder Pillar", "FP-03", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U4", 99, "SS-1 Back Side", "SS-1 Back Side", "GF", "Feeder Pillars 4", "Outdoor Feeder Pillar", "FP-04", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U5", 100, "FR", "FR", "GF", "Feeder Pillars 5", "Outdoor Feeder Pillar", "FP-05", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U6", 101, "SR", "SR", "GF", "Feeder Pillars 6", "Outdoor Feeder Pillar", "FP-06", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U7", 102, "NEAR SR9&10A", "Near SR9&10A", "GF", "Feeder Pillars 7", "Outdoor Feeder Pillar", "FP-07", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("ELE-007-U8", 103, "PCM Back Side", "PCM Back Side", "GF", "Feeder Pillars 8", "Outdoor Feeder Pillar", "FP-08", "", "", 1, 2010, 25, "In service")
                    )
                )
            )

            "AKA-HYD-ELE-008" -> asset.copy(
                buildingName = "Sub station",
                location = "Sub Station HT Room",
                quantity = 5,
                purchaseYear = 2010,
                lifeCycleYears = 25,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("ELE-008-U1", 65, "Sub station", "Sub Station", "HT Room", "HT Panels (5 Panels)", "HT VCB Panel", "HT-PNL-1..5", "", "", 5, 2010, 25, "In service")
                    )
                )
            )

            "AKA-HYD-PWR-002" -> asset.copy(
                buildingName = "PR, AB & FR/SR/Academies",
                location = "PR Ele Room GF, AB IT Room SF, FR/SR & Academies GF",
                model = "20kVA UPS & Campus Online UPS",
                capacity = "20 kVA (52 units across campus)",
                quantity = 52,
                purchaseYear = 2010,
                lifeCycleYears = 12,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("PWR-002-U1", 93, "PR", "Ele Room", "GF", "UPS (PR Ele Room)", "Online UPS", "", "", "", 2, 2010, 12, "In service"),
                        SubUnitAssetItem("PWR-002-U2", 94, "AB", "IT ROOM", "SF", "20kva UPS (AB IT Room)", "20kVA Online UPS", "", "", "", 2, 2010, 12, "In service"),
                        SubUnitAssetItem("PWR-002-U3", 95, "FR,SR&Academies", "FR,SR&Academies", "GF", "UPS (FR, SR & Academies)", "Online UPS", "", "", "", 48, 2010, 12, "In service")
                    )
                )
            )

            "AKA-HYD-VT-001" -> asset.copy(
                buildingName = "AB, COMMONS, SR 11&12CA/CB",
                location = "Academic Block, Commons & Senior Residences",
                make = "Kone",
                model = "Kone 13-Person & 10-Person Passenger Lifts",
                serial = "11020025, 13040022, 13040023, 13040003, 13040017",
                capacity = "13 Persons (1) & 10 Persons (4)",
                quantity = 5,
                purchaseYear = 2010,
                lifeCycleYears = 25,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("VT-001-U1", 60, "AB", "AB", "", "13 PERSONS Lift (AB)", "Kone 40205636", "11020025", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("VT-001-U2", 61, "COMMONS", "COMMONS", "", "10 PERSONS Lift #1 (Commons)", "Kone 40262265", "13040022", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("VT-001-U3", 62, "COMMONS", "COMMONS", "", "10 PERSONS Lift #2 (Commons)", "Kone 40262266", "13040023", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("VT-001-U4", 63, "SR 11& 12CA", "SR 11& 12CA", "", "10 PERSONS Lift (SR 11&12CA)", "Kone 40262267", "13040003", "", "", 1, 2010, 25, "In service"),
                        SubUnitAssetItem("VT-001-U5", 64, "SR 11& 12CB", "SR 11& 12CB", "", "10 PERSONS Lift (SR 11&12CB)", "Kone 40262264", "13040017", "", "", 1, 2010, 25, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-005" -> asset.copy(
                buildingName = "Campus Perimeter & Sports Field",
                location = "Car Parking, Near Gate-2, Bus Parking, HT-Yard, On call, Facilities, SR-9&10, Sports field",
                model = "2C/50, V6, 7C/25, 4C/40, UQDs 152/17",
                capacity = "4 HP, 5 HP & 7.5 HP Submersible",
                quantity = 10,
                purchaseYear = 2010,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-005-U1", 104, "Car Parking", "Car Parking", "", "5HP BORE WELL-1", "5HP Submersible", "BW-01", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U2", 105, "Near Gate -2", "Near Gate -2", "", "4HP BORE WELL-2", "2C/50", "BW-02", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U3", 106, "BUS Parking", "Near WTP", "", "7.5HP BORE WELL-3", "V6", "BW-03", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U4", 107, "HT - Yard", "HT - Yard", "", "5HP BORE WELL-4", "7C/25", "BW-04", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U5", 108, "On call", "On call", "", "5HP BORE WELL-5", "4C/40", "BW-05", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U6", 109, "FACILITIES", "Facilities", "", "5HP BORE WELL-6", "4C/40", "BW-06", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U7", 110, "SR- 9 & 10", "SR- 9 & 10", "", "5HP BORE WELL-7", "4C/40", "BW-07", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U8", 111, "Sports field", "Sports field", "", "7.5HP BORE WELL-8", "UQDs 152/17", "BW-08", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U9", 112, "Sports field", "Sports field", "", "5HP BORE WELL-9", "7C/25", "BW-09", "", "", 1, 2015, 20, "In service"),
                        SubUnitAssetItem("WAT-005-U10", 113, "Sports field", "Sports field", "", "5HP BORE WELL-10", "7C/25", "BW-10", "", "", 1, 2015, 20, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-007" -> asset.copy(
                buildingName = "STP",
                location = "STP PLANT",
                model = "NADH-54A (15HP Blower), KDS-335 (Filter Feed), 3HP FAB Motor, MGF, ACF",
                serial = "JGAM 4685, JGAM 4689, AQA JFB 2828, AQA JEB 2829",
                quantity = 8,
                purchaseYear = 2010,
                lifeCycleYears = 10,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-007-U1", 30, "STP", "STP PLANT", "", "15HP AIR BLOWER #1", "NADH-54A", "JGAM 4685", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U2", 31, "STP", "STP PLANT", "", "15HP AIR BLOWER #2", "NADH-54A", "JGAM 4689", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U3", 32, "STP", "STP PLANT", "", "Filter feed Pump-1", "KDS-335", "AQA JFB 2828", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U4", 33, "STP", "STP PLANT", "", "Filter feed Pump-2", "KDS-335", "AQA JEB 2829", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U5", 34, "STP", "STP PLANT", "", "3HP FAB MOTOR-1 (Unit A)", "152", "FAB-152-A", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U6", 35, "STP", "STP PLANT", "", "3HP FAB MOTOR-1 (Unit B)", "152", "FAB-152-B", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U7", 36, "STP", "STP PLANT", "", "MGF (Multigrade Filter)", "MGF", "", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-007-U8", 37, "STP", "STP PLANT", "", "ACF (Activated Carbon Filter)", "ACF", "", "", "", 1, 2010, 10, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-008" -> asset.copy(
                buildingName = "WTP",
                location = "WTP OUTSIDE & WTP ROOM",
                model = "200KL Softener, 83315222 (7.5KW), A96568137P11042 (11KW)",
                serial = "101524, 101041, #2, #5, #6",
                capacity = "2 × 200KL Softener + 5 WTP Pumps",
                quantity = 7,
                purchaseYear = 2010,
                lifeCycleYears = 10,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-008-U1", 9, "WTP", "WTP OUT SIDE", "", "200KL SOFTENER #1", "200KL Softener", "", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U2", 10, "WTP", "WTP OUT SIDE", "", "200KL SOFTENER #2", "200KL Softener", "", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U3", 11, "WTP", "WTP ROOM", "", "7.5KW Water Pump #1", "83315222", "101524", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U4", 12, "WTP", "WTP ROOM", "", "7.5KW Water Pump #2", "83315222", "101041", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U5", 13, "WTP", "WTP ROOM", "", "11KW Water Pump #2", "A96568137P11042", "2", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U6", 14, "WTP", "WTP ROOM", "", "11KW Water Pump #6", "A96568137P11042", "6", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-008-U7", 15, "WTP", "WTP ROOM", "", "11KW Water Pump #5", "A96568137P11042", "5", "", "", 1, 2010, 10, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-010" -> asset.copy(
                buildingName = "AC3",
                location = "AC3 · Swimming Pool GF",
                roomNo = "GF",
                model = "7.5KW Water Pump & Sand Filter",
                serial = "U26AA42-12G66, U26AA42-12G67",
                quantity = 4,
                purchaseYear = 2010,
                lifeCycleYears = 10,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-010-U1", 26, "AC3", "Swimming Pool", "GF", "7.5KW Water Pump #1", "7.5KW Pool Pump", "U26AA42-12G66", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-010-U2", 27, "AC3", "Swimming Pool", "GF", "7.5KW Water Pump #2", "7.5KW Pool Pump", "U26AA42-12G67", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-010-U3", 28, "AC3", "Swimming Pool", "GF", "SAND FILTER #1", "Pool Sand Filter", "SF-01", "", "", 1, 2010, 10, "In service"),
                        SubUnitAssetItem("WAT-010-U4", 29, "AC3", "Swimming Pool", "GF", "SAND FILTER #2", "Pool Sand Filter", "SF-02", "", "", 1, 2010, 10, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-002" -> asset.copy(
                buildingName = "Academic, Common & Residential Blocks",
                location = "Academy Bldg, Senior/Junior School, Commons, PDC, SR-1..12B, FR-1..3, DFR-1..3, Nursery",
                make = "KENT",
                model = "KENT ELITE 11 MRO (50 LPH) & KENT SUPER (10 LPH)",
                serial = "KR120624773 .. KR120726456 (114 units)",
                capacity = "50 LPH & 10 LPH",
                quantity = 114,
                purchaseYear = 2012,
                lifeCycleYears = 15,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-002-U1", 142, "Academy Building (GF)", "Academy Building (GF)", "GF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120624773", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U2", 143, "Academy Building (1stF)", "Academy Building (1stF)", "1stF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120726456", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U3", 144, "Senior School-1 (GF)", "Senior School-1 (GF)", "GF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120623858", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U4", 145, "Senior School-1 (1stF)", "Senior School-1 (1stF)", "1stF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120623395", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U5", 146, "Senior School-1 (GF)", "Senior School-1 (GF)", "GF", "10 LPH KENT RO", "KENT SUPER", "KR130115709", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U6", 147, "Senior School-3 (1stF)", "Senior School-3 (1stF)", "1stF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR121013332", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U7", 148, "Junior School-1 (GF)", "Junior School-1 (GF)", "GF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120733085", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U8", 149, "Junior School-1 (1stF)", "Junior School-1 (1stF)", "1stF", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120736983", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U9", 150, "COMMON", "Common", "", "50 LPH KENT RO (5 Units)", "KENT ELITE 11 MRO", "KR120733586..KR121100742", "", "", 5, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U10", 155, "PDC", "PDC", "", "50 LPH KENT RO", "KENT ELITE 11 MRO", "KR120904692", "", "", 1, 2012, 15, "In service"),
                        SubUnitAssetItem("WAT-002-U11", 236, "Principal Residency", "Principal Residency", "", "10 LPH KENT RO", "KENT SUPER", "KR120607361", "", "", 1, 2012, 11, "In service"),
                        SubUnitAssetItem("WAT-002-U12", 249, "AC-1 & NURSERY", "AC-1 GF/FF & Nursery", "", "50 LPH KENT RO (7 Units)", "KENT ELITE 11 MRO", "KR120904691..KR120726456", "", "", 7, 2012, 11, "In service")
                    )
                )
            )

            "AKA-HYD-WAT-004" -> asset.copy(
                buildingName = "Student Residences, Academies, Commons & Athletics",
                location = "SR 1–12CB, Senior Academy 1/3, Junior Academy 1, Commons GF/FF, Athletic Centre, Facilities",
                make = "Blue Star",
                model = "SRA4080G/SS4080/SP4080 & SRA300SF/SS60120/SP60120",
                capacity = "80 Lt, 100 Lt & 120 Lt",
                quantity = 37,
                purchaseYear = 2012,
                lifeCycleYears = 12,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("WAT-004-U1", 358, "Student residence 1–4", "SR 1–4 GF/FF/SF", "", "100 Lt Water Coolers (12 Units)", "SRA4080G / SRA300SF", "", "", "", 12, 2012, 12, "In service"),
                        SubUnitAssetItem("WAT-004-U2", 370, "Student residence 9&10, 11&12", "SR 9&10, 11&12", "", "100 Lt Water Coolers (12 Units)", "SRA4080G/SS4080/SP4080", "", "", "", 12, 2012, 12, "In service"),
                        SubUnitAssetItem("WAT-004-U3", 382, "Senior & Junior Academy", "SA-1, SA-3, JA-1", "", "100 Lt / 120 Lt Water Coolers (6 Units)", "100L / 120L Cooler", "", "", "", 6, 2012, 12, "In service"),
                        SubUnitAssetItem("WAT-004-U4", 386, "Commons", "Commons GF & FF", "", "100 Lt Water Coolers (4 Units)", "SRA300SF,SS60120,SP60120", "", "", "", 4, 2012, 12, "In service"),
                        SubUnitAssetItem("WAT-004-U5", 392, "Athletic Centre & Facilities", "Athletic GF/FF & Facilities Office", "", "80 Lt / 100 Lt Water Coolers (3 Units)", "80L / 100L Cooler", "", "", "", 3, 2012, 12, "In service")
                    )
                )
            )

            "AKA-HYD-HVC-006" -> asset.copy(
                buildingName = "SR9&10A & SR11&12A",
                location = "SR9&10A Terrace & SR11&12A Terrace",
                roomNo = "Terrace",
                model = "HP500 (500HP Heat Pump)",
                serial = "201501022",
                capacity = "500 HP",
                quantity = 2,
                purchaseYear = 2013,
                lifeCycleYears = 15,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("HVC-006-U1", 256, "SR9&10A", "SR9&10A", "Terrace", "500HP HEAT PUMP #1", "HP500", "201501022", "", "", 1, 2013, 15, "In service"),
                        SubUnitAssetItem("HVC-006-U2", 257, "SR11&12A", "SR11&12A", "Terrace", "500HP HEAT PUMP #2", "HP500", "HP500-2", "", "", 1, 2013, 15, "In service")
                    )
                )
            )

            "AKA-HYD-REF-001" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons Kitchen GF & FF",
                roomNo = "GF / FF",
                model = "CR22KRMPE111DM / CR30K6MPEF1-111DM / CR22KRMPE111DE",
                serial = "VVA0119, WB-21381, JER-0013890",
                capacity = "1.5 TR & 2 TR Cold Rooms",
                quantity = 6,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("REF-001-U1", 122, "COMMONS", "Commons", "GF", "Cold room 1.5 TR #1", "CR22KRMPE111DM", "VVA0119", "", "", 1, 2012, 20, "In service"),
                        SubUnitAssetItem("REF-001-U2", 123, "COMMONS", "Commons", "GF", "Cold room 2 TR #2", "CR30K6MPEF1-111DM", "WB-21381", "", "", 1, 2012, 20, "In service"),
                        SubUnitAssetItem("REF-001-U3", 124, "COMMONS", "Commons", "FF", "Cold room 1.5 TR #3", "CR22KRMPE111DE", "JER-0013890", "", "", 1, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-REF-002" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons GF",
                roomNo = "GF",
                model = "CHF400A, KFE444HAG-B3000, GDP192, KCT511HAE0420H",
                serial = "14MO5486, AAR-0000344, 13M15359, THK134OYCF, KYA0075554, 14M00200, 06224ZAB600053L",
                quantity = 7,
                purchaseYear = 2012,
                lifeCycleYears = 12,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("REF-002-U1", 114, "COMMONS", "Commons", "GF", "Commercial Refrigerator #1", "CHF400A", "14MO5486", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U2", 115, "COMMONS", "Commons", "GF", "Commercial Refrigerator #2", "KFE444HAG-B3000", "AAR-0000344", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U3", 116, "COMMONS", "Commons", "GF", "Commercial Refrigerator #3", "CHF400A", "13M15359", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U4", 117, "COMMONS", "Commons", "GF", "Commercial Refrigerator #4", "GDP192", "THK134OYCF", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U5", 118, "COMMONS", "Commons", "GF", "Commercial Refrigerator #5", "KCT511HAE0420H", "KYA0075554", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U6", 119, "COMMONS", "Commons", "GF", "Commercial Refrigerator #6", "CHF400A", "14M00200", "", "", 1, 2012, 12, "In service"),
                        SubUnitAssetItem("REF-002-U7", 120, "COMMONS", "Commons", "GF", "Commercial Refrigerator #7", "CHF400A", "06224ZAB600053L", "", "", 1, 2012, 12, "In service")
                    )
                )
            )

            "AKA-HYD-REF-003" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons GF",
                roomNo = "GF",
                model = "90A-Q",
                quantity = 1,
                purchaseYear = 2012,
                lifeCycleYears = 12
            )

            "AKA-HYD-KIT-002" -> asset.copy(
                buildingName = "KITCHEN & SS-2",
                location = "Kitchen GF & Senior School SS-2 GF",
                roomNo = "GF",
                quantity = 2,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("KIT-002-U1", 140, "KITCHEN", "Kitchen", "GF", "Kitchen Gas Bank", "Manifold Gas Bank", "", "", "", 1, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-002-U2", 141, "SS-2", "SS-2", "GF", "Lab Gas Bank", "Lab LPG Manifold", "", "", "", 1, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-KIT-004" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons FF",
                roomNo = "FF",
                make = "Electrolux",
                model = "HABER/ERT",
                serial = "31910001",
                quantity = 1,
                purchaseYear = 2013,
                lifeCycleYears = 12
            )

            "AKA-HYD-KIT-005" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons GF",
                roomNo = "GF",
                model = "Tilting Wet Grinder 20L, Wheat Atta Machine, Potato/Coconut Peeler, Veg Cutter, Masala Mixer",
                capacity = "20 Litres",
                quantity = 6,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("KIT-005-U1", 125, "COMMONS", "Commons", "GF", "Tilting Wet Grinder", "20LITRES", "", "", "", 1, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-005-U2", 126, "COMMONS", "Commons", "GF", "Wheat atta machine", "Commercial", "", "", "", 2, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-005-U3", 127, "COMMONS", "Commons", "GF", "Potato peeler", "Commercial", "", "", "", 2, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-005-U4", 134, "COMMONS", "Commons", "GF", "Coconut peeler & Vegetable cutter", "Commercial", "", "", "", 3, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-005-U5", 137, "COMMONS", "Commons", "GF", "Masala Mixer", "Commercial", "", "", "", 5, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-KIT-006" -> asset.copy(
                buildingName = "COMMONS",
                location = "Commons GF",
                roomNo = "GF",
                model = "Triple Deck Bakery Oven, Commercial Gas Ranges (1/2/4 Burner), Dosa Plate, Bread Toaster/Cutter",
                quantity = 15,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("KIT-006-U1", 138, "COMMONS", "Commons", "GF", "Triple Deck Bakery Oven", "3-Deck", "", "", "", 2, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-006-U2", 128, "COMMONS", "Commons", "GF", "1 Burner Commercial Gas Range", "1-Burner", "", "", "", 1, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-006-U3", 129, "COMMONS", "Commons", "GF", "2 Burner Commercial Gas Range", "2-Burner", "", "", "", 4, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-006-U4", 130, "COMMONS", "Commons", "GF", "4 Burner Commercial Gas Range", "4-Burner", "", "", "", 2, 2012, 20, "In service"),
                        SubUnitAssetItem("KIT-006-U5", 131, "COMMONS", "Commons", "GF", "Dosa Plate, Bread Toaster & Cutter, Bain Marie", "Commercial", "", "", "", 6, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-HVC-001" -> asset.copy(
                buildingName = "SR1–SR12B, FR1–FR3E, DFR1–3, PR, Facilities, Nursery, JS1, Academy Bldg",
                location = "Student & Faculty Residences, JS1, PR, Facilities, Nursery, Academy Building",
                make = "Blue Star / Carrier",
                model = "3HW18VAO (1.5 TR), 3HW24VAO (2 TR), CDUI301RCKE (2.5 TR), 3HW12VBO (1 TR), 42KGL",
                serial = "11A00830 .. 11A00215 (341 units across campus)",
                capacity = "1.0 TR, 1.5 TR, 2.0 TR & 2.5 TR",
                quantity = 341,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("HVC-001-U1", 395, "SR1", "SR1", "", "Split ACs in SR1 (15 Units: 1.5/2/2.5 TR)", "3HW18VAO / 3HW24VAO / CDUI301RCKE", "11A00830..11A18512", "", "", 15, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U2", 410, "SR2", "SR2", "", "Split ACs in SR2 (15 Units: 1.5/2/2.5 TR)", "3HW18VAO / 3HW24VAO / CDUI301RCKE", "11A10042..11A00862", "", "", 15, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U3", 425, "SR3 & SR4", "SR3 & SR4", "", "Split ACs in SR3 & SR4 (30 Units: 1.5/2 TR)", "3HW18VAO / 3HW24VAO", "", "", "", 30, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U4", 455, "SR9&10A & SR9&10B", "SR9&10A & B", "", "Split ACs in SR9&10A/B (60 Units: 1.5/2 TR)", "3HW18VAO / 3HW24VAO", "", "", "", 60, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U5", 515, "SR11&12A & SR11&12B", "SR11&12A & B", "", "Split ACs in SR11&12A/B (48 Units: 1.5/2 TR)", "3HW18VAO / 3HW24VAO", "", "", "", 48, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U6", 563, "FR2A–FR2E", "FR2A, FR2B, FR2C, FR2D, FR2E", "", "Split ACs in FR2 Block (50 Units: 1/2 TR)", "3HW12VBO / 3HW24SVBO / SILVER-1", "112A06095..113A01674", "", "", 50, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U7", 613, "FR3A–FR3E", "FR3A, FR3B, FR3C, FR3D, FR3E", "", "Split ACs in FR3 Block (60 Units: 1.5/2 TR)", "3HW18SVBO / 3HW24SVBO", "112A04845..112A00988", "", "", 60, 2013, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U8", 673, "DFR1–DFR3 & FR1", "DFR1, DFR2, DFR3, FR1", "", "Split ACs in DFR1–3 & FR1 (36 Units: 1/1.5/2 TR)", "3HW12VBO / 3HW18SVBO / 3HW24SVBO", "", "", "", 36, 2013, 20, "In service"),
                        SubUnitAssetItem("HVC-001-U9", 709, "PR, Facilities, Nursery, JS1, Academy Bldg", "PR, Facilities, Nursery, JS1", "", "Split ACs in PR (9), Facilities (2), Nursery (2), JS1 (12), Academy (2)", "3HW24VA / 42KGL-O24C / 3HW18VAO", "40211V04725..11A00215", "", "", 27, 2013, 20, "In service")
                    )
                )
            )

            "AKA-HYD-HVC-002" -> asset.copy(
                buildingName = "COMMONS, Academy Building, JS2, JS3, JS4, SS1, SS2, SS3",
                location = "Commons, Academy Building, Junior School 2–4, Senior School 1–3",
                make = "Blue Star",
                model = "BVRF12TCE (12HP), BVRF14TCE (14HP), BVRF18TCE (18HP), BVRF21TCE (21HP)",
                serial = "11F00011..10F00260 (38 ODU units)",
                capacity = "12 HP, 14 HP, 18 HP & 21 HP",
                quantity = 38,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("HVC-002-U1", 735, "COMMONS", "COMMONS", "", "14HP VRF (Commons)", "BVRF14TCE", "BVRF14-CMN", "", "", 1, 2010, 20, "In service"),
                        SubUnitAssetItem("HVC-002-U2", 772, "Academy Building", "Academy Building", "", "18HP VRF Units (8 Units)", "BVRF18TCE", "11F00011, 10F00177, 11F00013, 11F00015, 11F00010, 11F00003, 11F00012", "", "", 8, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-002-U3", 774, "Academy Building", "Academy Building", "", "21HP VRF Units (6 Units)", "BVRF21TCE", "11F00013, 11F00264, 10F00261, 10F00260, 11F00007, 10F00151", "", "", 6, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-002-U4", 775, "Academy Building", "Academy Building", "", "12HP & 14HP VRF Units (6 Units)", "BVRF12TCE / BVRF14TCE", "11F00062, 11F00120, 11F00007, 11F00073, 11F00002, 11F00003", "", "", 6, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-002-U5", 795, "JS2, JS3, JS4", "Junior School 2, 3, 4", "", "18HP & 14HP VRF Units (5 Units)", "BVRF18TCE / BVRF14TCE", "11F00002, 11F00043, 11F00030, 11F00057, 11F00099", "", "", 5, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-002-U6", 801, "SS1, SS2, SS3", "Senior School 1, 2, 3", "", "14HP & 18HP VRF Units (12 Units)", "BVRF14TCE / BVRF18TCE", "12F00001..10F00260", "", "", 12, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-HVC-003" -> asset.copy(
                buildingName = "COMMONS, ATHLETICS, SQUASH COURT, SS2",
                location = "Commons, Athletics, Squash Court & Senior School 2",
                model = "DSA1021R1 (8.5TR), DSA1321R1 (11TR), DSA361R1 (3TR), DPA661R1 (5.5TR)",
                serial = "DA12F00133, DA12F00137, DA12F00273, DA12F00275",
                capacity = "3 TR, 5.5 TR, 8.5 TR & 11 TR",
                quantity = 17,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("HVC-003-U1", 736, "COMMONS", "COMMONS", "", "8.5TR, 11TR & 3TR Ductable ACs (Commons)", "DSA1021R1 / DSA1321R1 / DSA361R1", "", "", "", 3, 2010, 20, "In service"),
                        SubUnitAssetItem("HVC-003-U2", 758, "ATHLETICS", "Athletics", "", "8.5TR & 5.5TR Ductable ACs (Athletics)", "DSA1021R1 / DPA661R1", "", "", "", 2, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-003-U3", 768, "SQUASH COURT", "Squash Court", "", "8.5TR Ductable ACs (4 Units)", "DSA1021R1", "DA12F00133, DA12F00137, DA12F00273, DA12F00275", "", "", 4, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-003-U4", 812, "SS2", "SS2", "", "5.5TR (6) & 8.5TR (2) Ductable ACs (SS2)", "DPA661R1 / DPA1021R1", "", "", "", 8, 2012, 20, "In service")
                    )
                )
            )

            "AKA-HYD-HVC-004" -> asset.copy(
                buildingName = "COMMONS, ATHLETICS, JS2",
                location = "Commons, Commons Kitchen, Athletics & JS2",
                model = "SCR481 (4TR), DSA361R1 / SCR36VSE (3TR), SCR241 (2TR), SCR181 (1.5TR), SCR361 (1TR)",
                capacity = "1 TR, 1.5 TR, 2 TR, 3 TR & 4 TR",
                quantity = 7,
                purchaseYear = 2012,
                lifeCycleYears = 20
            )

            "AKA-HYD-HVC-005" -> asset.copy(
                buildingName = "JS1, COMMONS KITCHEN, ATHLETICS, Academy Bldg, SS1, SS2",
                location = "Commons Kitchen, Athletics, JS1, Academy Building, SS1 & SS2",
                model = "SACFT / SASU / SWAU / SAGM / SWACU / SAGET / ETV-040VV / EIU-02500",
                serial = "90000558, 27023..26005, SAGFT-18*18F-10000547..599, 11-12VV1043..1045, 90000485..494",
                capacity = "600 CFM to 26,100 CFM Exhaust & TFA Units",
                quantity = 31,
                purchaseYear = 2012,
                lifeCycleYears = 20,
                subUnitsJson = SopJson.encodeSubUnits(
                    listOf(
                        SubUnitAssetItem("HVC-005-U1", 734, "JS1", "JS1", "", "Exhaust Fan 2000 CFM", "SACFT-9X9", "90000558", "", "", 1, 2013, 20, "In service"),
                        SubUnitAssetItem("HVC-005-U2", 743, "Commons", "COMMONS KITCHEN", "", "Kitchen Exhaust Fans (600–26100 CFM, 8 Units)", "SACFT / SASU-170 / SWAU-450", "27023..27029, 20722, 21009, 21010", "", "", 8, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-005-U3", 751, "Commons", "COMMONS KITCHEN", "", "Kitchen TFA Units (2000–25500 CFM, 4 Units)", "SAGM-120 / SACFT / SWACU-450", "10126, 27027, 27021, 26005", "", "", 4, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-005-U4", 762, "ATHLETICS", "Athletics", "", "Athletics Exhaust Fans (5000 & 1000 CFM, 6 Units)", "SAGET-18*188", "SAGFT-18*18F-10000547..599", "", "", 6, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-005-U5", 793, "Academy Building & SS1", "Academy Building & SS1", "", "Exhaust Fans 4000 CFM (2) & 2500 CFM (1)", "ETV-040VV / EIU-02500", "11-12VV1043..1045", "", "", 3, 2012, 20, "In service"),
                        SubUnitAssetItem("HVC-005-U6", 820, "SS2", "SS2", "", "SS2 Exhaust Fans (5) & TFA 1300/2500 CFM (4)", "SACFT-12X12 / SACFT-9X9 / SAGM-20", "90000485..90000494", "", "", 9, 2012, 20, "In service")
                    )
                )
            )

            else -> asset
        }
    }

    private fun getAdditionalPlantAssets(): List<AssetEntity> = listOf(
        AssetEntity(
            id = "AKA-HYD-WAT-012",
            sr = 49,
            cat = "WAT",
            name = "Irrigation Water Pumps – Sports Field & Pump Room",
            system = "Water & wastewater",
            category = "Mechanical / Irrigation",
            criticality = "Medium",
            status = "In service",
            buildingName = "IRRIGATION",
            location = "SPORTS field & IRRIGATION PUMP ROOM",
            make = "Grundfos / Kirloskar",
            model = "A96500511P11143, A96517047P11139, A96500509P10836, AP96122014P11124, AP96122800P11124",
            serial = "S.No 1, 2, 3, 4 (Ser No 16 to 25)",
            capacity = "1.5 kW, 2.2 kW, 4 kW, 5.5 kW & 11 kW",
            quantity = 10,
            purchaseYear = 2010,
            lifeCycleYears = 10,
            owner = "Mechanical & Landscape Supervisor",
            strategy = "PPM + seasonal irrigation pressure & current monitoring",
            routine = "D: discharge pressure, leaks, abnormal vibration during turf watering; W: strainer clean, auto timer & dry-run protection check",
            ppm = "M: motor current, gland packing/seal check; Q: NRV & foot valve clean, starter contactor inspect; H: motor IR test; A: impeller & bearing overhaul",
            statutory = "SOP AKAH-EH-AM-A1-2026; CEA Safety Regs 2023; IS 732 earthing & RCD protection",
            records = "Discharge pressure bar; motor current A; voltage V; run hours h; IR MΩ",
            limits = "Current ≤ nameplate FLC; IR ≥ 1 MΩ at 500 V DC; no dry running",
            subUnitsJson = SopJson.encodeSubUnits(
                listOf(
                    SubUnitAssetItem("WAT-012-U1", 16, "IRRIGATION", "SPORTS field", "", "5.5KW Water Pump #3", "A96500511P11143", "3", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U2", 17, "IRRIGATION", "SPORTS field", "", "5.5KW Water Pump #2", "A96500511P11143", "2", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U3", 18, "IRRIGATION", "SPORTS field", "", "2.2KW Water Pump #1", "A96517047P11139", "1", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U4", 19, "IRRIGATION", "PUMP ROOM", "", "4KW Water Pump #4", "A96500509P10836", "4", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U5", 20, "IRRIGATION", "PUMP ROOM", "", "11KW Water Pump #2", "AP96122014P11124", "2", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U6", 21, "IRRIGATION", "PUMP ROOM", "", "11KW Water Pump #1", "AP96122014P11124", "1", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U7", 22, "IRRIGATION", "PUMP ROOM", "", "1.5KW Water Pump #1", "AP96516663P11124", "1", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U8", 23, "IRRIGATION", "PUMP ROOM", "", "11KW Water Pump #1 (Set B)", "AP96122800P11124", "1", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U9", 24, "IRRIGATION", "PUMP ROOM", "", "11KW Water Pump #2 (Set B)", "AP96122800P11124", "2", "", "", 1, 2010, 10, "In service"),
                    SubUnitAssetItem("WAT-012-U10", 25, "IRRIGATION", "PUMP ROOM", "", "1.5KW Water Pump #2", "AP6517044P11124", "2", "", "", 1, 2010, 10, "In service")
                )
            )
        ),
        AssetEntity(
            id = "AKA-HYD-LND-001",
            sr = 50,
            cat = "LND",
            name = "Central Laundry Plant – Washer, Dryer, Hydro & Boiler",
            system = "Laundry & appliances",
            category = "Laundry Equipment",
            criticality = "Critical",
            status = "In service",
            buildingName = "Facility Building",
            location = "Facility Building · LAUNDRY",
            model = "30kg Washer, SVGMTD-30 (30kg Dryer), 15kg Hydro, 2-Table Boiler, SVGMVIT (1/2HP Vacuum Table)",
            capacity = "30 kg Washer & Dryer / 15 kg Hydro / 0.5 HP Vacuum Tables",
            quantity = 6,
            purchaseYear = 2013,
            lifeCycleYears = 15,
            owner = "Housekeeping & Laundry Supervisor",
            strategy = "PPM + daily lint/steam safety checks; OEM service",
            routine = "D: clean lint screen after every shift, check door interlocks, steam pressure & drain valves; W: V-belt tension, drum seal, vacuum table foot pedal",
            ppm = "M: lubricate bearings, check heating elements & steam traps; Q: motor amps, earthing & RCD test; A: boiler pressure vessel & safety valve statutory test, full electrical IR check",
            statutory = "SOP AKAH-EH-AM-A1-2026; Indian Boilers regulations (for steam generator); IS 732 wiring",
            records = "Motor current A; drying temp °C; boiler pressure bar; door interlock pass/fail; lint filter cleaned",
            limits = "Door interlock must halt drum immediately; IR ≥ 1 MΩ; boiler relief valve within red-line pressure",
            subUnitsJson = SopJson.encodeSubUnits(
                listOf(
                    SubUnitAssetItem("LND-001-U1", 352, "Facility Building", "LAUNDRY", "", "30kg Washing machine", "Commercial 30kg", "", "", "", 1, 2013, 15, "In service"),
                    SubUnitAssetItem("LND-001-U2", 353, "Facility Building", "LAUNDRY", "", "30kg Tumble Dryer machine", "SVGMTD-30", "SVGMTD-30", "", "", 1, 2013, 15, "In service"),
                    SubUnitAssetItem("LND-001-U3", 354, "Facility Building", "LAUNDRY", "", "15kg hydro machine", "15kg Hydro Extractor", "", "", "", 1, 2013, 15, "In service"),
                    SubUnitAssetItem("LND-001-U4", 355, "Facility Building", "LAUNDRY", "", "2 Table Boiler machine", "2-Table Steam Boiler", "", "", "", 1, 2013, 15, "In service"),
                    SubUnitAssetItem("LND-001-U5", 356, "Facility Building", "LAUNDRY", "", "1/2HP VACCUM TABLE #1", "SVGMVIT", "SVGMVIT-1", "", "", 1, 2013, 15, "In service"),
                    SubUnitAssetItem("LND-001-U6", 357, "Facility Building", "LAUNDRY", "", "1/2HP VACCUM TABLE #2", "SVGMVIT", "SVGMVIT-2", "", "", 1, 2013, 15, "In service")
                )
            )
        ),
        AssetEntity(
            id = "AKA-HYD-LND-002",
            sr = 51,
            cat = "LND",
            name = "Residential Washing Machines – 6kg / 6.5kg (SR, FR & DFR)",
            system = "Laundry & appliances",
            category = "Residential Appliances",
            criticality = "Medium",
            status = "In service",
            buildingName = "SR-1..12B, FR-1..3, DFR-1..3, Principal Residency",
            location = "Student Residences, Faculty Residences & DFR Blocks (Ser No 258–351)",
            make = "IFB / Samsung",
            model = "SENORITA VX (6kg), SENORITA AQVA (6.5kg), Samsung (6kg)",
            serial = "18797 .. 18663 (94 machines)",
            capacity = "6 kg & 6.5 kg Front Load",
            quantity = 94,
            purchaseYear = 2012,
            lifeCycleYears = 11,
            owner = "Residential Facilities Coordinator",
            strategy = "PPM + quarterly descaling & filter clean",
            routine = "W: inlet hose leak check, drain filter clean, door boot seal wipe, vibration check during spin",
            ppm = "Q: drum descaling cycle, inlet solenoid mesh clean, check 16A plug & RCD; A: shock absorber/bearing check, earth continuity & IR test",
            statutory = "SOP AKAH-EH-AM-A1-2026; IS 302-2-7; 30 mA RCCB protection",
            records = "Earth continuity Ω; IR MΩ; descaling date; drain pump & door lock status",
            limits = "Earth continuity < 0.5 Ω; IR ≥ 1 MΩ; zero water leakage at inlet/drain",
            subUnitsJson = SopJson.encodeSubUnits(
                listOf(
                    SubUnitAssetItem("LND-002-U1", 258, "SR-1", "SR-1 GF/1stF/2ndF (Student & Faculty)", "", "6kg Washing machines in SR-1 (6 Units)", "IFB SENORITA VX", "18797, 18805, 18814, 18216, 18809, 65268", "", "", 6, 2012, 11, "In service"),
                    SubUnitAssetItem("LND-002-U2", 264, "SR-2", "SR-2 GF/1stF/2ndF (Student & Faculty)", "", "6kg Washing machines in SR-2 (6 Units)", "Samsung / IFB SENORITA VX", "50576, 25640, 86059, 18819, 18797, 27026", "", "", 6, 2012, 11, "In service"),
                    SubUnitAssetItem("LND-002-U3", 270, "SR-3 & SR-4", "SR-3 & SR-4 GF/1stF/2ndF", "", "6kg Washing machines in SR-3 & SR-4 (12 Units)", "IFB SENORITA VX / Samsung", "19745, 50739, 49807..19757", "", "", 12, 2012, 11, "In service"),
                    SubUnitAssetItem("LND-002-U4", 282, "SR-9,10 & SR-9B,10B", "SR-9,10A & B", "", "6kg / 6.5kg Washing machines in SR-9 & 10 (18 Units)", "SENORITA VX / AQVA", "18808, 14164, 26113..19745", "", "", 18, 2012, 11, "In service"),
                    SubUnitAssetItem("LND-002-U5", 300, "SR-11,12 & SR-11B,12B", "SR-11,12A & B", "", "6kg / 6.5kg Washing machines in SR-11 & 12 (18 Units)", "SENORITA VX / AQVA", "50725, 48013..14196", "", "", 18, 2012, 11, "In service"),
                    SubUnitAssetItem("LND-002-U6", 318, "FR-1, FR-2, FR-3, DFR-1..3, PR, LAUNDER", "Faculty Residences & Laundry", "", "6kg / 6.5kg Washing machines in FR/DFR (34 Units)", "IFB SENORITA VX", "26418..18663", "", "", 34, 2012, 11, "In service")
                )
            )
        )
    )
}
