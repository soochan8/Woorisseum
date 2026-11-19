package com.woorisseum.feature.shopping.model

/** 연결부가 제공할 표시용 스냅샷이다. 접속·편집·최근 변경을 구분하며 잠금으로 사용하지 않는다.
 * 실제 연결과 유효기간 만료·정리는 서버 연동 단계에서 구현한다. 운영 초기값은 NotConnected다. */
sealed interface ShoppingPresenceUiState {
    data object NotConnected : ShoppingPresenceUiState
    data class Connected(
        val viewers: List<ShoppingCollaboratorUiModel> = emptyList(),
        val editing: Map<String, List<ShoppingCollaboratorUiModel>> = emptyMap(),
        val recentChanges: Map<String, ShoppingCollaboratorUiModel> = emptyMap(),
    ) : ShoppingPresenceUiState
}

data class ShoppingCollaboratorUiModel(val userId: String, val displayName: String)
