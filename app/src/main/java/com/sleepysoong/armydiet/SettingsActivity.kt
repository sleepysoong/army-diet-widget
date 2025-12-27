package com.sleepysoong.armydiet

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
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
                        container = container,
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
    container: AppContainer,
    onBack: () -> Unit
) {
    val keywords by container.preferences.highlightKeywords.collectAsStateWithLifecycle(initialValue = emptySet())
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "맛있는 음식 설정", 
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
        ) {
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
                    scope.launch { container.preferences.addKeyword(keyword) }
                }
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            KeywordList(
                keywords = keywords,
                onRemove = { keyword ->
                    scope.launch { container.preferences.removeKeyword(keyword) }
                }
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            ResetApiKeySection(
                onReset = {
                    scope.launch {
                        container.preferences.saveApiKey("")
                        // Optionally finish activity or show toast, but observing API key in Main will trigger UI change
                        onBack() // Close settings to go back to main (which will show API input screen)
                    }
                }
            )
        }
    }
}

@Composable
fun ResetApiKeySection(onReset: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = ArmyColors.Error.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, ArmyColors.Error.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "API Key 초기화",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = ArmyColors.Error
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "저장된 API Key를 삭제하고 다시 입력합니다.",
                style = MaterialTheme.typography.bodySmall,
                color = ArmyColors.Error.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onReset,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ArmyColors.Error
                )
            ) {
                Text("초기화", color = androidx.compose.ui.graphics.Color.White)
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