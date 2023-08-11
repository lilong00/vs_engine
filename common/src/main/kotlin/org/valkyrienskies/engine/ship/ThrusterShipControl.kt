package org.valkyrienskies.engine.ship  // 船舶引擎包

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import org.joml.Vector3d
import org.joml.Vector3dc
import org.joml.Vector3i
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.core.api.ships.saveAttachment
import org.valkyrienskies.core.impl.api.ServerShipUser
import org.valkyrienskies.core.impl.api.ShipForcesInducer
import org.valkyrienskies.core.impl.api.Ticked
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD


@JsonAutoDetect(  // Json自动检测
    fieldVisibility = JsonAutoDetect.Visibility.ANY,  // 字段可见性为任何
    getterVisibility = JsonAutoDetect.Visibility.NONE,  // 获取器可见性为无
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,  // is获取器可见性为无
    setterVisibility = JsonAutoDetect.Visibility.NONE  // 设置器可见性为无
)

class ThrusterShipControl : ShipForcesInducer, ServerShipUser, Ticked {  // 推进器船舶控制类
    @JsonIgnore  // 忽略Json
    override var ship: ServerShip? = null  // 覆盖船舶：服务器船舶为空
    private var extraForce = 0.0  // 额外力量为0.0
    private var physConsumption = 0f  // 物理消耗为0
    private val farters = mutableListOf<Pair<Vector3i, Direction>>()  // 私有的farters为可变列表
    var consumed = 0f  // 消耗为0
        private set  // 私有设置

    override fun applyForces(physShip: PhysShip) {
        if (ship == null) return

        val forcesApplier = physShip

        physShip as PhysShipImpl

        physShip.buoyantFactor = 0.0 // 将浮力因子设为0，以忽略重量

        val shipCoords = ship!!.transform.positionInShip
        var falloff = 1.0

        val actualUpwardForce = Vector3d(0.0, (5000.0/falloff), 0.0)
        balloonpos.forEach {
            if (actualUpwardForce.isFinite) {
                forcesApplier.applyInvariantForceToPos(actualUpwardForce, Vector3d(it).sub(shipCoords))
            }
        }

        farters.forEach {
            val (pos, dir) = it

            val tPos = Vector3d(pos).add(0.5, 0.5, 0.5).sub(ship!!.transform.positionInShip)

            if (tPos.isFinite) {
                physShip.applyRotDependentForceToPos(dir.normal.toJOMLD().mul(-10000.0), tPos)
            }
        }
    }

    private var power = 0.0  // 力量为0.0

    private var balloonpos = mutableListOf<Vector3dc>()  // 气球位置为可变列表

    override fun tick() {  // 覆盖tick函数
        extraForce = power  // 额外力量为力量
        power = 0.0  // 力量为0.0
        consumed = physConsumption * /* should be phyics ticks based*/ 0.1f  // 消耗为物理消耗乘以0.1f
        physConsumption = 0.0f  // 物理消耗为0.0f
    }

    fun addFarter(pos: BlockPos, dir: Direction) {  // 添加farter函数
        farters.add(pos.toJOML() to dir)  // farters添加位置转换为JOML到方向
    }

    fun removeFarter(pos: BlockPos, dir: Direction) {  // 移除farter函数
        farters.remove(pos.toJOML() to dir)  // farters移除位置转换为JOML到方向
    }

    companion object {
        // 获取或创建与 ServerShip 关联的 ThrusterShipControl 实例的函数
        fun getOrCreate(ship: ServerShip): ThrusterShipControl {
            return ship.getAttachment<ThrusterShipControl>()
                ?: ThrusterShipControl().also { ship.saveAttachment(it) }
        }
    }
}


