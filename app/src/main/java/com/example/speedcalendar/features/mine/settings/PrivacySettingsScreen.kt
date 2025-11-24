package com.example.speedcalendar.features.mine.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.data.model.PrivacySettingDTO
import com.example.speedcalendar.data.model.VisibilityLevel
import com.example.speedcalendar.data.model.VisibilityOption
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.AuthViewModel
import com.example.speedcalendar.viewmodel.PrivacyViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsScreen(
    onBack: () -> Unit,
    authViewModel: AuthViewModel = viewModel(),
    privacyViewModel: PrivacyViewModel = viewModel()
) {
    val userInfo by authViewModel.userInfo.collectAsState()
    val privacySettings by privacyViewModel.privacySettings.collectAsState()
    val isLoading by privacyViewModel.isLoading.collectAsState()
    val successMessage by privacyViewModel.successMessage.collectAsState()
    val errorMessage by privacyViewModel.errorMessage.collectAsState()

    var localSettings by remember { mutableStateOf<List<PrivacySettingDTO>>(emptyList()) }
    var showDialog by remember { mutableStateOf<PrivacySettingDTO?>(null) }

    val hasChanges by remember(localSettings, privacySettings) {
        derivedStateOf {
            if (localSettings.isEmpty() || privacySettings.isEmpty()) false
            else localSettings.zip(privacySettings).any { (local, original) -> local.visibilityLevel != original.visibilityLevel }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(userInfo) {
        userInfo?.userId?.let { privacyViewModel.loadPrivacySettings(it) }
    }

    LaunchedEffect(privacySettings) {
        if (privacySettings.isNotEmpty()) localSettings = privacySettings
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            privacyViewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            privacyViewModel.clearErrorMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text("隐私设置", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background)
            )
        },
        bottomBar = {
            Button(
                onClick = { userInfo?.userId?.let { privacyViewModel.updatePrivacySettings(it, localSettings) } },
                enabled = hasChanges && !isLoading,
                modifier = Modifier.fillMaxWidth().padding(16.dp).height(56.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                else Text("保存更改", fontSize = 18.sp)
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        if (isLoading && localSettings.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
        } else {
            LazyColumn(Modifier.fillMaxSize().padding(paddingValues), contentPadding = PaddingValues(vertical = 16.dp)) {
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            "管理谁可以查看你的个人信息。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
                        )
                        Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface, RoundedCornerShape(16.dp))) {
                            localSettings.forEachIndexed { index, setting ->
                                PrivacySettingItem(setting) { showDialog = setting }
                                if (index < localSettings.size - 1) HorizontalDivider(Modifier.padding(start = 16.dp), color = Background)
                            }
                        }
                    }
                }
            }
        }
    }

    showDialog?.let {
        VisibilitySelectionDialog(it, { showDialog = null }) { newLevel ->
            localSettings = localSettings.map { s -> if (s.fieldName == it.fieldName) s.copy(visibilityLevel = newLevel) else s }
            showDialog = null
        }
    }
}

@Composable
private fun PrivacySettingItem(setting: PrivacySettingDTO, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(setting.displayName, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(getVisibilityLabel(setting.visibilityLevel), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(8.dp))
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun VisibilitySelectionDialog(setting: PrivacySettingDTO, onDismiss: () -> Unit, onConfirm: (VisibilityLevel) -> Unit) {
    var selectedLevel by remember { mutableStateOf(setting.visibilityLevel) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置 ${setting.displayName} 可见性", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                VisibilityOption.options.forEach {
                    Row(Modifier.fillMaxWidth().clickable { selectedLevel = it.level }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selectedLevel == it.level, { selectedLevel = it.level }, colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text(it.label, style = MaterialTheme.typography.bodyLarge, fontWeight = if (selectedLevel == it.level) FontWeight.Bold else FontWeight.Normal)
                            Text(it.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton({ onConfirm(selectedLevel) }, colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue)) { Text("确定") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}

private fun getVisibilityLabel(level: VisibilityLevel): String = when (level) {
    VisibilityLevel.PUBLIC -> "公开"
    VisibilityLevel.FRIENDS_ONLY -> "仅好友"
    VisibilityLevel.PRIVATE -> "私密"
}
