package jp.co.arsaga.extensions.repositoryFlow

import jp.co.arsaga.extensions.repository.common.BasePagingRepository
import jp.co.arsaga.extensions.repository.common.BaseRepository
import jp.co.arsaga.extensions.repository.common.UiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

abstract class StateFlowRepository<Res, Req>(
    private val coroutineScope: CoroutineScope,
    override val requestQuery: (() -> Req)? = null
) : BaseRepository.Impl<UiState<Res>, StateFlow<UiState<Res>>, Req>(requestQuery) {

    private val _dataSource = DataSource<Res>(coroutineScope, ::onActive)

    override val dataSource: StateFlow<UiState<Res>> = _dataSource.stateFlow

    override fun dataPush(response: UiState<Res>?) {
        response ?: return
        _dataSource.stateFlow.value = response
    }

    override fun refresh() {
        fetch(requestQuery?.invoke())
    }

    open fun onActive(isActive: Boolean) {
        if (isActive && isNeedUpdate()) refresh()
    }
}

abstract class StateFlowPagingRepository<Res, Req, Content>(
    private val coroutineScope: CoroutineScope,
    override val requestQuery: (() -> Req)? = null
) : BasePagingRepository.Impl<UiState<Res>, StateFlow<UiState<Res>>, Req, Content>(requestQuery) {

    private val _dataSource = DataSource<Res>(coroutineScope, ::onActive)

    override val dataSource: StateFlow<UiState<Res>> = _dataSource.stateFlow

    override fun limitEntityCount(): Int = 100

    override fun dataPush(response: UiState<Res>?) {
        response ?: return
        _dataSource.stateFlow.value = combineList(currentList(), response)
    }

    override fun refresh() {
        if (dataSource.value.data == null) {
            requestQuery?.invoke()
        } else {
            latestRequestCache
        }.run {
            dispatch(this)
        }
    }

    open fun onActive(isActive: Boolean) {
        if (isActive && isNeedUpdate()) refresh()
    }
}

private class DataSource<Res>(
    coroutineScope: CoroutineScope,
    onActive: (Boolean) -> Unit
) {
    val stateFlow = MutableStateFlow(UiState<Res>()).apply {
        subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .onEach { onActive(it) }
            .launchIn(coroutineScope)
    }
}
