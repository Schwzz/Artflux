package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.CuratedMediaData
import com.example.model.MediaRating
import com.example.model.MediaSourceConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Media Browser", appName)
  }

  @Test
  fun testCuratedMediaDataIntegrity() {
    assertTrue(CuratedMediaData.ITEMS.isNotEmpty())
    val firstItem = CuratedMediaData.ITEMS.first()
    assertNotNull(firstItem.imageUrl)
    assertNotNull(firstItem.thumbnailUrl)
    assertEquals(MediaRating.SAFE, firstItem.rating)
  }

  @Test
  fun testMediaSourceConfigSerialization() {
    val config = MediaSourceConfig(
      name = "Test API",
      apiUrl = "https://api.example.com/v1/search",
      searchParam = "query",
      pageParam = "p",
      imageUrlField = "img_url"
    )

    val json = config.toJson()
    val restored = MediaSourceConfig.fromJson(json)

    assertEquals(config.name, restored.name)
    assertEquals(config.apiUrl, restored.apiUrl)
    assertEquals(config.searchParam, restored.searchParam)
    assertEquals(config.pageParam, restored.pageParam)
    assertEquals(config.imageUrlField, restored.imageUrlField)
  }
}
