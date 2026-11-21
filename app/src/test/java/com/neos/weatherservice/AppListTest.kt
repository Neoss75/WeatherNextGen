package com.neos.weatherservice

import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.Collator
import java.util.Locale

class AppListTest {

    @Test fun `known iGO builds come first, then the rest by name`() {
        val apps = listOf(
            AppEntry("Яндекс Карты", "ru.yandex.yandexmaps"),
            AppEntry("Radio", "com.example.radio"),
            AppEntry("iGO Pioneer", "com.nng.igo.pioneer"),
            AppEntry("Браузер", "com.example.browser"),
            AppEntry("iGO Primo", "com.nng.igo.primong"),
            AppEntry("iGO Primo", "com.nng.igo.primong"), // two launcher icons, one package
            AppEntry("avic sync", "jp.pioneer.mbg.avicsync"),
            AppEntry("Музыка", "com.example.music"),
        )
        val known = listOf("jp.pioneer.mbg.avicsync", "com.nng.igo.primong", "com.nng.igo.pioneer")
        val sorted = AppList.sort(apps, known, Collator.getInstance(Locale("ru", "RU")))
        assertEquals(
            listOf(
                "jp.pioneer.mbg.avicsync", "com.nng.igo.primong", "com.nng.igo.pioneer",
                "com.example.radio", "com.example.browser", "com.example.music", "ru.yandex.yandexmaps",
            ),
            sorted.map { it.packageName },
        )
    }
}
