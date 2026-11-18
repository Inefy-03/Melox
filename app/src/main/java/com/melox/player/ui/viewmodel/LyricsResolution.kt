package com.melox.player.ui.viewmodel

import com.melox.player.data.repository.LyricsReadException
import com.melox.player.data.repository.LyricsRequest
import com.melox.player.model.LyricsDocument
import com.melox.player.model.LyricsUiState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest

internal fun LyricsRequest.hasSameLyricsContentTarget(other: LyricsRequest): Boolean =
    mediaId == other.mediaId && contentUri == other.contentUri

internal fun shouldShowLyricsLoading(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
): Boolean = previousRequest?.hasSameLyricsContentTarget(request) != true

internal fun shouldPublishLyricsResolution(
    previousRequest: LyricsRequest?,
    request: LyricsRequest,
    previousDocument: LyricsDocument?,
    document: LyricsDocument?,
): Boolean = shouldShowLyricsLoading(previousRequest, request) || previousDocument != document

internal fun Flow<LyricsRequest?>.resolveLyricsStates(
    loadDocument: suspend (LyricsRequest) -> LyricsDocument?,
): Flow<LyricsUiState> = channelFlow {
    var previousRequest: LyricsRequest? = null
    var previousDocument: LyricsDocument? = null
    collectLatest { request ->
        if (request == null) {
            send(LyricsUiState.Unavailable)
            previousRequest = null
            previousDocument = null
            return@collectLatest
        }

        val showLoading = shouldShowLyricsLoading(previousRequest, request)
        if (showLoading) send(LyricsUiState.Loading)

        val document = try {
            loadDocument(request)
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (exception: Exception) {
            val previousSourceConfirmedAbsent = exception is LyricsReadException &&
                previousDocument?.source?.let { it !in exception.failedSources } == true
            if (showLoading || previousSourceConfirmedAbsent) {
                send(LyricsUiState.Unavailable)
                previousDocument = null
            }
            previousRequest = request
            return@collectLatest
        }
        currentCoroutineContext().ensureActive()
        if (
            shouldPublishLyricsResolution(
                previousRequest = previousRequest,
                request = request,
                previousDocument = previousDocument,
                document = document,
            )
        ) {
            send(document?.let { LyricsUiState.Available(it, request.mediaId) }
                ?: LyricsUiState.Unavailable)
        }
        previousRequest = request
        previousDocument = document
    }
}
