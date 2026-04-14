package ai.tour.guide.data.shared

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

interface IBaseViewModelState {
    val errorMessage: Any?
    val isLoading: Boolean
    val isSuccess: Boolean
}

data class BaseViewState<T>(
    val data: T,
    override val errorMessage: Any? = null,
    override val isLoading: Boolean = false,
    override val isSuccess: Boolean = false
) : IBaseViewModelState

interface IBaseViewModel {
    val stateFlow: StateFlow<IBaseViewModelState>
    fun clearError()
}

abstract class BaseViewModel<T>(initialData: T) : ViewModel(), IBaseViewModel {
    protected val _state = MutableStateFlow(BaseViewState(initialData))

    @Suppress("UNCHECKED_CAST")
    override val stateFlow: StateFlow<IBaseViewModelState> =
        _state as StateFlow<IBaseViewModelState>

    val viewStateFlow: StateFlow<BaseViewState<T>> = _state.asStateFlow()

    private val stateLock = Mutex()

    protected fun updateState(updater: BaseViewState<T>.() -> BaseViewState<T>) {
        viewModelScope.launch {
            stateLock.withLock {
                _state.value = _state.value.updater()
            }
        }
    }

    protected fun updateData(updater: T.() -> T) {
        updateState { copy(data = data.updater()) }
    }

    override fun clearError() {
        updateState { copy(errorMessage = null) }
    }

    protected suspend fun withLoading(block: suspend () -> Unit) {
        updateState { copy(isLoading = true, errorMessage = null) }
        try {
            block()
        } catch (e: Exception) {
            Log.e("BaseViewModel", e.stackTraceToString())
            updateState { copy(errorMessage = e.message) }
        } finally {
            updateState { copy(isLoading = false) }
        }
    }
}