package org.mechdancer.navigation.dwa

import org.mechdancer.navigation.dwa.process.*
import kotlin.math.absoluteValue
import kotlin.math.log2

private val conditions = setOf(
	//终端位置条件
	Condition(1.0) { local, _, trajectory ->
		local.nodes.last() distanceTo trajectory.nodes.last()
	},
	//终端方向条件
	Condition(1.0) { local, _, trajectory ->
		(local.nodes.last() deflectionTo trajectory.nodes.last()).absoluteValue
	},
	//全程贴合性条件
	Condition(1.5) { local, _, trajectory ->
		trajectory
			.nodes
			.map { pathNode -> pathNode.position }
			.sumByDouble { point ->
				local.segments.map { segment -> segment.distanceTo(point) }.min()!!
			}
	},
	//线速度条件
	Condition(1.0) { _, speed, _ ->
		log2(speed.first.absoluteValue + speed.second.absoluteValue)
	}
)

/**
 * 最优化函数
 * @param local 局部目标路径
 * @param current 当前位姿
 * @param speeds 速率样点集
 */
internal fun optimize(
	local: Trajectory,
	current: Node,
	speeds: Set<Pair<Double, Double>>
): Pair<Double, Double> {
	//轨迹集 = { 速率样点 -> 轨迹 }
	val trajectories = speeds.map { it to trajectory(current, it, 1.0, 5) }.toSet()
	//轨迹-条件表 := (条件 × 轨迹) -> 价值
	val table = (conditions descartes trajectories).map {
		it to it.first.f(local, it.second.first, it.second.second)
	}
	//按列计算归一化系数
	val normalizer =
		conditions
			.associate {
				it to table
					.filter { pair -> pair.first.first == it }
					.sumByDouble { pair -> pair.second }
					.let { value -> if (value > 0) it.k / value else .0 }
			}
	//最优化
	return trajectories.minBy { trajectory ->
		table
			.filter { it.first.second == trajectory }.toList()
			.sumByDouble { it.second * normalizer[it.first.first]!! }
	}!!.first
}
