package ai.tour.guide.ui.components.shared

import ai.tour.guide.data.state.IBaseViewModel
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource

@Composable
fun ToastOnRequestError(viewModel: IBaseViewModel) {
    val context = LocalContext.current
    val viewModelState by viewModel.stateFlow.collectAsState()
    val errorMessage = viewModelState.errorMessage?.let {
        when (it) {
            is Int -> stringResource(it)
            is String -> it
            else -> null
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.clearError()
        }
    }
}