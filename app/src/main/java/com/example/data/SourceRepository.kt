package com.example.data

import android.content.Context
import com.example.model.MediaSourceConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

class SourceRepository(private val context: Context) {
    private val prefs = context.getSharedPreferences("media_browser_sources", Context.MODE_PRIVATE)
    private val _sources = MutableStateFlow<List<MediaSourceConfig>>(emptyList())
    val sources: StateFlow<List<MediaSourceConfig>> = _sources.asStateFlow()

    init {
        loadSources()
    }

    fun loadSources() {
        val savedJson = prefs.getString("custom_sources", null)
        val customSources = mutableListOf<MediaSourceConfig>()
        if (!savedJson.isNullOrBlank()) {
            try {
                val array = JSONArray(savedJson)
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    customSources.add(MediaSourceConfig.fromJson(obj))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val allSources = MediaSourceConfig.DEFAULT_SOURCES + customSources
        _sources.value = allSources
    }

    fun addSource(source: MediaSourceConfig) {
        val currentCustom = _sources.value.filter { !it.isBuiltIn }.toMutableList()
        currentCustom.removeAll { it.id == source.id }
        currentCustom.add(0, source)
        saveCustomSources(currentCustom)
    }

    fun updateSource(source: MediaSourceConfig) {
        addSource(source)
    }

    fun deleteSource(sourceId: String) {
        val currentCustom = _sources.value.filter { !it.isBuiltIn && it.id != sourceId }
        saveCustomSources(currentCustom)
    }

    private fun saveCustomSources(customSources: List<MediaSourceConfig>) {
        val array = JSONArray()
        for (source in customSources) {
            array.put(source.toJson())
        }
        prefs.edit().putString("custom_sources", array.toString()).apply()
        _sources.value = MediaSourceConfig.DEFAULT_SOURCES + customSources
    }

    fun getSourceById(id: String): MediaSourceConfig? {
        return _sources.value.firstOrNull { it.id == id }
    }
}
