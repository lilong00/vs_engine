package org.valkyrienskies.engine.ship

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import net.minecraft.world.entity.player.Player
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.VSBeta
import org.valkyrienskies.core.api.ships.*
import org.valkyrienskies.core.impl.api.ServerShipUser
import org.valkyrienskies.core.impl.api.Ticked
import org.valkyrienskies.core.impl.api.shipValue
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.mod.api.SeatedControllingPlayer
import kotlin.math.*

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
 class Gyros(val omega: Vector3dc, val vel: Vector3dc) : ShipForcesInducer, ServerShipUser, Ticked {

    @JsonIgnore
    // 船舶实例
    override var ship: ServerShip? = null

    @delegate:JsonIgnore
    // 控制玩家
    private val controllingPlayer by shipValue<SeatedControllingPlayer>()


    var aligning = false
    // 物理消耗
    private var physConsumption = 0f
    // 锚定状态
    private val anchored get() = anchorsActive > 0


    // 消耗
    var consumed = 0f
        private set

    // 坐下的玩家
    var seatedPlayer: Player? = null


    @OptIn(VSBeta::class)
    // 应用力量方法
    override fun applyForces(physShip: PhysShip) {
        // 如果舵小于1，启用流体阻力，因为所有的舵都已被摧毁
        if (helms < 1) {
            physShip.doFluidDrag = true
            return
        }
        // 当舵存在时，禁用流体阻力，因为它使船难以驾驶
        physShip.doFluidDrag = EngineConfig.SERVER.doFluidDrag

        physShip as PhysShipImpl

        val mass = physShip.inertia.shipMass
        val segment = physShip
        //val omega: Vector3dc = //SegmentUtils.getOmega(physShip.poseVel, segment, Vector3d())
        //val vel
        val balloonForceProvided = balloons * forcePerBalloon


        val buoyantFactorPerFloater = min(
            EngineConfig.SERVER.floaterBuoyantFactorPerKg / 15 / mass,
            EngineConfig.SERVER.maxFloaterBuoyantFactor
        )

        // 设置物理船的浮力因子
        physShip.buoyantFactor = 1.0 + floaters * buoyantFactorPerFloater

        // 稳定化
        //stabilize(physShip, omega, vel, segment, physShip, controllingPlayer == null && !aligning, controllingPlayer == null)
        stabilize(
            physShip,
            omega,
            vel,
            physShip,
            controllingPlayer == null && !aligning,
            controllingPlayer == null
        )

        var idealUpwardVel = Vector3d(0.0, 0.0, 0.0)

        // 区域 升降
        // 计算理想的向上力量
        val idealUpwardForce = Vector3d(
            0.0,
            idealUpwardVel.y()  - (GRAVITY / EngineConfig.SERVER.elevationSnappiness),
            0.0
        ).mul(mass * EngineConfig.SERVER.elevationSnappiness)

        // 计算实际的向上力量
        val actualUpwardForce = Vector3d(0.0, min(balloonForceProvided, max(idealUpwardForce.y(), 0.0)), 0.0)
        // 应用力量
        physShip.applyInvariantForce(actualUpwardForce)
        // 结束区域

        // 区域 锚定
        // 设置船只是否静止
        physShip.isStatic = anchored
        // 结束区域

        // 对y分量添加阻力
        physShip.applyInvariantForce(Vector3d().mul(-mass))
    }

    var anchors = 0
        set(v) {
            field = v; deleteIfEmpty()
        }

    // 活动的锚的数量
    var anchorsActive = 0
    // 气球的数量
    var balloons = 0
        set(v) {
            field = v; deleteIfEmpty()
        }

    // 舵的数量
    var helms = 0
        set(v) {
            field = v; deleteIfEmpty()
        }

    // 浮子的数量 * 15
    var floaters = 0
        set(v) {
            field = v; deleteIfEmpty()
        }

    // 每帧更新
    override fun tick() {
        consumed = physConsumption * /* should be phyics ticks based*/ 0.1f
        physConsumption = 0.0f
    }

    // 如果为空则删除
    private fun deleteIfEmpty() {
        if (helms <= 0 && floaters <= 0 && anchors <= 0 && balloons <= 0) {
            ship?.saveAttachment<Gyros>(null)
        }
    }

    companion object {
        // 获取或创建
        fun getOrCreate(ship: ServerShip) {
            //return ship.getAttachment<Gyros>()
                //?: Gyros().also { ship.saveAttachment(it) }
        }
        // 每个气球的力量
        private val forcePerBalloon get() = EngineConfig.SERVER.massPerBalloon * -GRAVITY

        // 重力
        private const val GRAVITY = -10.0
    }
 }