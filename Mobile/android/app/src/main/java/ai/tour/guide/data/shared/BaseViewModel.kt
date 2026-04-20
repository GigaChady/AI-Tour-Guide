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

data class BaseViewModelState<T>(
    val data: T,
    val toastMessage: Any? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false
)

interface IBaseViewModel<T> {
    val stateFlow: StateFlow<BaseViewModelState<T>>
    fun clearError()
}

abstract class BaseViewModel<T>(initialData: T) : ViewModel(), IBaseViewModel<T> {
    protected val state = MutableStateFlow(BaseViewModelState(initialData))

    override val stateFlow: StateFlow<BaseViewModelState<T>> =
        state as StateFlow<BaseViewModelState<T>>

    val viewStateFlow: StateFlow<BaseViewModelState<T>> = state.asStateFlow()

    private val stateLock = Mutex()

    protected fun updateState(updater: BaseViewModelState<T>.() -> BaseViewModelState<T>) {
        viewModelScope.launch {
            stateLock.withLock {
                state.value = state.value.updater()
            }
        }
    }

    protected fun updateData(updater: T.() -> T) {
        updateState { copy(data = data.updater()) }
    }

    override fun clearError() {
        updateState { copy(toastMessage = null) }
    }

    protected suspend fun withLoading(block: suspend () -> Unit) {
        updateState { copy(isLoading = true, toastMessage = null) }
        try {
            block()
        } catch (e: Exception) {
            Log.e("BaseViewModel", e.stackTraceToString())
            updateState { copy(toastMessage = e.message) }
        } finally {
            updateState { copy(isLoading = false) }
        }
    }
}