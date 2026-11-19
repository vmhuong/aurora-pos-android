package com.example.qrorderkitchen.ui.viewmodel

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.qrorderkitchen.data.local.UserPreferencesRepository
import com.example.qrorderkitchen.data.models.StaffModel
import com.example.qrorderkitchen.data.repository.AuroraRepository
import com.example.qrorderkitchen.data.repository.AuthRepository
import com.example.qrorderkitchen.data.repository.AuthResult
import com.example.qrorderkitchen.service.SessionCleanupService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Loading : AuthState()
    object Unauthenticated : AuthState()
    data class Authenticated(val staff: StaffModel) : AuthState()
}

class AuthViewModel(private val context: Context) : ViewModel() {
    private val authRepo = AuthRepository()
    private val prefsRepo = UserPreferencesRepository(context)

    private val _authState = MutableStateFlow<AuthState>(AuthState.Loading)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _toastMessage = MutableSharedFlow<String>()
    val toastMessage: SharedFlow<String> = _toastMessage.asSharedFlow()

    val showLogoutDialog = MutableStateFlow(false)
    val showExitDialog = MutableStateFlow(false)

    init {
        // Mỗi lần mở app mới đều bắt buộc đăng nhập lại từ màn hình LoginScreen.
        // Tự động thu dọn và giải phóng toàn bộ bàn nếu có phiên cũ chưa kịp dọn dẹp.
        val orphanUsername = SessionCleanupService.getActiveStaffUsername(context)
        if (!orphanUsername.isNullOrBlank()) {
            viewModelScope.launch(Dispatchers.IO) {
                AuroraRepository().releaseAllTablesForStaff(orphanUsername)
            }
            SessionCleanupService.clearSession(context)
        }

        viewModelScope.launch {
            prefsRepo.clearSession()
            _authState.value = AuthState.Unauthenticated
        }
    }

    fun login(usernameInput: String, pinInput: String) {
        viewModelScope.launch {
            _loginError.value = null
            _authState.value = AuthState.Loading

            when (val result = authRepo.authenticateStaff(usernameInput, pinInput)) {
                is AuthResult.Success -> {
                    SessionCleanupService.start(context, result.staff.username)
                    prefsRepo.saveStaffSession(result.staff)
                    _authState.value = AuthState.Authenticated(result.staff)
                    _toastMessage.emit("Xin chào ${result.staff.name} (${result.staff.staffRole.displayName})")
                }
                is AuthResult.Error -> {
                    _loginError.value = result.message
                    _authState.value = AuthState.Unauthenticated
                }
            }
        }
    }

    fun logout(auroraViewModel: AuroraViewModel? = null) {
        val currentStaff = (_authState.value as? AuthState.Authenticated)?.staff
        viewModelScope.launch {
            if (currentStaff != null) {
                AuroraRepository().releaseAllTablesForStaff(currentStaff.username)
            }
            SessionCleanupService.stop(context)
            auroraViewModel?.clearSessionState()
            prefsRepo.clearSession()
            _authState.value = AuthState.Unauthenticated
            showLogoutDialog.value = false
            _toastMessage.emit("Đã đăng xuất ca làm việc và giải phóng bàn")
        }
    }

    fun logoutAndExit(activity: Activity?, auroraViewModel: AuroraViewModel? = null) {
        val currentStaff = (_authState.value as? AuthState.Authenticated)?.staff
        viewModelScope.launch {
            if (currentStaff != null) {
                AuroraRepository().releaseAllTablesForStaff(currentStaff.username)
            }
            SessionCleanupService.stop(context)
            auroraViewModel?.clearSessionState()
            prefsRepo.clearSession()
            _authState.value = AuthState.Unauthenticated
            showExitDialog.value = false
            activity?.finishAffinity()
        }
    }

    fun performExitCleanup() {
        val currentStaff = (_authState.value as? AuthState.Authenticated)?.staff
        val username = currentStaff?.username ?: SessionCleanupService.getActiveStaffUsername(context)
        if (!username.isNullOrBlank()) {
            val thread = Thread {
                try {
                    AuroraRepository().releaseAllTablesForStaffBlocking(username, 2)
                } catch (_: Exception) {}
            }
            thread.start()
            try {
                thread.join(2000)
            } catch (_: InterruptedException) {}
        }
        SessionCleanupService.clearSession(context)
        viewModelScope.launch {
            prefsRepo.clearSession()
        }
    }
}
