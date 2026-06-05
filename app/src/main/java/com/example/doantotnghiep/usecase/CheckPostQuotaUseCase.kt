package com.example.doantotnghiep.usecase

import com.example.doantotnghiep.repository.RoomRepository

class CheckPostQuotaUseCase(private val repository: RoomRepository) {

    companion object {
        const val FREE_POSTS_PER_DAY = 3
    }

    operator fun invoke(
        uid: String,
        onAllowed: (remainingToday: Int, usePurchasedSlot: Boolean) -> Unit,
        onBlocked: (unlockAt: Long) -> Unit,
        onFailure: (String) -> Unit
    ) {
        repository.checkDailyPostQuota(uid, FREE_POSTS_PER_DAY, onAllowed, onBlocked, onFailure)
    }
}
