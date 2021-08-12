package jp.co.arsaga.extensions.repositoryFlow

import jp.co.arsaga.extensions.repository.common.BasePagingRepository
import jp.co.arsaga.extensions.repository.common.BaseRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*

abstract class StateFlowRepository<Res, Req>(
    private val coroutineScope: CoroutineScope,
    initRequest: Req? = null
) : BaseRepository.Impl<Res?, StateFlow<Res?>, Req>(initRequest) {

    private val _dataSource = DataSource<Res>(coroutineScope, ::onActive, ::onInactive)

    override val dataSource: StateFlow<Res?> = _dataSource.stateFlow

    override fun dataPush(response: Res?) {
        _dataSource.stateFlow.value = response
    }

    open fun onActive() {
        if (isNeedUpdate()) refresh()
    }

    open fun onInactive() {}
}

abstract class StateFlowPagingRepository<Res, Req, Content>(
    private val coroutineScope: CoroutineScope,
    initRequest: Req? = null
) : BasePagingRepository.Impl<Res?, StateFlow<Res?>, Req, Content>(initRequest) {

    private val _dataSource = DataSource<Res>(coroutineScope, ::onActive, ::onInactive)

    override val dataSource: StateFlow<Res?> = _dataSource.stateFlow

    override fun limitEntityCount(): Int = 100

    override fun dataPush(response: Res?) {
        _dataSource.stateFlow.value = combineList(currentList(), response)
    }

    open fun onActive() {
        if (isNeedUpdate()) refresh()
    }

    open fun onInactive() {}
}

private class DataSource<Res>(
    coroutineScope: CoroutineScope,
    onActive: () -> Unit,
    onInactive: () -> Unit
) {
    val stateFlow = MutableStateFlow<Res?>(null).apply {
        subscriptionCount
            .map { count -> count > 0 }
            .distinctUntilChanged()
            .onEach {
                if (it) onActive()
                else onInactive()
            }.launchIn(coroutineScope)
    }
}
