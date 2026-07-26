package com.example.service

sealed interface CaptureSurfaceResizeResult {
    data object Success : CaptureSurfaceResizeResult
    data object RolledBack : CaptureSurfaceResizeResult
    data class InfrastructureBroken(val safeReason: String) : CaptureSurfaceResizeResult
}

interface SurfaceResizeOps<R> {
    fun createNewReader(): R
    fun resizeToNew()
    fun attachNew(reader: R)
    fun resizeToOld()
    fun attachOld(reader: R?)
    fun closeReader(reader: R?)
}

/**
 * Production coordinator for surface resizing with transactional rollback.
 * Generic over reader handle type R for 100% JVM unit testability.
 */
class CaptureSurfaceResizeCoordinator<R>(
    private val diagnostic: (String) -> Unit = {}
) {

    private fun report(message: String) {
        try {
            diagnostic(message)
        } catch (_: Throwable) {
            // Resize diagnostics must never affect transaction semantics.
        }
    }

    fun resize(
        hasDisplay: Boolean,
        oldReader: R?,
        ops: SurfaceResizeOps<R>
    ): Pair<CaptureSurfaceResizeResult, R?> {
        if (!hasDisplay) {
            return Pair(CaptureSurfaceResizeResult.InfrastructureBroken("VirtualDisplay is null"), null)
        }

        var newReader: R? = null
        try {
            val created = ops.createNewReader()
            newReader = created
            ops.resizeToNew()
            ops.attachNew(created)

            if (oldReader != null) {
                ops.closeReader(oldReader)
            }

            return Pair(CaptureSurfaceResizeResult.Success, created)
        } catch (e: Exception) {
            report("Failed to resize display (${e::class.java.simpleName}); attempting rollback")
            if (newReader != null) {
                try { ops.closeReader(newReader) } catch (ex: Exception) {}
            }

            try {
                ops.resizeToOld()
                if (oldReader != null) {
                    ops.attachOld(oldReader)
                }
                return Pair(CaptureSurfaceResizeResult.RolledBack, oldReader)
            } catch (rollbackException: Exception) {
                report("Capture surface rollback failed (${rollbackException::class.java.simpleName})")
                if (oldReader != null) {
                    try { ops.closeReader(oldReader) } catch (ex: Exception) {}
                }
                return Pair(
                    CaptureSurfaceResizeResult.InfrastructureBroken("Capture surface recovery failed"),
                    null
                )
            }
        }
    }
}
