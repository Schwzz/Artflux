package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.MediaApiClient
import com.example.model.AuthParam
import com.example.model.AuthType
import com.example.model.DiagnosticStatus
import com.example.model.MediaSourceConfig
import com.example.model.SourceDiagnosticReport
import com.example.ui.theme.EmeraldSafe
import com.example.ui.theme.RoseBadge
import kotlinx.coroutines.launch

enum class AddSourceStep {
    ENTRY_CHOICE,
    TEMPLATES,
    IMPORT_JSON,
    MANUAL_FORM,
    REVIEW_AND_TEST
}

@Composable
fun AddSourceDialog(
    initialConfig: MediaSourceConfig? = null,
    onDismiss: () -> Unit,
    onSave: (MediaSourceConfig) -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val apiClient = remember { MediaApiClient() }

    var currentStep by remember {
        mutableStateOf(
            if (initialConfig != null) AddSourceStep.MANUAL_FORM else AddSourceStep.ENTRY_CHOICE
        )
    }

    // Form fields state
    var sourceName by remember { mutableStateOf(initialConfig?.name ?: "") }
    var apiUrl by remember { mutableStateOf(initialConfig?.apiUrl ?: "") }
    var searchParam by remember { mutableStateOf(initialConfig?.searchParam ?: "tags") }
    var pageParam by remember { mutableStateOf(initialConfig?.pageParam ?: "page") }
    var pageStartsAt by remember { mutableIntStateOf(initialConfig?.pageStartsAt ?: 1) }
    var pageSizeParam by remember { mutableStateOf(initialConfig?.pageSizeParam ?: "limit") }
    var defaultPageSize by remember { mutableIntStateOf(initialConfig?.defaultPageSize ?: 25) }
    var itemsPath by remember { mutableStateOf(initialConfig?.itemsPath ?: "") }
    var imageUrlField by remember { mutableStateOf(initialConfig?.imageUrlField ?: "file_url") }
    var thumbUrlField by remember { mutableStateOf(initialConfig?.thumbUrlField ?: "preview_url") }
    var sampleUrlField by remember { mutableStateOf(initialConfig?.sampleUrlField ?: "sample_url") }
    var postUrlField by remember { mutableStateOf(initialConfig?.postUrlField ?: "id") }
    var tagsField by remember { mutableStateOf(initialConfig?.tagsField ?: "tags") }
    var ratingField by remember { mutableStateOf(initialConfig?.ratingField ?: "rating") }
    var mediaTypeField by remember { mutableStateOf(initialConfig?.mediaTypeField ?: "") }
    var titleField by remember { mutableStateOf(initialConfig?.titleField ?: "tags") }
    var authorField by remember { mutableStateOf(initialConfig?.authorField ?: "owner") }
    var description by remember { mutableStateOf(initialConfig?.description ?: "") }

    var gifQueryTag by remember { mutableStateOf(initialConfig?.gifQueryTag ?: "") }
    var videoQueryTag by remember { mutableStateOf(initialConfig?.videoQueryTag ?: "") }
    var safeRatingTag by remember { mutableStateOf(initialConfig?.safeRatingTag ?: "") }
    var suggestiveRatingTag by remember { mutableStateOf(initialConfig?.suggestiveRatingTag ?: "") }
    var adultRatingTag by remember { mutableStateOf(initialConfig?.adultRatingTag ?: "") }

    // Authentication State
    var authType by remember { mutableStateOf(initialConfig?.authType ?: AuthType.NONE) }
    var authHeaderName by remember { mutableStateOf(initialConfig?.authHeaderName ?: "") }
    var authHeaderValue by remember { mutableStateOf(initialConfig?.authHeaderValue ?: "") }
    var authQueryParams by remember {
        mutableStateOf(
            if (initialConfig != null && initialConfig.authQueryParams.isNotEmpty()) {
                initialConfig.authQueryParams
            } else if (initialConfig != null && initialConfig.apiKey.isNotBlank() && initialConfig.apiKeyInQuery) {
                listOf(AuthParam(initialConfig.apiKeyQueryParam.ifBlank { "api_key" }, initialConfig.apiKey))
            } else {
                listOf(AuthParam("api_key", ""))
            }
        )
    }

    // Import JSON State
    var importJsonText by remember { mutableStateOf("") }
    var importError by remember { mutableStateOf<String?>(null) }
    var promptCopiedFeedback by remember { mutableStateOf(false) }

    // Testing / Diagnostics State
    var isTestingSource by remember { mutableStateOf(false) }
    var diagnosticReport by remember { mutableStateOf<SourceDiagnosticReport?>(null) }

    fun populateFromConfig(config: MediaSourceConfig) {
        sourceName = config.name
        apiUrl = config.apiUrl
        searchParam = config.searchParam
        pageParam = config.pageParam
        pageStartsAt = config.pageStartsAt
        pageSizeParam = config.pageSizeParam
        defaultPageSize = config.defaultPageSize
        itemsPath = config.itemsPath
        imageUrlField = config.imageUrlField
        thumbUrlField = config.thumbUrlField
        sampleUrlField = config.sampleUrlField
        postUrlField = config.postUrlField
        tagsField = config.tagsField
        ratingField = config.ratingField
        mediaTypeField = config.mediaTypeField
        titleField = config.titleField
        authorField = config.authorField
        description = config.description
        gifQueryTag = config.gifQueryTag
        videoQueryTag = config.videoQueryTag
        safeRatingTag = config.safeRatingTag
        suggestiveRatingTag = config.suggestiveRatingTag
        adultRatingTag = config.adultRatingTag
        authType = config.authType
        authHeaderName = config.authHeaderName
        authHeaderValue = config.authHeaderValue
        authQueryParams = if (config.authQueryParams.isNotEmpty()) {
            config.authQueryParams
        } else if (config.apiKey.isNotBlank() && config.apiKeyInQuery) {
            listOf(AuthParam(config.apiKeyQueryParam.ifBlank { "api_key" }, config.apiKey))
        } else {
            listOf(AuthParam("api_key", ""))
        }
    }

    fun buildCurrentConfig(): MediaSourceConfig {
        return MediaSourceConfig(
            id = initialConfig?.id ?: java.util.UUID.randomUUID().toString(),
            name = sourceName.trim().ifBlank { "Custom Source" },
            apiUrl = apiUrl.trim(),
            searchParam = searchParam.trim(),
            pageParam = pageParam.trim(),
            pageStartsAt = pageStartsAt,
            pageSizeParam = pageSizeParam.trim(),
            defaultPageSize = defaultPageSize,
            itemsPath = itemsPath.trim(),
            imageUrlField = imageUrlField.trim(),
            thumbUrlField = thumbUrlField.trim(),
            sampleUrlField = sampleUrlField.trim(),
            postUrlField = postUrlField.trim(),
            tagsField = tagsField.trim(),
            ratingField = ratingField.trim(),
            mediaTypeField = mediaTypeField.trim(),
            titleField = titleField.trim(),
            authorField = authorField.trim(),
            description = description.trim(),
            gifQueryTag = gifQueryTag.trim(),
            videoQueryTag = videoQueryTag.trim(),
            safeRatingTag = safeRatingTag.trim(),
            suggestiveRatingTag = suggestiveRatingTag.trim(),
            adultRatingTag = adultRatingTag.trim(),
            authType = authType,
            authHeaderName = authHeaderName.trim(),
            authHeaderValue = authHeaderValue.trim(),
            authQueryParams = authQueryParams.filter { it.key.isNotBlank() },
            isBuiltIn = false
        )
    }

    fun runDiagnostics(targetConfig: MediaSourceConfig = buildCurrentConfig()) {
        coroutineScope.launch {
            isTestingSource = true
            diagnosticReport = null
            val report = apiClient.diagnoseSource(targetConfig)
            diagnosticReport = report
            isTestingSource = false
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (currentStep != AddSourceStep.ENTRY_CHOICE && initialConfig == null) {
                            IconButton(
                                onClick = {
                                    currentStep = when (currentStep) {
                                        AddSourceStep.REVIEW_AND_TEST -> AddSourceStep.MANUAL_FORM
                                        else -> AddSourceStep.ENTRY_CHOICE
                                    }
                                },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                        }

                        Column {
                            Text(
                                text = when (currentStep) {
                                    AddSourceStep.ENTRY_CHOICE -> "Add Media Source"
                                    AddSourceStep.TEMPLATES -> "Source Templates"
                                    AddSourceStep.IMPORT_JSON -> "Import Configuration"
                                    AddSourceStep.MANUAL_FORM -> if (initialConfig == null) "Manual Source Setup" else "Edit Source"
                                    AddSourceStep.REVIEW_AND_TEST -> "Review & Diagnostics"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp
                            )
                            Text(
                                text = when (currentStep) {
                                    AddSourceStep.ENTRY_CHOICE -> "Choose configuration method"
                                    AddSourceStep.TEMPLATES -> "Select a pre-made imageboard endpoint"
                                    AddSourceStep.IMPORT_JSON -> "Paste structured Artflux JSON configuration"
                                    AddSourceStep.MANUAL_FORM -> "Configure endpoint, mappings & authentication"
                                    AddSourceStep.REVIEW_AND_TEST -> "Verify connection and response before saving"
                                },
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .testTag("close_add_source_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Step Content
                Box(modifier = Modifier.weight(1f)) {
                    when (currentStep) {
                        AddSourceStep.ENTRY_CHOICE -> {
                            EntryChoiceScreen(
                                onSelectTemplates = { currentStep = AddSourceStep.TEMPLATES },
                                onSelectImport = { currentStep = AddSourceStep.IMPORT_JSON },
                                onSelectManual = { currentStep = AddSourceStep.MANUAL_FORM }
                            )
                        }

                        AddSourceStep.TEMPLATES -> {
                            TemplatesSelectionScreen(
                                onSelectTemplate = { template ->
                                    populateFromConfig(template)
                                    currentStep = AddSourceStep.REVIEW_AND_TEST
                                    runDiagnostics(template)
                                }
                            )
                        }

                        AddSourceStep.IMPORT_JSON -> {
                            ImportJsonScreen(
                                jsonText = importJsonText,
                                onJsonTextChange = {
                                    importJsonText = it
                                    importError = null
                                },
                                errorMessage = importError,
                                promptCopied = promptCopiedFeedback,
                                onCopyPrompt = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = ClipData.newPlainText("Artflux AI Prompt", MediaSourceConfig.AI_PROMPT_TEMPLATE)
                                    clipboard.setPrimaryClip(clip)
                                    promptCopiedFeedback = true
                                },
                                onPasteFromClipboard = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        importJsonText = text
                                        importError = null
                                    }
                                },
                                onValidateAndImport = {
                                    val parseResult = MediaSourceConfig.parseJson(importJsonText)
                                    parseResult.onSuccess { parsedConfig ->
                                        populateFromConfig(parsedConfig)
                                        currentStep = AddSourceStep.REVIEW_AND_TEST
                                        runDiagnostics(parsedConfig)
                                    }.onFailure { error ->
                                        importError = error.message ?: "Failed to parse JSON"
                                    }
                                }
                            )
                        }

                        AddSourceStep.MANUAL_FORM -> {
                            ManualSetupForm(
                                sourceName = sourceName,
                                onSourceNameChange = { sourceName = it },
                                apiUrl = apiUrl,
                                onApiUrlChange = { apiUrl = it },
                                searchParam = searchParam,
                                onSearchParamChange = { searchParam = it },
                                pageParam = pageParam,
                                onPageParamChange = { pageParam = it },
                                pageStartsAt = pageStartsAt,
                                onPageStartsAtChange = { pageStartsAt = it },
                                pageSizeParam = pageSizeParam,
                                onPageSizeParamChange = { pageSizeParam = it },
                                defaultPageSize = defaultPageSize,
                                onDefaultPageSizeChange = { defaultPageSize = it },
                                itemsPath = itemsPath,
                                onItemsPathChange = { itemsPath = it },
                                imageUrlField = imageUrlField,
                                onImageUrlFieldChange = { imageUrlField = it },
                                thumbUrlField = thumbUrlField,
                                onThumbUrlFieldChange = { thumbUrlField = it },
                                sampleUrlField = sampleUrlField,
                                onSampleUrlFieldChange = { sampleUrlField = it },
                                postUrlField = postUrlField,
                                onPostUrlFieldChange = { postUrlField = it },
                                tagsField = tagsField,
                                onTagsFieldChange = { tagsField = it },
                                ratingField = ratingField,
                                onRatingFieldChange = { ratingField = it },
                                mediaTypeField = mediaTypeField,
                                onMediaTypeFieldChange = { mediaTypeField = it },
                                titleField = titleField,
                                onTitleFieldChange = { titleField = it },
                                authorField = authorField,
                                onAuthorFieldChange = { authorField = it },
                                gifQueryTag = gifQueryTag,
                                onGifQueryTagChange = { gifQueryTag = it },
                                videoQueryTag = videoQueryTag,
                                onVideoQueryTagChange = { videoQueryTag = it },
                                safeRatingTag = safeRatingTag,
                                onSafeRatingTagChange = { safeRatingTag = it },
                                suggestiveRatingTag = suggestiveRatingTag,
                                onSuggestiveRatingTagChange = { suggestiveRatingTag = it },
                                adultRatingTag = adultRatingTag,
                                onAdultRatingTagChange = { adultRatingTag = it },
                                authType = authType,
                                onAuthTypeChange = { authType = it },
                                authHeaderName = authHeaderName,
                                onAuthHeaderNameChange = { authHeaderName = it },
                                authHeaderValue = authHeaderValue,
                                onAuthHeaderValueChange = { authHeaderValue = it },
                                authQueryParams = authQueryParams,
                                onAuthQueryParamsChange = { authQueryParams = it }
                            )
                        }

                        AddSourceStep.REVIEW_AND_TEST -> {
                            ReviewAndTestScreen(
                                config = buildCurrentConfig(),
                                report = diagnosticReport,
                                isTesting = isTestingSource,
                                onRunTest = { runDiagnostics() },
                                onEditDetails = { currentStep = AddSourceStep.MANUAL_FORM }
                            )
                        }
                    }
                }

                // Bottom Action Footer
                if (currentStep == AddSourceStep.MANUAL_FORM || currentStep == AddSourceStep.REVIEW_AND_TEST) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (currentStep == AddSourceStep.MANUAL_FORM) {
                                    val config = buildCurrentConfig()
                                    currentStep = AddSourceStep.REVIEW_AND_TEST
                                    runDiagnostics(config)
                                } else {
                                    runDiagnostics()
                                }
                            },
                            enabled = !isTestingSource && apiUrl.isNotBlank(),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("test_source_button")
                        ) {
                            if (isTestingSource) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...", fontSize = 12.sp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Test Diagnostics", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp)
                            }
                        }

                        Button(
                            onClick = {
                                val validation = MediaSourceConfig.validateConfig(buildCurrentConfig())
                                if (validation.isEmpty()) {
                                    onSave(buildCurrentConfig())
                                }
                            },
                            enabled = sourceName.isNotBlank() && apiUrl.isNotBlank(),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("save_source_button")
                        ) {
                            Text(
                                text = if (initialConfig == null) "Save Source" else "Update Source",
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EntryChoiceScreen(
    onSelectTemplates: () -> Unit,
    onSelectImport: () -> Unit,
    onSelectManual: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "How would you like to add it?",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        // 1. Templates Choice Card
        EntryChoiceCard(
            title = "Templates",
            badge = "QUICKEST",
            badgeColor = EmeraldSafe,
            description = "Choose from ready-to-use booru & imageboard templates including Safebooru, Danbooru, Yande.re, Gelbooru, and Moebooru.",
            icon = Icons.Default.ViewList,
            iconTint = MaterialTheme.colorScheme.primary,
            testTag = "choice_templates",
            onClick = onSelectTemplates
        )

        // 2. Import Configuration Card (AI Workflow)
        EntryChoiceCard(
            title = "Import Configuration",
            badge = "RECOMMENDED",
            badgeColor = MaterialTheme.colorScheme.primary,
            description = "Paste a structured JSON configuration generated with an external AI (ChatGPT, Claude, Gemini) or shared by a friend.",
            icon = Icons.Default.UploadFile,
            iconTint = MaterialTheme.colorScheme.secondary,
            testTag = "choice_import_json",
            onClick = onSelectImport
        )

        // 3. Manual Setup Card
        EntryChoiceCard(
            title = "Manual Setup",
            badge = "ADVANCED",
            badgeColor = MaterialTheme.colorScheme.tertiary,
            description = "Directly specify REST API endpoints, query parameters, JSON response mappings, media tags, and multi-key authentication.",
            icon = Icons.Default.Tune,
            iconTint = MaterialTheme.colorScheme.tertiary,
            testTag = "choice_manual_setup",
            onClick = onSelectManual
        )
    }
}

@Composable
fun EntryChoiceCard(
    title: String,
    badge: String,
    badgeColor: Color,
    description: String,
    icon: ImageVector,
    iconTint: Color,
    testTag: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f))
                    .border(1.dp, iconTint.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = title,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(badgeColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badge,
                            color = badgeColor,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
fun TemplatesSelectionScreen(
    onSelectTemplate: (MediaSourceConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Select a source template to prefill parameters. You will be able to review, adjust, and test it before saving:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 4.dp)
        )

        MediaSourceConfig.SOURCE_TEMPLATES.forEach { template ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clickable { onSelectTemplate(template) }
                    .testTag("template_${template.name.replace(" ", "_")}"),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = template.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "USE TEMPLATE",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Text(
                        text = template.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Text(
                        text = template.apiUrl,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    if (template.authType != AuthType.NONE || template.authQueryParams.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Requires API Credentials (api_key, user_id)",
                                color = MaterialTheme.colorScheme.secondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ImportJsonScreen(
    jsonText: String,
    onJsonTextChange: (String) -> Unit,
    errorMessage: String?,
    promptCopied: Boolean,
    onCopyPrompt: () -> Unit,
    onPasteFromClipboard: () -> Unit,
    onValidateAndImport: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // AI Workflow Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "External AI Workflow",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Button(
                        onClick = onCopyPrompt,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (promptCopied) EmeraldSafe else MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("copy_ai_prompt_button")
                    ) {
                        Icon(
                            imageVector = if (promptCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (promptCopied) "Copied!" else "Copy AI Prompt",
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Text(
                    text = "Copy our optimized prompt, paste it into ChatGPT, Claude, or Gemini alongside any API documentation, and paste the generated JSON below.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }

        // Paste action header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Configuration JSON",
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )

            OutlinedButton(
                onClick = onPasteFromClipboard,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("paste_clipboard_button")
            ) {
                Icon(
                    imageVector = Icons.Default.ContentPaste,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Paste from Clipboard", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
            }
        }

        // JSON text input
        OutlinedTextField(
            value = jsonText,
            onValueChange = onJsonTextChange,
            placeholder = {
                Text(
                    text = "{\n  \"name\": \"My Booru Source\",\n  \"apiUrl\": \"https://api.example.com/posts\",\n  \"searchParam\": \"tags\",\n  \"imageUrlField\": \"file_url\"\n}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp
                )
            },
            minLines = 9,
            maxLines = 14,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("import_json_input"),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surface,
                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            textStyle = MaterialTheme.typography.bodySmall.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )
        )

        // Error message card
        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = RoseBadge.copy(alpha = 0.15f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, RoseBadge),
                shape = RoundedCornerShape(10.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Error,
                        contentDescription = null,
                        tint = RoseBadge,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Validate Button
        Button(
            onClick = onValidateAndImport,
            enabled = jsonText.isNotBlank(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("validate_import_button")
        ) {
            Text(
                text = "Parse & Review Configuration",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ManualSetupForm(
    sourceName: String,
    onSourceNameChange: (String) -> Unit,
    apiUrl: String,
    onApiUrlChange: (String) -> Unit,
    searchParam: String,
    onSearchParamChange: (String) -> Unit,
    pageParam: String,
    onPageParamChange: (String) -> Unit,
    pageStartsAt: Int,
    onPageStartsAtChange: (Int) -> Unit,
    pageSizeParam: String,
    onPageSizeParamChange: (String) -> Unit,
    defaultPageSize: Int,
    onDefaultPageSizeChange: (Int) -> Unit,
    itemsPath: String,
    onItemsPathChange: (String) -> Unit,
    imageUrlField: String,
    onImageUrlFieldChange: (String) -> Unit,
    thumbUrlField: String,
    onThumbUrlFieldChange: (String) -> Unit,
    sampleUrlField: String,
    onSampleUrlFieldChange: (String) -> Unit,
    postUrlField: String,
    onPostUrlFieldChange: (String) -> Unit,
    tagsField: String,
    onTagsFieldChange: (String) -> Unit,
    ratingField: String,
    onRatingFieldChange: (String) -> Unit,
    mediaTypeField: String,
    onMediaTypeFieldChange: (String) -> Unit,
    titleField: String,
    onTitleFieldChange: (String) -> Unit,
    authorField: String,
    onAuthorFieldChange: (String) -> Unit,
    gifQueryTag: String,
    onGifQueryTagChange: (String) -> Unit,
    videoQueryTag: String,
    onVideoQueryTagChange: (String) -> Unit,
    safeRatingTag: String,
    onSafeRatingTagChange: (String) -> Unit,
    suggestiveRatingTag: String,
    onSuggestiveRatingTagChange: (String) -> Unit,
    adultRatingTag: String,
    onAdultRatingTagChange: (String) -> Unit,
    authType: AuthType,
    onAuthTypeChange: (AuthType) -> Unit,
    authHeaderName: String,
    onAuthHeaderNameChange: (String) -> Unit,
    authHeaderValue: String,
    onAuthHeaderValueChange: (String) -> Unit,
    authQueryParams: List<AuthParam>,
    onAuthQueryParamsChange: (List<AuthParam>) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Basic Information
        FormSectionTitle("1. Basic Information")
        FormTextField(
            label = "Source Name *",
            value = sourceName,
            onValueChange = onSourceNameChange,
            placeholder = "e.g., Wallhaven, Danbooru",
            testTag = "input_source_name"
        )
        FormTextField(
            label = "API Base / Endpoint URL *",
            value = apiUrl,
            onValueChange = onApiUrlChange,
            placeholder = "https://api.example.com/v1/posts.json",
            testTag = "input_api_url"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 2.dp))

        // 2. Request Parameters
        FormSectionTitle("2. Request & Pagination")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Search Query Param",
                    value = searchParam,
                    onValueChange = onSearchParamChange,
                    placeholder = "tags or q",
                    testTag = "input_search_param"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Page Number Param",
                    value = pageParam,
                    onValueChange = onPageParamChange,
                    placeholder = "page or pid",
                    testTag = "input_page_param"
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Page Starts At (0 or 1)",
                    value = pageStartsAt.toString(),
                    onValueChange = { onPageStartsAtChange(it.toIntOrNull() ?: 1) },
                    placeholder = "1",
                    testTag = "input_page_starts_at"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Page Size Param",
                    value = pageSizeParam,
                    onValueChange = onPageSizeParamChange,
                    placeholder = "limit or count",
                    testTag = "input_page_size_param"
                )
            }
        }

        FormTextField(
            label = "Results JSON Array Path",
            value = itemsPath,
            onValueChange = onItemsPathChange,
            placeholder = "e.g. data or post (leave blank if root is array)",
            testTag = "input_items_path"
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 2.dp))

        // 3. Response Mapping
        FormSectionTitle("3. JSON Field Mappings")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Image URL Field *",
                    value = imageUrlField,
                    onValueChange = onImageUrlFieldChange,
                    placeholder = "file_url or url",
                    testTag = "input_image_field"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Preview / Thumbnail Field",
                    value = thumbUrlField,
                    onValueChange = onThumbUrlFieldChange,
                    placeholder = "preview_url or thumb",
                    testTag = "input_thumb_field"
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Sample / Medium Field",
                    value = sampleUrlField,
                    onValueChange = onSampleUrlFieldChange,
                    placeholder = "sample_url or large_file_url",
                    testTag = "input_sample_field"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Post Webpage URL / ID",
                    value = postUrlField,
                    onValueChange = onPostUrlFieldChange,
                    placeholder = "id or link",
                    testTag = "input_post_field"
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Tags Field",
                    value = tagsField,
                    onValueChange = onTagsFieldChange,
                    placeholder = "tags or tag_string",
                    testTag = "input_tags_field"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Rating Field",
                    value = ratingField,
                    onValueChange = onRatingFieldChange,
                    placeholder = "rating or purity",
                    testTag = "input_rating_field"
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 2.dp))

        // 4. Media Discovery & Ratings
        FormSectionTitle("4. Media Discovery Tags (Optional)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "GIF Query Tag",
                    value = gifQueryTag,
                    onValueChange = onGifQueryTagChange,
                    placeholder = "e.g. animated",
                    testTag = "input_gif_query_tag"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Video Query Tag",
                    value = videoQueryTag,
                    onValueChange = onVideoQueryTagChange,
                    placeholder = "e.g. webm or video",
                    testTag = "input_video_query_tag"
                )
            }
        }

        FormSectionTitle("5. Server-Side Rating Tags (Optional)")
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Safe Tag",
                    value = safeRatingTag,
                    onValueChange = onSafeRatingTagChange,
                    placeholder = "rating:g",
                    testTag = "input_safe_rating_tag"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Suggestive Tag",
                    value = suggestiveRatingTag,
                    onValueChange = onSuggestiveRatingTagChange,
                    placeholder = "rating:s,q",
                    testTag = "input_suggestive_rating_tag"
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                FormTextField(
                    label = "Adult Tag",
                    value = adultRatingTag,
                    onValueChange = onAdultRatingTagChange,
                    placeholder = "rating:e",
                    testTag = "input_adult_rating_tag"
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(vertical = 2.dp))

        // 6. Authentication Section
        FormSectionTitle("6. Authentication")
        Text(
            text = "Select the authentication method required by this API:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
        )

        // Auth Type Selector Chips (2x2 grid so Query Parameters is centered and does not wrap awkwardly)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AuthType.values().toList().chunked(2).forEach { rowTypes ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowTypes.forEach { type ->
                        val isSelected = authType == type
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
                                .border(1.dp, if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline, RoundedCornerShape(8.dp))
                                .clickable { onAuthTypeChange(type) }
                                .padding(horizontal = 8.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = type.label,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        when (authType) {
            AuthType.NONE -> {
                Text(
                    text = "No authentication credentials will be sent with requests.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp
                )
            }

            AuthType.BEARER_TOKEN -> {
                FormTextField(
                    label = "Bearer Token *",
                    value = authHeaderValue,
                    onValueChange = onAuthHeaderValueChange,
                    placeholder = "eyJhbGciOi...",
                    testTag = "input_bearer_token"
                )
            }

            AuthType.CUSTOM_HEADER -> {
                FormTextField(
                    label = "Header Name *",
                    value = authHeaderName,
                    onValueChange = onAuthHeaderNameChange,
                    placeholder = "e.g. X-API-KEY or Client-ID",
                    testTag = "input_custom_header_name"
                )
                FormTextField(
                    label = "Header Value *",
                    value = authHeaderValue,
                    onValueChange = onAuthHeaderValueChange,
                    placeholder = "Your API Key or Token",
                    testTag = "input_custom_header_value"
                )
            }

            AuthType.QUERY_PARAMS -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Query Parameters (e.g. api_key, user_id):",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )

                    authQueryParams.forEachIndexed { index, param ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                FormTextField(
                                    label = "Param Key",
                                    value = param.key,
                                    onValueChange = { newKey ->
                                        val updated = authQueryParams.toMutableList()
                                        updated[index] = param.copy(key = newKey)
                                        onAuthQueryParamsChange(updated)
                                    },
                                    placeholder = "api_key",
                                    testTag = "input_query_param_key_$index"
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                FormTextField(
                                    label = "Param Value",
                                    value = param.value,
                                    onValueChange = { newVal ->
                                        val updated = authQueryParams.toMutableList()
                                        updated[index] = param.copy(value = newVal)
                                        onAuthQueryParamsChange(updated)
                                    },
                                    placeholder = "secret_value",
                                    testTag = "input_query_param_value_$index"
                                )
                            }
                            IconButton(
                                onClick = {
                                    val updated = authQueryParams.toMutableList()
                                    updated.removeAt(index)
                                    onAuthQueryParamsChange(updated)
                                },
                                modifier = Modifier
                                    .padding(top = 18.dp)
                                    .size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove parameter",
                                    tint = RoseBadge,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = {
                            val updated = authQueryParams.toMutableList()
                            updated.add(AuthParam("", ""))
                            onAuthQueryParamsChange(updated)
                        },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.testTag("add_auth_param_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Parameter", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun ReviewAndTestScreen(
    config: MediaSourceConfig,
    report: SourceDiagnosticReport?,
    isTesting: Boolean,
    onRunTest: () -> Unit,
    onEditDetails: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)
                    ) {
                        Text(
                            text = config.name,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = config.apiUrl,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onEditDetails,
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        modifier = Modifier.testTag("review_edit_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit details",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Edit",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (config.supportsGifs) {
                        BadgePill("GIF", MaterialTheme.colorScheme.tertiary)
                    }
                    if (config.supportsVideos) {
                        BadgePill("VIDEO", MaterialTheme.colorScheme.secondary)
                    }
                    if (config.hasAuthentication) {
                        BadgePill("AUTH", EmeraldSafe)
                    }
                    if (config.safeRatingTag.isNotBlank() || config.adultRatingTag.isNotBlank()) {
                        BadgePill("RATINGS", MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        // Diagnostics Checklist
        Text(
            text = "DIAGNOSTIC TEST REPORT",
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(top = 4.dp)
        )

        if (isTesting) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary,
                        strokeWidth = 3.dp,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Running API diagnostic tests...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        } else if (report != null) {
            val isAllPassed = report.isSuccess
            val bannerBg = if (isAllPassed) EmeraldSafe.copy(alpha = 0.15f) else RoseBadge.copy(alpha = 0.15f)
            val bannerBorder = if (isAllPassed) EmeraldSafe else RoseBadge
            val bannerIcon = if (isAllPassed) Icons.Default.CheckCircle else Icons.Default.Error
            val bannerTint = if (isAllPassed) EmeraldSafe else RoseBadge

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(bannerBg)
                    .border(1.dp, bannerBorder, RoundedCornerShape(10.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = bannerIcon,
                        contentDescription = null,
                        tint = bannerTint,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (isAllPassed) "Diagnostics Successful" else "Diagnostics Warning / Failed",
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = report.summary,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                report.steps.forEach { step ->
                    val (icon, tint) = when (step.status) {
                        DiagnosticStatus.PASSED -> Icons.Default.CheckCircle to EmeraldSafe
                        DiagnosticStatus.WARNING -> Icons.Default.Warning to Color(0xFFFBBF24)
                        DiagnosticStatus.FAILED -> Icons.Default.Error to RoseBadge
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = tint,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = step.title,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                            Text(
                                text = step.detail,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Ready to test '${config.name}'",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = onRunTest,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Run Diagnostic Test", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun BadgePill(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(color.copy(alpha = 0.2f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun FormSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.primary,
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(bottom = 3.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
            shape = RoundedCornerShape(8.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            )
        )
    }
}

