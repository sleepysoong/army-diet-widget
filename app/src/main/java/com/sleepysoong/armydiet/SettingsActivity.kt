package com.sleepysoong.armydiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.clickable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sleepysoong.armydiet.data.local.AppPreferences
import com.sleepysoong.armydiet.di.AppContainer
import com.sleepysoong.armydiet.ui.theme.AppTheme
import com.sleepysoong.armydiet.ui.theme.ArmyColors
import kotlinx.coroutines.launch

class SettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val container = AppContainer.getInstance(this)
        
        setContent {
            AppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ArmyColors.Background
                ) {
                    SettingsScreen(
                        dependencies = container,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    dependencies: SettingsDependencies,
    onBack: () -> Unit
) {
    val keywords by dependencies.settings.highlightKeywords.collectAsStateWithLifecycle(initialValue = emptySet())
    val mealSource by dependencies.settings.mealSource.collectAsStateWithLifecycle(initialValue = AppPreferences.SOURCE_LOCAL)
    val endpoint by dependencies.settings.externalApiEndpoint.collectAsStateWithLifecycle(initialValue = "")
    val mndUnitCode by dependencies.settings.mndUnitCode.collectAsStateWithLifecycle(
        initialValue = AppPreferences.DEFAULT_MND_UNIT_CODE
    )
    val scope = rememberCoroutineScope()
    var endpointInput by remember { mutableStateOf("") }
    var mndUnitCodeInput by remember { mutableStateOf(AppPreferences.DEFAULT_MND_UNIT_CODE) }

    LaunchedEffect(endpoint) {
        endpointInput = endpoint ?: ""
    }

    LaunchedEffect(mndUnitCode) {
        mndUnitCodeInput = mndUnitCode
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "식단 설정", 
                        fontWeight = FontWeight.Bold,
                        color = ArmyColors.Primary
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "뒤로 가기",
                            tint = ArmyColors.OnSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ArmyColors.Surface
                )
            )
        },
        containerColor = ArmyColors.Background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "식단 데이터 소스",
                style = MaterialTheme.typography.titleMedium,
                color = ArmyColors.Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "식단 데이터를 로컬 저장소에서 읽을지, 외부 API에서 가져올지 선택합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            SourceOptionRow(
                title = "로컬",
                description = "기존 방식대로 로컬 DB에서 식단을 불러옵니다.",
                selected = mealSource == AppPreferences.SOURCE_LOCAL,
                onSelect = {
                    scope.launch {
                        dependencies.settings.setMealSource(AppPreferences.SOURCE_LOCAL)
                        dependencies.widgetUpdateDispatcher.updateAll()
                    }
                }
            )

            if (mealSource == AppPreferences.SOURCE_LOCAL) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = mndUnitCodeInput,
                    onValueChange = { mndUnitCodeInput = it },
                    label = { Text("국방부 부대 코드") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_local_unit_code"),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = {
                        Text("DS_TB_MNDT_DATEBYMLSVC_ 뒤에 붙는 번호를 입력하세요. 예: 7369")
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ArmyColors.Primary,
                        focusedLabelColor = ArmyColors.Primary
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            val normalizedCode = mndUnitCodeInput.trim()
                                .ifBlank { AppPreferences.DEFAULT_MND_UNIT_CODE }
                            val changed = normalizedCode != mndUnitCode

                            dependencies.settings.setMndUnitCode(normalizedCode)

                            if (changed) {
                                dependencies.settings.updateSyncStatus(0, 0)
                                dependencies.mealDao.clearMeals()
                            }

                            dependencies.widgetUpdateDispatcher.updateAll()
                        }
                    },
                    enabled = mndUnitCodeInput.trim().ifBlank { AppPreferences.DEFAULT_MND_UNIT_CODE } != mndUnitCode,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_save_unit_code"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("부대 코드 저장")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            SourceOptionRow(
                title = "외부 API",
                description = "설정한 API 서버에서 오늘 식단만 가져옵니다.",
                selected = mealSource == AppPreferences.SOURCE_EXTERNAL,
                onSelect = {
                    scope.launch {
                        dependencies.settings.setMealSource(AppPreferences.SOURCE_EXTERNAL)
                        dependencies.widgetUpdateDispatcher.updateAll()
                    }
                }
            )

            if (mealSource == AppPreferences.SOURCE_EXTERNAL) {
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = { endpointInput = it },
                    label = { Text("API Endpoint") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_external_endpoint"),
                    shape = MaterialTheme.shapes.medium,
                    supportingText = {
                        Text("예: https://example.com")
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        scope.launch {
                            dependencies.settings.setExternalApiEndpoint(endpointInput.trim())
                            dependencies.widgetUpdateDispatcher.updateAll()
                        }
                    },
                    enabled = endpointInput.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("settings_save_endpoint"),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("엔드포인트 저장")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "강조할 키워드 관리",
                style = MaterialTheme.typography.titleMedium,
                color = ArmyColors.Primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "등록된 키워드가 메뉴에 포함되면\n앱과 위젯에서 진한 초록색으로 강조됩니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(32.dp))
            
            KeywordInput(
                onAdd = { keyword ->
                    scope.launch { 
                        dependencies.settings.addKeyword(keyword) 
                        dependencies.widgetUpdateDispatcher.updateAll()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            KeywordList(
                keywords = keywords,
                onRemove = { keyword ->
                    scope.launch { 
                        dependencies.settings.removeKeyword(keyword) 
                        dependencies.widgetUpdateDispatcher.updateAll()
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            ResetApiKeySection(
                onResetApiKey = {
                    scope.launch {
                        dependencies.settings.clearApiKey()
                        dependencies.widgetUpdateDispatcher.updateAll()
                        onBack()
                    }
                },
                onResetEndpoint = {
                    scope.launch {
                        dependencies.settings.clearExternalApiEndpoint()
                        dependencies.widgetUpdateDispatcher.updateAll()
                        onBack()
                    }
                }
            )
        }
    }
}

@Composable
fun ResetApiKeySection(
    onResetApiKey: () -> Unit,
    onResetEndpoint: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ArmyColors.Error.copy(alpha = 0.1f)
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, ArmyColors.Error.copy(alpha = 0.5f)),
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = ArmyColors.Error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "데이터 초기화",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ArmyColors.Error
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "저장된 API Key/외부 엔드포인트를 삭제하고 초기 화면으로 돌아갑니다. 이 작업은 되돌릴 수 없습니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = ArmyColors.Error.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onResetApiKey,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArmyColors.Error,
                    contentColor = androidx.compose.ui.graphics.Color.White
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("API Key 초기화", fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onResetEndpoint,
                modifier = Modifier.fillMaxWidth(),
                border = androidx.compose.foundation.BorderStroke(1.dp, ArmyColors.Error),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = ArmyColors.Error
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Text("외부 API 초기화", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun SourceOptionRow(
    title: String,
    description: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("source_option_$title")
            .clickable(onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) ArmyColors.PrimaryContainer else ArmyColors.Surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = selected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = ArmyColors.Primary
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = ArmyColors.Primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun KeywordInput(onAdd: (String) -> Unit) {
    var newKeyword by remember { mutableStateOf("") }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = newKeyword,
            onValueChange = { newKeyword = it },
            label = { Text("키워드 추가 (예: 치킨)") },
            singleLine = true,
            modifier = Modifier.weight(1f),
            shape = MaterialTheme.shapes.medium,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ArmyColors.Primary,
                focusedLabelColor = ArmyColors.Primary
            )
        )
        Spacer(modifier = Modifier.width(12.dp))
        Button(
            onClick = {
                if (newKeyword.isNotBlank()) {
                    onAdd(newKeyword)
                    newKeyword = ""
                }
            },
            enabled = newKeyword.isNotBlank(),
            shape = MaterialTheme.shapes.medium,
            colors = ButtonDefaults.buttonColors(
                containerColor = ArmyColors.Primary,
                disabledContainerColor = ArmyColors.Primary.copy(alpha = 0.5f)
            ),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "추가")
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun KeywordList(
    keywords: Set<String>,
    onRemove: (String) -> Unit
) {
    Column {
        Text(
            text = "등록된 키워드 (${keywords.size})",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = ArmyColors.OnSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keywords.sorted().forEach { keyword ->
                InputChip(
                    selected = false,
                    onClick = { onRemove(keyword) },
                    label = { 
                        Text(
                            keyword, 
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        ) 
                    },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "삭제",
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = InputChipDefaults.inputChipColors(
                        containerColor = ArmyColors.Surface,
                        labelColor = ArmyColors.OnSurface,
                        trailingIconColor = ArmyColors.OnSurfaceVariant
                    ),
                    border = InputChipDefaults.inputChipBorder(
                        borderColor = ArmyColors.OnSurfaceVariant.copy(alpha = 0.2f)
                    ),
                    shape = MaterialTheme.shapes.large
                )
            }
        }
    }
}
