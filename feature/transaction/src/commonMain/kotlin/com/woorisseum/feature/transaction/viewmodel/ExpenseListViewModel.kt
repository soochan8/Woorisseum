package com.woorisseum.feature.transaction.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.woorisseum.core.domain.transaction.card.CardRepository
import com.woorisseum.core.domain.transaction.model.Expense
import com.woorisseum.core.domain.transaction.repository.ExpenseRepository
import com.woorisseum.core.domain.transaction.search.ExpenseSearch
import com.woorisseum.feature.transaction.model.toExpenseOperationError
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import com.woorisseum.feature.transaction.model.ExpenseSearchPeriod
import com.woorisseum.feature.transaction.model.ExpenseSearchPreset
import com.woorisseum.feature.transaction.model.toPeriod
import com.woorisseum.feature.transaction.model.ExpenseDateRange
import com.woorisseum.feature.transaction.model.ExpenseListFilters
import com.woorisseum.feature.transaction.model.ExpenseListUiEffect
import com.woorisseum.feature.transaction.model.ExpenseListUiIntent
import com.woorisseum.feature.transaction.model.ExpenseListUiState
import com.woorisseum.feature.transaction.model.toExpensePaymentFilter
import com.woorisseum.feature.transaction.model.toPaymentMethods
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import com.woorisseum.core.domain.common.AppResult
import kotlinx.datetime.YearMonth
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlinx.datetime.minusMonth
import kotlinx.datetime.number
import kotlinx.datetime.plusMonth

/** DB 원본을 관찰하고 월·기간·필터에 맞는 표시 상태를 만든다. */
class ExpenseListViewModel(
    initialMonth: YearMonth,
    private val cardRepository: CardRepository,
    private val repository: ExpenseRepository,
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) : ViewModel() {
    private var cached: Pair<ExpenseSearch, AppResult<List<Expense>>>? = null
    private fun query(state: ExpenseListUiState): ExpenseSearch = if (state.isSearchOpen) {
        ExpenseSearch(start = state.searchPeriod.range?.start, endInclusive = state.searchPeriod.range?.endInclusive)
    } else ExpenseSearch(month = state.month)

    private fun present(state: ExpenseListUiState): ExpenseListUiState {
        val result = cached?.takeIf { it.first == query(state) }?.second
        return when (result) {
            null -> state.copy(isLoading = true, loadError = null, isExpenseSourceReady = false,
                dayGroups = emptyList(), totalAmountText = "", expenseCount = 0, totalAmountOverflow = false)
            is AppResult.Success -> state.withExpenseResults(result.data).copy(isLoading = false, loadError = null)
            is AppResult.Failure -> state.copy(isLoading = false, loadError = result.error.toExpenseOperationError(),
                isExpenseSourceReady = false, dayGroups = emptyList(), totalAmountText = "", expenseCount = 0, totalAmountOverflow = false)
        }
    }
    private val _uiState = MutableStateFlow(present(emptyMonthState(initialMonth)))
    val uiState = _uiState.asStateFlow()

    // 화면이 비활성 상태일 때 발생한 이동 요청은 재생하지 않는다.
    private val _effects = MutableSharedFlow<ExpenseListUiEffect>()
    val effects = _effects.asSharedFlow()

    private var cardObservation: Job? = null

    private var expenseObservation: Job? = null

    init { observeCards(); observeExpenses() }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeExpenses() {
        expenseObservation?.cancel()
        cached = null
        _uiState.update(::present)
        expenseObservation = viewModelScope.launch {
            _uiState.map(::query).distinctUntilChanged().flatMapLatest { criteria ->
                repository.observeExpenses(criteria).map { criteria to it }
            }.collect { (criteria, result) ->
                if (criteria != query(_uiState.value)) return@collect
                cached = criteria to result
                _uiState.update(::present)
            }
        }
    }

    private fun observeCards() {
        cardObservation?.cancel()
        cardObservation = viewModelScope.launch {
            cardRepository.observeCards().collect { result ->
                if (result is AppResult.Failure) {
                    _uiState.update { it.copy(cardLoadFailed = true) }
                    return@collect
                }
                val cards = (result as AppResult.Success).data
                _uiState.update { state ->
                    val methods = cards.toPaymentMethods()
                    val selected = state.filters.paymentMethod?.takeIf { filter ->
                        methods.any { it.toExpensePaymentFilter() == filter }
                    }
                    present(state.copy(cardLoadFailed = false, availablePaymentMethods = methods, filters = state.filters.copy(paymentMethod = selected)))
                }
            }
        }
    }

    fun onIntent(intent: ExpenseListUiIntent) {
        when (intent) {
            ExpenseListUiIntent.RetryExpenses -> observeExpenses()
            ExpenseListUiIntent.RetryCards -> observeCards()
            ExpenseListUiIntent.OpenSearch -> _uiState.update { if (it.isSearchOpen) it else present(it.copy(isSearchOpen = true, searchPeriod = ExpenseSearchPeriod())) }
            ExpenseListUiIntent.CloseSearch -> _uiState.update { present(it.copy(isSearchOpen = false, searchQuery = "", searchPeriod = ExpenseSearchPeriod())) }
            ExpenseListUiIntent.ClearSearch -> _uiState.update { present(it.copy(searchQuery = "")) }
            is ExpenseListUiIntent.SearchQueryChanged -> _uiState.update { present(it.copy(searchQuery = intent.query)) }
            is ExpenseListUiIntent.SelectSearchPreset -> {
                if (intent.preset != ExpenseSearchPreset.CUSTOM) {
                    val period = intent.preset.toPeriod(today())
                    _uiState.update { if (it.isSearchOpen) present(it.copy(searchPeriod = period)) else it }
                }
            }
            is ExpenseListUiIntent.SelectSearchRange -> _uiState.update {
                if (!it.isSearchOpen || intent.start > intent.endInclusive) it else present(it.copy(
                    searchPeriod = ExpenseSearchPeriod(ExpenseSearchPreset.CUSTOM, ExpenseDateRange(intent.start, intent.endInclusive))))
            }
            ExpenseListUiIntent.PreviousMonth -> _uiState.update {
                present(emptyMonthState(it.month.minusMonth(), it))
            }
            ExpenseListUiIntent.NextMonth -> _uiState.update {
                present(emptyMonthState(it.month.plusMonth(), it))
            }
            is ExpenseListUiIntent.SelectCategory -> _uiState.update {
                present(it.copy(filters = it.filters.copy(category = intent.category)))
            }
            is ExpenseListUiIntent.SelectDateRange -> _uiState.update { state ->
                if (intent.start > intent.endInclusive || intent.start < state.month.firstDay ||
                    intent.endInclusive > state.month.lastDay
                ) state else {
                    // 월 전체를 선택하면 별도 날짜 필터가 없는 상태로 정규화한다.
                    val range = if (intent.start == state.month.firstDay && intent.endInclusive == state.month.lastDay) {
                        null
                    } else ExpenseDateRange(intent.start, intent.endInclusive)
                    present(state.copy(filters = state.filters.copy(dateRange = range)))
                }
            }
            is ExpenseListUiIntent.SelectPaymentMethod -> _uiState.update { state ->
                if (intent.paymentMethod == null || state.availablePaymentMethods.any {
                    it.toExpensePaymentFilter() == intent.paymentMethod
                }) present(state.copy(filters = state.filters.copy(paymentMethod = intent.paymentMethod)))
                else state
            }
            ExpenseListUiIntent.ResetFilters -> _uiState.update {
                present(if (it.isSearchOpen) it.copy(
                    filters = it.filters.copy(category = null, paymentMethod = null), searchPeriod = ExpenseSearchPeriod())
                    else it.copy(filters = ExpenseListFilters()))
            }
            is ExpenseListUiIntent.OpenExpense -> viewModelScope.launch {
                _effects.emit(ExpenseListUiEffect.NavigateToDetail(intent.expenseId))
            }
            ExpenseListUiIntent.AddExpense -> viewModelScope.launch {
                _effects.emit(ExpenseListUiEffect.NavigateToAddExpense)
            }
            is ExpenseListUiIntent.ShowMonth -> _uiState.update {
                present(emptyMonthState(intent.month, it).copy(filters = ExpenseListFilters(), isSearchOpen = false, searchQuery = "", searchPeriod = ExpenseSearchPeriod()))
            }
        }
    }

    private fun emptyMonthState(
        month: YearMonth,
        previousState: ExpenseListUiState? = null,
    ): ExpenseListUiState {
        val state = ExpenseListUiState(
            month = month,
            periodText = "${month.month.number}.1 – ${month.month.number}.${month.numberOfDays}",
            filters = previousState?.filters?.copy(dateRange = null) ?: ExpenseListFilters(),
        )
        return if (previousState == null) state else state.copy(
            availablePaymentMethods = previousState.availablePaymentMethods,
            cardLoadFailed = previousState.cardLoadFailed,
            isSearchOpen = previousState.isSearchOpen,
            searchQuery = previousState.searchQuery,
            searchPeriod = previousState.searchPeriod,
        )
    }
}
