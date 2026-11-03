package com.zarfolio.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zarfolio.core.common.ApiResult
import com.zarfolio.core.model.ServerConfiguration
import com.zarfolio.core.network.ServerConnectionRepository
import com.zarfolio.core.network.ServerUrlValidator
import com.zarfolio.core.security.TokenStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ServerConfigurationUiState(
    val servers: List<ServerConfiguration> = emptyList(),
    val loading: Boolean = true,
    val checking: Boolean = false,
    val resultMessage: String? = null,
    val connectionPassed: Boolean? = null,
    val authenticated: Boolean = false,
    val userName: String? = null,
)

@HiltViewModel
class ServerConfigurationViewModel @Inject constructor(
    private val repository: ServerConnectionRepository,
    private val tokenStore: TokenStore,
) : ViewModel() {
    private val _state = MutableStateFlow(ServerConfigurationUiState())
    val state: StateFlow<ServerConfigurationUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeServers().collect { servers ->
                val active = servers.firstOrNull { it.isActive }
                val authenticated = active?.let { tokenStore.read(it.id)?.isNotBlank() == true } ?: false
                val userName = active?.let { tokenStore.readUserName(it.id) }
                _state.value = _state.value.copy(servers = servers, loading = false, authenticated = authenticated, userName = userName)
            }
        }
    }

    fun test(name: String, rawUrl: String) {
        val url = ServerUrlValidator.normalize(rawUrl)
        if (url == null) { _state.value = _state.value.copy(resultMessage = "نشانی سرور معتبر نیست", connectionPassed = false); return }
        viewModelScope.launch {
            _state.value = _state.value.copy(checking = true, resultMessage = null, connectionPassed = null)
            when (val result = repository.test(ServerConfiguration("connection-check", name.ifBlank { "سرور جدید" }, url))) {
                is ApiResult.Success -> _state.value = _state.value.copy(checking = false, resultMessage = "سرور پاسخ شبکه‌ای داد (${result.value} ms)", connectionPassed = true)
                is ApiResult.Failure -> _state.value = _state.value.copy(checking = false, resultMessage = result.message ?: "اتصال برقرار نشد", connectionPassed = false)
            }
        }
    }

    fun save(name: String, rawUrl: String) {
        val url = ServerUrlValidator.normalize(rawUrl)
        if (url == null) { _state.value = _state.value.copy(resultMessage = "نشانی سرور معتبر نیست", connectionPassed = false); return }
        viewModelScope.launch {
            val current = _state.value.servers.firstOrNull { it.isActive }
            val item = ServerConfiguration(current?.id ?: UUID.randomUUID().toString(), name.trim().ifBlank { "سرور زر" }, url, true)
            runCatching { repository.save(item) }
                .onSuccess { _state.value = _state.value.copy(resultMessage = "تنظیمات سرور ذخیره شد", connectionPassed = true) }
                .onFailure { _state.value = _state.value.copy(resultMessage = "ذخیرهٔ تنظیمات انجام نشد", connectionPassed = false) }
        }
    }

    /** Saves one of the two login connection profiles and makes it the active API target. */
    fun saveLoginConnection(isLocal: Boolean, rawUrl: String) {
        val url = ServerUrlValidator.normalize(rawUrl)
        if (url == null) {
            _state.value = _state.value.copy(resultMessage = "نشانی سرور معتبر نیست", connectionPassed = false)
            return
        }
        val id = if (isLocal) LOCAL_CONNECTION_ID else SERVER_CONNECTION_ID
        val label = if (isLocal) "اتصال محلی" else "سرور" 
        viewModelScope.launch {
            runCatching {
                repository.save(ServerConfiguration(id, label, url))
                repository.select(id)
            }.onSuccess {
                _state.value = _state.value.copy(resultMessage = "$label ذخیره و انتخاب شد", connectionPassed = true)
            }.onFailure {
                _state.value = _state.value.copy(resultMessage = "ذخیرهٔ اتصال انجام نشد", connectionPassed = false)
            }
        }
    }

    fun select(id: String) { viewModelScope.launch { runCatching { repository.select(id) } } }
    fun refreshSession() {
        viewModelScope.launch {
            val active = repository.active()
            val authenticated = active?.let { tokenStore.read(it.id)?.isNotBlank() == true } ?: false
            _state.value = _state.value.copy(authenticated = authenticated, userName = if (authenticated) active?.let { tokenStore.readUserName(it.id) } else null)
        }
    }
    fun clearMessage() { _state.value = _state.value.copy(resultMessage = null, connectionPassed = null) }

    companion object {
        const val LOCAL_CONNECTION_ID = "login-connection-local"
        const val SERVER_CONNECTION_ID = "login-connection-server"
    }
}
