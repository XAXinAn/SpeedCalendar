package com.example.speedcalendar.features.mine

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.speedcalendar.ui.theme.Background
import com.example.speedcalendar.ui.theme.PrimaryBlue
import com.example.speedcalendar.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginSheet(
    onClose: () -> Unit,
    viewModel: AuthViewModel = viewModel()
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    // var verificationCode by remember { mutableStateOf("") } // TODO: 验证码登录
    var agreementChecked by remember { mutableStateOf(false) }

    val isLoading by viewModel.isLoading.collectAsState()
    val isLoggedIn by viewModel.isLoggedIn.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val successMessage by viewModel.successMessage.collectAsState()
    // val countdown by viewModel.countdown.collectAsState() // TODO: 验证码登录

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(isLoggedIn) {
        if (isLoggedIn) {
            Toast.makeText(context, "登录成功", Toast.LENGTH_SHORT).show()
            onClose()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    LaunchedEffect(successMessage) {
        successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        containerColor = Background,
        topBar = {
            TopAppBar(
                title = { Text(if (isRegisterMode) "注册账号" else "账号登录", fontWeight = FontWeight.Bold) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回") } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Background),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().padding(16.dp)) {
                Button(
                    onClick = {
                        if (isRegisterMode) {
                            if (password != confirmPassword) {
                                Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            viewModel.register(phoneNumber, password)
                        } else {
                            viewModel.login(phoneNumber, password)
                        }
                    },
                    enabled = agreementChecked && phoneNumber.isNotBlank() && password.isNotBlank() && (!isRegisterMode || confirmPassword.isNotBlank()) && !isLoading,
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    else Text(if (isRegisterMode) "注册" else "登录", fontSize = 18.sp)
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                TextButton(
                    onClick = { isRegisterMode = !isRegisterMode },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (isRegisterMode) "已有账号？去登录" else "没有账号？去注册")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp)) {
            Spacer(Modifier.height(24.dp))
            Text(if (isRegisterMode) "创建新账号" else "欢迎回来！", fontSize = 28.sp, fontWeight = FontWeight.Bold)
            Text(if (isRegisterMode) "请填写以下信息完成注册" else "使用手机号和密码登录", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(40.dp))

            LoginInputItem("手机号", phoneNumber, { phoneNumber = it }, KeyboardOptions(keyboardType = KeyboardType.Phone))
            Spacer(Modifier.height(16.dp))
            
            LoginInputItem("密码", password, { password = it }, KeyboardOptions(keyboardType = KeyboardType.Password), isPassword = true)
            Spacer(Modifier.height(16.dp))
            
            if (isRegisterMode) {
                LoginInputItem("确认密码", confirmPassword, { confirmPassword = it }, KeyboardOptions(keyboardType = KeyboardType.Password), isPassword = true)
                Spacer(Modifier.height(24.dp))
            }

            // VerificationCodeInputItem(verificationCode, { verificationCode = it }, countdown, { viewModel.sendVerificationCode(phoneNumber) }) // TODO: 验证码登录
            // Spacer(Modifier.height(24.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(agreementChecked, { agreementChecked = it }, colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue))
                Text("我已阅读并同意《服务协议》和《隐私政策》", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun LoginInputItem(
    label: String, 
    value: String, 
    onValueChange: (String) -> Unit, 
    keyboardOptions: KeyboardOptions,
    isPassword: Boolean = false
) {
    Column {
        Text(label, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        BasicTextField(value, onValueChange,
            modifier = Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)).padding(horizontal = 16.dp, vertical = 16.dp),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
            keyboardOptions = keyboardOptions, 
            singleLine = true,
            visualTransformation = if (isPassword) androidx.compose.ui.text.input.PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None
        )
    }
}

@Composable
private fun VerificationCodeInputItem(value: String, onValueChange: (String) -> Unit, countdown: Int, onSendCode: () -> Unit) {
    Column {
        Text("验证码", fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 8.dp))
        Row(Modifier.fillMaxWidth().height(56.dp).background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp)), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value, onValueChange,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp, vertical = 16.dp),
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onBackground),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true
            )
            HorizontalDivider(Modifier.width(1.dp).height(24.dp).background(Background))
            TextButton(
                onClick = onSendCode, 
                enabled = countdown == 0,
                colors = ButtonDefaults.textButtonColors(contentColor = PrimaryBlue, disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant)
            ) {
                Text(if (countdown > 0) "${countdown}s 后重试" else "获取验证码", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginSheetPreview() {
    LoginSheet(onClose = {})
}
