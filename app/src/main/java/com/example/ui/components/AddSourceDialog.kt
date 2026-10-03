package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.GeminiAiSetupService
import com.example.data.MediaApiClient
import com.example.model.MediaSourceConfig
import com.example.ui.theme.CardBorder
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.NeonIndigo
import com.example.ui.theme.NeonIndigoLight
import com.example.ui.theme.RoseBadge
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import kotlinx.coroutines.launch

@Composable
fun AddSourceDialog(
    initialConfig: MediaSourceConfig? = null,
    onDismiss: () -> Unit,
    onSave: (MediaSourceConfig) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: AI Setup, 1: Manual, 2: Templates
    val coroutineScope = rememberCoroutineScope()
    val aiService = remember { GeminiAiSetupService() }
    val apiClient = remember { MediaApiClient() }

    // Form fields
    var sourceName by remember { mutableStateOf(initialConfig?.name ?: "") }
    var apiUrl by remember { mutableStateOf(initialConfig?.apiUrl ?: "") }
    var searchParam by remember { mutableStateOf(initialConfig?.searchParam ?: "q") }
    var pageParam by remember { mutableStateOf(initialConfig?.pageParam ?: "page") }
    var itemsPath by remember { mutableStateOf(initialConfig?.itemsPath ?: "") }
    var imageUrlField by remember { mutableStateOf(initialConfig?.imageUrlField ?: "url") }
    var thumbUrlField by remember { mutableStateOf(initialConfig?.thumbUrlField ?: "thumbnail") }
    var postUrlField by remember { mutableStateOf(initialConfig?.postUrlField ?: "link") }
    var tagsField by remember { mutableStateOf(initialConfig?.tagsField ?: "tags") }
    var ratingField by remember { mutableStateOf(initialConfig?.ratingField ?: "rating") }
    var mediaTypeField by remember { mutableStateOf(initialConfig?.mediaTypeField ?: "media_type") }
    var gifQueryTag by remember { mutableStateOf(initialConfig?.gifQueryTag ?: "") }
    var videoQueryTag by remember { mutableStateOf(initialConfig?.videoQueryTag ?: "") }
    var safeRatingTag by remember { mutableStateOf(initialConfig?.safeRatingTag ?: "") }
    var suggestiveRatingTag by remember { mutableStateOf(initialConfig?.suggestiveRatingTag ?: "") }
    var adultRatingTag by remember { mutableStateOf(initialConfig?.adultRatingTag ?: "") }
    var apiKey by remember { mutableStateOf(initialConfig?.apiKey ?: "") }
    var apiKeyHeader by remember { mutableStateOf(initialConfig?.apiKeyHeader ?: "") }

    // AI Setup state
    var aiInputText by remember { mutableStateOf("") }
    var isAnalyzingAi by remember { mutableStateOf(false) }
    var aiErrorMessage by remember { mutableStateOf<String?>(null) }
    var aiSuccessMessage by remember { mutableStateOf<String?>(null) }

    // Testing state
    var isTestingSource by remember { mutableStateOf(false) }
    var testResultStatus by remember { mutableStateOf<String?>(null) }
    var testIsSuccess by remember { mutableStateOf<Boolean?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (initialConfig == null) "Add Media Source" else "Edit Source",
                            style = MaterialTheme.typography.titleLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Configure custom REST API endpoints or use AI Setup",
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .testTag("close_add_source_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }
                }

                // Tabs: AI Setup, Manual Form, Quick Templates
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = TextPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = NeonIndigo
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = if (selectedTab == 0) NeonIndigoLight else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("AI Setup", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tune,
                                    contentDescription = null,
                                    tint = if (selectedTab == 1) NeonIndigoLight else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Manual Config", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ViewList,
                                    contentDescription = null,
                                    tint = if (selectedTab == 2) NeonIndigoLight else TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Templates", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (selectedTab) {
                        0 -> {
                            // --- AI Setup Tab ---
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = NeonIndigoLight,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Smart AI Source Analyzer",
                                            color = TextPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }

                                    Text(
                                        text = "Paste API documentation or an example JSON response below. Gemini AI analyzes the schema and auto-populates all field mappings for you.",
                                        color = TextSecondary,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
                                    )

                                    OutlinedTextField(
                                        value = aiInputText,
                                        onValueChange = {
                                            aiInputText = it
                                            aiErrorMessage = null
                                            aiSuccessMessage = null
                                        },
                                        placeholder = {
                                            Text(
                                                text = "Paste JSON response, curl command, or API docs here...\n\nExample:\n{\n  \"data\": [\n    {\n      \"id\": 101,\n      \"url\": \"https://...\",\n      \"thumbnail\": \"https://...\",\n      \"tags\": [\"art\", \"cyberpunk\"]\n    }\n  ]\n}",
                                                color = TextTertiary,
                                                fontSize = 12.sp
                                            )
                                        },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp)
                                            .testTag("ai_input_text_field"),
                                        shape = RoundedCornerShape(10.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color(0xFF0F1523),
                                            unfocusedContainerColor = Color(0xFF0F1523),
                                            focusedBorderColor = NeonIndigo,
                                            unfocusedBorderColor = CardBorder,
                                            focusedTextColor = TextPrimary,
                                            unfocusedTextColor = TextPrimary
                                        )
                                    )

                                    Spacer(modifier = Modifier.height(10.dp))

                                    // Quick Sample Paste and Analyze buttons
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                aiInputText = SAMPLE_API_JSON
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.ContentPaste,
                                                contentDescription = null,
                                                tint = CyanAccent,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Paste Sample JSON", color = CyanAccent, fontSize = 11.sp)
                                        }

                                        Button(
                                            onClick = {
                                                coroutineScope.launch {
                                                    isAnalyzingAi = true
                                                    aiErrorMessage = null
                                                    aiSuccessMessage = null
                                                    val result = aiService.analyzeApiSpecOrJson(aiInputText)
                                                    isAnalyzingAi = false
                                                    result.onSuccess { parsed ->
                                                        sourceName = parsed.name
                                                        apiUrl = parsed.apiUrl
                                                        searchParam = parsed.searchParam
                                                        pageParam = parsed.pageParam
                                                        itemsPath = parsed.itemsPath
                                                        imageUrlField = parsed.imageUrlField
                                                        thumbUrlField = parsed.thumbUrlField
                                                        postUrlField = parsed.postUrlField
                                                        tagsField = parsed.tagsField
                                                        ratingField = parsed.ratingField
                                                        mediaTypeField = parsed.mediaTypeField
                                                        aiSuccessMessage = "Successfully generated configuration for '${parsed.name}'! Switch to Manual Config tab to review or click Save below."
                                                    }.onFailure { err ->
                                                        aiErrorMessage = err.message ?: "Failed to analyze structure."
                                                    }
                                                }
                                            },
                                            enabled = !isAnalyzingAi && aiInputText.isNotBlank(),
                                            colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.testTag("ai_analyze_button")
                                        ) {
                                            if (isAnalyzingAi) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(16.dp),
                                                    color = TextPrimary,
                                                    strokeWidth = 2.dp
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Analyzing...", fontSize = 12.sp)
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    tint = TextPrimary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text("Analyze with AI", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                    }

                                    // Status alerts
                                    if (aiSuccessMessage != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                            colors = CardDefaults.cardColors(containerColor = EmeraldSafe.copy(alpha = 0.15f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldSafe)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.CheckCircle,
                                                    contentDescription = null,
                                                    tint = EmeraldSafe,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = aiSuccessMessage!!,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }

                                    if (aiErrorMessage != null) {
                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 10.dp),
                                            colors = CardDefaults.cardColors(containerColor = RoseBadge.copy(alpha = 0.15f)),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, RoseBadge)
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Error,
                                                    contentDescription = null,
                                                    tint = RoseBadge,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = aiErrorMessage!!,
                                                    color = TextPrimary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        1 -> {
                            // --- Manual Configuration Form ---
                            FormSectionTitle("1. Basic Information")
                            FormTextField(
                                label = "Source Name *",
                                value = sourceName,
                                onValueChange = { sourceName = it },
                                placeholder = "e.g., Wallhaven Wallpapers",
                                testTag = "input_source_name"
                            )
                            FormTextField(
                                label = "API Endpoint URL *",
                                value = apiUrl,
                                onValueChange = { apiUrl = it },
                                placeholder = "https://api.example.com/v1/search",
                                testTag = "input_api_url"
                            )

                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            FormSectionTitle("2. Request Parameters")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Search Query Param",
                                        value = searchParam,
                                        onValueChange = { searchParam = it },
                                        placeholder = "q",
                                        testTag = "input_search_param"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Page Number Param",
                                        value = pageParam,
                                        onValueChange = { pageParam = it },
                                        placeholder = "page",
                                        testTag = "input_page_param"
                                    )
                                }
                            }

                            FormTextField(
                                label = "Items Array JSON Path",
                                value = itemsPath,
                                onValueChange = { itemsPath = it },
                                placeholder = "e.g. data or results (leave empty if root is array)",
                                testTag = "input_items_path"
                            )

                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            FormSectionTitle("3. JSON Field Mappings")
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Image URL Field *",
                                        value = imageUrlField,
                                        onValueChange = { imageUrlField = it },
                                        placeholder = "url or download_url",
                                        testTag = "input_image_field"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Thumbnail URL Field",
                                        value = thumbUrlField,
                                        onValueChange = { thumbUrlField = it },
                                        placeholder = "thumbnail or preview_url",
                                        testTag = "input_thumb_field"
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Post/Webpage URL Field",
                                        value = postUrlField,
                                        onValueChange = { postUrlField = it },
                                        placeholder = "link or post_url",
                                        testTag = "input_post_field"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Tags Field",
                                        value = tagsField,
                                        onValueChange = { tagsField = it },
                                        placeholder = "tags or labels",
                                        testTag = "input_tags_field"
                                    )
                                }
                            }

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Rating Field",
                                        value = ratingField,
                                        onValueChange = { ratingField = it },
                                        placeholder = "rating or purity",
                                        testTag = "input_rating_field"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Media Type Field",
                                        value = mediaTypeField,
                                        onValueChange = { mediaTypeField = it },
                                        placeholder = "media_type or type",
                                        testTag = "input_media_type_field"
                                    )
                                }
                            }

                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            FormSectionTitle("4. Media Discovery Tags (Optional)")
                            Text(
                                text = "Tags or keywords used by this API to find animated GIFs and videos (e.g., animated, webm).",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "GIF Query Tag",
                                        value = gifQueryTag,
                                        onValueChange = { gifQueryTag = it },
                                        placeholder = "e.g. animated",
                                        testTag = "input_gif_query_tag"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Video Query Tag",
                                        value = videoQueryTag,
                                        onValueChange = { videoQueryTag = it },
                                        placeholder = "e.g. video or webm",
                                        testTag = "input_video_query_tag"
                                    )
                                }
                            }

                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            FormSectionTitle("5. Server-Side Rating Tags (Optional)")
                            Text(
                                text = "Tags or keywords used by this API to filter content ratings (e.g. rating:g, rating:s, rating:e).",
                                color = TextSecondary,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Safe Tag",
                                        value = safeRatingTag,
                                        onValueChange = { safeRatingTag = it },
                                        placeholder = "e.g. rating:g",
                                        testTag = "input_safe_rating_tag"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Suggestive Tag",
                                        value = suggestiveRatingTag,
                                        onValueChange = { suggestiveRatingTag = it },
                                        placeholder = "e.g. rating:s,q",
                                        testTag = "input_suggestive_rating_tag"
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    FormTextField(
                                        label = "Adult Tag",
                                        value = adultRatingTag,
                                        onValueChange = { adultRatingTag = it },
                                        placeholder = "e.g. rating:e",
                                        testTag = "input_adult_rating_tag"
                                    )
                                }
                            }

                            HorizontalDivider(color = CardBorder, modifier = Modifier.padding(vertical = 4.dp))
                            FormSectionTitle("6. Authentication (Optional)")
                            FormTextField(
                                label = "API Key / Token",
                                value = apiKey,
                                onValueChange = { apiKey = it },
                                placeholder = "Leave empty if not required",
                                testTag = "input_api_key"
                            )
                            FormTextField(
                                label = "Header Name (or empty if query param)",
                                value = apiKeyHeader,
                                onValueChange = { apiKeyHeader = it },
                                placeholder = "e.g. Authorization or X-API-KEY",
                                testTag = "input_api_key_header"
                            )
                        }

                        2 -> {
                            // --- Quick Templates Tab ---
                            Text(
                                text = "Select a pre-made source template to immediately populate the configuration fields:",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )

                            SOURCE_TEMPLATES.forEach { template ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp))
                                        .clickable {
                                            sourceName = template.name
                                            apiUrl = template.apiUrl
                                            searchParam = template.searchParam
                                            pageParam = template.pageParam
                                            itemsPath = template.itemsPath
                                            imageUrlField = template.imageUrlField
                                            thumbUrlField = template.thumbUrlField
                                            postUrlField = template.postUrlField
                                            tagsField = template.tagsField
                                            ratingField = template.ratingField
                                            mediaTypeField = template.mediaTypeField
                                            gifQueryTag = template.gifQueryTag
                                            videoQueryTag = template.videoQueryTag
                                            safeRatingTag = template.safeRatingTag
                                            suggestiveRatingTag = template.suggestiveRatingTag
                                            adultRatingTag = template.adultRatingTag
                                            selectedTab = 1 // Switch to manual config tab
                                        },
                                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                                ) {
                                    Column(modifier = Modifier.padding(14.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = template.name,
                                                color = TextPrimary,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = "USE TEMPLATE",
                                                color = NeonIndigoLight,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp
                                            )
                                        }
                                        Text(
                                            text = template.description,
                                            color = TextSecondary,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(top = 4.dp)
                                        )
                                        Text(
                                            text = template.apiUrl,
                                            color = TextTertiary,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Test Source Feedback Banner
                    if (testResultStatus != null) {
                        val isOk = testIsSuccess == true
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isOk) EmeraldSafe.copy(alpha = 0.15f) else RoseBadge.copy(alpha = 0.15f)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (isOk) EmeraldSafe else RoseBadge)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isOk) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (isOk) EmeraldSafe else RoseBadge,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = testResultStatus!!,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Bar: Test Source + Save Source
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Test Button
                    OutlinedButton(
                        onClick = {
                            if (apiUrl.isBlank()) {
                                testResultStatus = "Please enter an API URL before testing."
                                testIsSuccess = false
                                return@OutlinedButton
                            }
                            coroutineScope.launch {
                                isTestingSource = true
                                testResultStatus = null
                                val testConfig = MediaSourceConfig(
                                    name = sourceName.ifBlank { "Test Source" },
                                    apiUrl = apiUrl.trim(),
                                    searchParam = searchParam,
                                    pageParam = pageParam,
                                    itemsPath = itemsPath,
                                    imageUrlField = imageUrlField,
                                    thumbUrlField = thumbUrlField,
                                    postUrlField = postUrlField,
                                    tagsField = tagsField,
                                    ratingField = ratingField,
                                    mediaTypeField = mediaTypeField,
                                    gifQueryTag = gifQueryTag.trim(),
                                    videoQueryTag = videoQueryTag.trim(),
                                    safeRatingTag = safeRatingTag.trim(),
                                    suggestiveRatingTag = suggestiveRatingTag.trim(),
                                    adultRatingTag = adultRatingTag.trim(),
                                    apiKey = apiKey,
                                    apiKeyHeader = apiKeyHeader
                                )
                                val result = apiClient.fetchMedia(testConfig, query = "", page = 1)
                                isTestingSource = false
                                result.onSuccess { items ->
                                    if (items.isNotEmpty()) {
                                        testIsSuccess = true
                                        testResultStatus = "✓ Test Successful! Found ${items.size} media items. First item: '${items[0].title}'."
                                    } else {
                                        testIsSuccess = true
                                        testResultStatus = "✓ Connected to API, but 0 items returned for initial page."
                                    }
                                }.onFailure { error ->
                                    testIsSuccess = false
                                    testResultStatus = "✗ Test Failed: ${error.message}"
                                }
                            }
                        },
                        enabled = !isTestingSource && apiUrl.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("test_source_button")
                    ) {
                        if (isTestingSource) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = NeonIndigoLight,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Testing...", fontSize = 12.sp)
                        } else {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = NeonIndigoLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Test Source", color = NeonIndigoLight, fontSize = 12.sp)
                        }
                    }

                    // Save Button
                    Button(
                        onClick = {
                            if (sourceName.isBlank() || apiUrl.isBlank()) {
                                testResultStatus = "Please provide both Source Name and API Endpoint URL."
                                testIsSuccess = false
                                return@Button
                            }

                            val newConfig = MediaSourceConfig(
                                id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
                                name = sourceName.trim(),
                                apiUrl = apiUrl.trim(),
                                searchParam = searchParam.trim(),
                                pageParam = pageParam.trim(),
                                itemsPath = itemsPath.trim(),
                                imageUrlField = imageUrlField.trim(),
                                thumbUrlField = thumbUrlField.trim(),
                                postUrlField = postUrlField.trim(),
                                tagsField = tagsField.trim(),
                                ratingField = ratingField.trim(),
                                mediaTypeField = mediaTypeField.trim(),
                                gifQueryTag = gifQueryTag.trim(),
                                videoQueryTag = videoQueryTag.trim(),
                                safeRatingTag = safeRatingTag.trim(),
                                suggestiveRatingTag = suggestiveRatingTag.trim(),
                                adultRatingTag = adultRatingTag.trim(),
                                apiKey = apiKey.trim(),
                                apiKeyHeader = apiKeyHeader.trim(),
                                isBuiltIn = false,
                                description = "User configured source"
                            )
                            onSave(newConfig)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.testTag("save_source_button")
                    ) {
                        Text(
                            text = if (initialConfig == null) "Save Source" else "Update Source",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun FormSectionTitle(title: String) {
    Text(
        text = title,
        color = NeonIndigoLight,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            color = TextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = TextTertiary, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurface,
                unfocusedContainerColor = DarkSurface,
                focusedBorderColor = NeonIndigo,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
    }
}

private const val SAMPLE_API_JSON = """{
  "total": 540,
  "data": [
    {
      "id": "item_901",
      "name": "Cybernetic Sakura Blossom",
      "path": "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=1200",
      "preview": "https://images.unsplash.com/photo-1508739773434-c26b3d09e071?q=80&w=400",
      "post_url": "https://example.com/art/901",
      "tags": ["cyberpunk", "japan", "neon", "cherry-blossom"],
      "purity": "safe",
      "type": "illustration",
      "author": "Kenji Takahashi"
    }
  ]
}"""

private val SOURCE_TEMPLATES = listOf(
    MediaSourceConfig(
        name = "Safebooru Endpoint",
        apiUrl = "https://safebooru.org/index.php?page=dapi&s=post&q=index&json=1",
        searchParam = "tags",
        pageParam = "pid",
        pageStartsAt = 0,
        itemsPath = "",
        imageUrlField = "sample_url",
        thumbUrlField = "preview_url",
        postUrlField = "id",
        tagsField = "tags",
        ratingField = "rating",
        gifQueryTag = "animated",
        videoQueryTag = "",
        safeRatingTag = "rating:general",
        suggestiveRatingTag = "",
        adultRatingTag = "",
        description = "Public anime illustration archive with tag-based search."
    ),
    MediaSourceConfig(
        name = "Danbooru JSON API",
        apiUrl = "https://danbooru.donmai.us/posts.json",
        searchParam = "tags",
        pageParam = "page",
        pageStartsAt = 1,
        itemsPath = "",
        imageUrlField = "file_url",
        thumbUrlField = "preview_file_url",
        postUrlField = "id",
        tagsField = "tag_string",
        ratingField = "rating",
        gifQueryTag = "animated",
        videoQueryTag = "webm",
        safeRatingTag = "rating:g",
        suggestiveRatingTag = "rating:s,q",
        adultRatingTag = "rating:e",
        description = "Standard Danbooru-compatible REST endpoint returning JSON post objects."
    ),
    MediaSourceConfig(
        name = "Moebooru REST API",
        apiUrl = "https://konachan.net/post.json",
        searchParam = "tags",
        pageParam = "page",
        pageStartsAt = 1,
        itemsPath = "",
        imageUrlField = "file_url",
        thumbUrlField = "preview_url",
        postUrlField = "id",
        tagsField = "tags",
        ratingField = "rating",
        gifQueryTag = "animated",
        videoQueryTag = "",
        safeRatingTag = "rating:s",
        suggestiveRatingTag = "rating:q",
        adultRatingTag = "rating:e",
        description = "Moebooru format endpoint for anime wallpapers and illustrations."
    )
)
