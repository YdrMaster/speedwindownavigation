package org.mechdancer.navigation.dwa

import org.mechdancer.navigation.dwa.process.functions.descartes
import org.mechdancer.navigation.dwa.process.functions.div
import org.mechdancer.navigation.dwa.process.functions.times
import kotlin.math.PI
import kotlin.math.absoluteValue
import kotlin.math.max

/**
 * @param period             平均控制周期 秒
 * @param interestAreaRadius 感兴趣区半径 米
 * @param speedWindow        线速度限制  m  * s^-1
 * @param angularRateWindow  角速度限制 rad * s^-1
 * @param accelerationLimit        线加速度限制  m  * s^-2
 * @param angularAccelerationLimit 角加速度限制 rad * s^-2
 * @param speedSampleCount       线速度采样点数量
 * @param angularRateSampleCount 角速度采样点数量
 */
data class Configuration(
	val period: Double = 0.1,
	val interestAreaRadius: Double = 1.0,
	val speedWindow: ClosedFloatingPointRange<Double> = -0.5..0.5,
	val angularRateWindow: ClosedFloatingPointRange<Double> = -PI / 4..PI / 4,
	val accelerationLimit: Double = 0.1,
	val angularAccelerationLimit: Double = 0.1,
	val speedSampleCount: Int = 3,
	val angularRateSampleCount: Int = 3
) {
	private companion object {
		fun maxAbs(range: ClosedFloatingPointRange<Double>) =
			max(range.start.absoluteValue, range.endInclusive.absoluteValue)

		fun length(range: ClosedFloatingPointRange<Double>) =
			range.endInclusive - range.start
	}

	private fun speedDynamic(speed: Double) =
		accelerationLimit.let { speed - it..speed + it }

	private fun angularRateDynamic(angularRate: Double) =
		angularAccelerationLimit.let { angularRate - it..angularRate + it }

	/** 在某样附近采样 */
	fun dynamic(sample: Sample) =
		(speedDynamic(sample.first) * speedWindow / speedSampleCount).toSet() descartes
			(angularRateDynamic(sample.second) * angularRateWindow / angularRateSampleCount).toSet()

	init {
		assert(interestAreaRadius > maxAbs(speedWindow) * period)
		assert(length(speedWindow) > accelerationLimit)
		assert(length(speedWindow) > angularAccelerationLimit)
		assert(accelerationLimit > 0)
		assert(angularAccelerationLimit > 0)
		assert(speedSampleCount >= 2)
		assert(angularRateSampleCount >= 2)
	}
}
