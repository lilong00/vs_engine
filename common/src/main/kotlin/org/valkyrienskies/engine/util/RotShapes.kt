package org.valkyrienskies.engine.util

import net.minecraft.core.Direction
import net.minecraft.world.phys.shapes.Shapes
import net.minecraft.world.phys.shapes.VoxelShape
import kotlin.math.max
import kotlin.math.min

interface RotShape {
    // 顺时针旋转90度
    fun rotate90(): RotShape

    // 顺时针旋转180度，相当于连续调用两次rotate90()
    fun rotate180(): RotShape = rotate90().rotate90()

    // 顺时针旋转270度，相当于连续调用三次rotate90()
    fun rotate270(): RotShape = rotate180().rotate90()

    // 创建 Minecraft 方块形状
    fun makeMcShape(): VoxelShape

    // 构建最终的 Minecraft 方块形状，同时进行优化
    fun build(): VoxelShape = makeMcShape().optimize()
}

class DirectionalShape(shape: RotShape) {
    val north = shape.build()
    val east = shape.rotate90().build()
    val south = shape.rotate180().build()
    val west = shape.rotate270().build()

    operator fun get(direction: Direction): VoxelShape = when (direction) {
        Direction.NORTH -> north
        Direction.EAST -> east
        Direction.SOUTH -> south
        Direction.WEST -> west
        else -> throw IllegalArgumentException()
    }

    companion object {
        fun north(shape: RotShape) = DirectionalShape(shape)
        fun east(shape: RotShape) = DirectionalShape(shape.rotate270())
        fun south(shape: RotShape) = DirectionalShape(shape.rotate180())
        fun west(shape: RotShape) = DirectionalShape(shape.rotate90())
    }
}

object RotShapes {
    // 创建一个立方体形状
    fun box(x1: Double, y1: Double, z1: Double, x2: Double, y2: Double, z2: Double): RotShape =
        Box(x1, y1, z1, x2, y2, z2)

    // 创建多个形状的并集
    fun or(vararg shapes: RotShape): RotShape = Union(shapes.asList())

    private class Box(val x1: Double, val y1: Double, val z1: Double, val x2: Double, val y2: Double, val z2: Double) :
        RotShape {

        // 顺时针旋转90度，相当于交换x坐标和z坐标
        override fun rotate90(): RotShape = Box(16 - z1, y1, x1, 16 - z2, y2, x2)

        // 创建 Minecraft 方块形状
        override fun makeMcShape(): VoxelShape = Shapes.box(
            min(x1, x2) / 16,
            min(y1, y2) / 16,
            min(z1, z2) / 16,
            max(x1, x2) / 16,
            max(y1, y2) / 16,
            max(z1, z2) / 16)
    }

    private class Union(val shapes: List<RotShape>) : RotShape {
        // 顺时针旋转90度，对每个形状调用rotate90()
        override fun rotate90(): RotShape = Union(shapes.map { it.rotate90() })

        // 创建 Minecraft 方块形状，是所有形状的并集
        override fun makeMcShape(): VoxelShape = shapes.fold(Shapes.empty()) { mc, n -> Shapes.or(mc, n.makeMcShape()) }
    }
}
