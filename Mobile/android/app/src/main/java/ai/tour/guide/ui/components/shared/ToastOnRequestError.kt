package ai.tour.guide.ui.components.shared

import ai.tour.guide.data.shared.IBaseViewModel
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun ToastOnRequestError(viewModel: IBaseViewModel<*>) {
    val context = LocalContext.current
    val viewModelState by viewModel.stateFlow.collectAsStateWithLifecycle()
    val toastMessage = viewModelState.toastMessage?.let {
        when (it) {
            is Int -> stringResource(it)
            is String -> it
            else -> null
        }
    }

    LifecycleStartEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
        onStopOrDispose { }
    }
}
