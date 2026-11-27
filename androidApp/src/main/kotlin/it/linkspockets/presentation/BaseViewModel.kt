package it.linkspockets.presentation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

abstract class BaseEffectViewModel<INTENT, STATE, EFFECT>(
    state: STATE
) : ViewModel() {
    private val _state = MutableStateFlow(state)

    val state: StateFlow<STATE> = _state.asStateFlow()

    val exceptionHandlerException = CoroutineExceptionHandler { _, exception ->
        Log.e("COROUTINE ERROR", "Si è verificato un errore", exception)
    }

    private val _effect = Channel<EFFECT>(Channel.BUFFERED)

    /** Effetti laterali one-shot. Osservabili nei Composable tramite [handleEffect]. */
    val effect = _effect.receiveAsFlow()

    @Composable
    fun getState() = state.collectAsStateWithLifecycle().value

    fun setState(state: STATE) {
        updateState { state }
    }

    protected fun updateState(transform: (STATE) -> STATE) {
        _state.update(transform)
    }

    protected suspend fun emitEffect(effect: EFFECT) {
        _effect.send(effect)
    }

    @Composable
    fun handleEffect(onEffect: (EFFECT) -> Unit) {
        LaunchedEffect(Unit) {
            effect.collect { effect ->
                onEffect(effect)
            }
        }
    }

    fun dispatch(intent: INTENT, dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate) =
        viewModelScope.launch(dispatcher + exceptionHandlerException) { onAction(intent) }

    protected abstract suspend fun onAction(intent: INTENT)

    override fun onCleared() {
        _effect.close()
        super.onCleared()
    }

}

sealed class BaseEffect

abstract class BaseViewModel<INTENT, STATE>(
    state: STATE
) : BaseEffectViewModel<INTENT, STATE, BaseEffect>(state)