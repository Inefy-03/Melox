package com.melox.player.model

/** Observable lifecycle of a local MediaStore scan. */
sealed interface ScanStatus {
    data object Idle : ScanStatus

    /** No query should run until the UI completes the runtime-permission flow. */
    data object PermissionRequired : ScanStatus

    data object Scanning : ScanStatus

    data class Success(val count: Int) : ScanStatus

    data class Error(val message: String) : ScanStatus
}
