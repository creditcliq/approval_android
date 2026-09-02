package com.creditchek.approval_android.core.network

import kotlin.math.roundToInt

enum class NetworkQualityStatus {
    EXCELLENT, MODERATE, UNSTABLE, OFFLINE
}

class NetworkQualityEstimator(private val windowSize: Int = 5) {
    private val latencies = mutableListOf<Long>()

    var quality: NetworkQualityStatus = NetworkQualityStatus.MODERATE
        private set

    private var hasSuccessfulSample = false
    private var consecutiveFailures = 0

    val effectiveLatencyMilliseconds: Long?
        get() {
            if (latencies.isEmpty()) return null

            val sorted = latencies.sorted()
            val middleIndex = sorted.size / 2
            return if (sorted.size % 2 == 1) {
                sorted[middleIndex]
            } else {
                ((sorted[middleIndex - 1] + sorted[middleIndex]) / 2.0).roundToInt().toLong()
            }
        }

    fun recordLatency(latencyMs: Long): NetworkQualityStatus {
        consecutiveFailures = 0
        latencies.add(latencyMs)
        if (latencies.size > windowSize) {
            latencies.removeAt(0)
        }
        val effectiveLatency = effectiveLatencyMilliseconds ?: return quality
        if (!hasSuccessfulSample || quality == NetworkQualityStatus.OFFLINE) {
            quality = classifyBaseline(effectiveLatency)
            hasSuccessfulSample = true
            return quality
        }
        quality = when (quality) {
            NetworkQualityStatus.EXCELLENT -> when {
                effectiveLatency >= 1400 -> NetworkQualityStatus.UNSTABLE
                effectiveLatency >= 650 -> NetworkQualityStatus.MODERATE
                else -> NetworkQualityStatus.EXCELLENT
            }
            NetworkQualityStatus.MODERATE -> when {
                effectiveLatency <= 450 -> NetworkQualityStatus.EXCELLENT
                effectiveLatency >= 1400 -> NetworkQualityStatus.UNSTABLE
                else -> NetworkQualityStatus.MODERATE
            }
            NetworkQualityStatus.UNSTABLE -> when {
                effectiveLatency <= 450 -> NetworkQualityStatus.EXCELLENT
                effectiveLatency <= 1000 -> NetworkQualityStatus.MODERATE
                else -> NetworkQualityStatus.UNSTABLE
            }
            NetworkQualityStatus.OFFLINE -> classifyBaseline(effectiveLatency)
        }
        return quality
    }
    fun recordFailure(): NetworkQualityStatus {
        consecutiveFailures++
        quality = if (consecutiveFailures >= 2) {
            NetworkQualityStatus.OFFLINE
        } else {
            NetworkQualityStatus.UNSTABLE
        }
        return quality
    }
    private fun classifyBaseline(latencyMs: Long): NetworkQualityStatus {
        return when {
            latencyMs <= 500 -> NetworkQualityStatus.EXCELLENT
            latencyMs <= 1200 -> NetworkQualityStatus.MODERATE
            else -> NetworkQualityStatus.UNSTABLE
        }
    }

}