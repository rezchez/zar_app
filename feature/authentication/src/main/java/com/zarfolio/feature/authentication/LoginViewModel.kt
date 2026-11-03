package com.zarfolio.feature.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LoginUiState(
    val busy: Boolean = false,
    val message: String? = null,
    val authenticated: Boolean = false,
    val mfaRequired: Boolean = false,
    val authenticatorAvailable: Boolean = false,
    val emailOtpAvailable: Boolean = false,
    val mfaMethod: String = "totp",
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val repository: LoginRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(LoginUiState())
    val state = _state.asStateFlow()
    private var loginId = ""
    private var password = ""
    private var mfaId: String? = null
    private var otpId: String? = null

    fun selectMfaMethod(method: String) {
        if (method != "totp" && method != "email") return
        val current = _state.value
        if ((method == "totp" && current.authenticatorAvailable) || (method == "email" && current.emailOtpAvailable)) {
            _state.value = current.copy(mfaMethod = method, message = null)
        }
    }

    fun login(loginIdInput: String, passwordInput: String, code: String = "") {
        val current = _state.value
        if (!current.mfaRequired && (loginIdInput.isBlank() || passwordInput.isBlank())) {
            _state.value = _state.value.copy(message = "ایمیل یا شماره همراه و رمز عبور را وارد کنید")
            return
        }
        val checkingMfa = current.mfaRequired
        val method = if (checkingMfa) current.mfaMethod else null
        if (checkingMfa && code.isBlank()) {
            _state.value = current.copy(message = "کد تأیید را وارد کنید")
            return
        }
        if (!checkingMfa) {
            loginId = loginIdInput.trim()
            password = passwordInput
        }
        _state.value = current.copy(busy = true, message = null)
        viewModelScope.launch {
            runCatching {
                repository.login(
                    loginId = loginId,
                    password = password,
                    mfaId = mfaId,
                    otpId = otpId,
                    code = code.trim().ifBlank { null },
                    authMethod = method,
                )
            }.onSuccess { result ->
                if (result.authenticated) {
                    loginId = ""; password = ""; mfaId = null; otpId = null
                    _state.value = LoginUiState(authenticated = true, message = result.message)
                } else {
                    mfaId = result.mfaId
                    otpId = result.otpId
                    _state.value = LoginUiState(
                        mfaRequired = result.mfaRequired,
                        authenticatorAvailable = result.authenticatorAvailable,
                        emailOtpAvailable = result.emailOtpAvailable,
                        mfaMethod = if (result.authenticatorAvailable) "totp" else "email",
                        message = result.message,
                    )
                }
            }.onFailure { error ->
                // Keep credentials only while a second-factor challenge is active.
                if (!_state.value.mfaRequired) { loginId = ""; password = "" }
                _state.value = _state.value.copy(busy = false, message = error.message ?: "ورود انجام نشد")
            }
        }
    }
}
