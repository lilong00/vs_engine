package org.valkyrienskies.engine.ship

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import net.minecraft.core.Direction
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.world.entity.player.Player
import org.joml.AxisAngle4d
import org.joml.Quaterniond
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.VSBeta
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.core.api.ships.saveAttachment
import org.valkyrienskies.core.impl.api.ServerShipUser
import org.valkyrienskies.core.impl.api.ShipForcesInducer
import org.valkyrienskies.core.impl.api.Ticked
import org.valkyrienskies.core.impl.api.shipValue
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.core.impl.pipelines.SegmentUtils
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.mod.api.SeatedControllingPlayer
import org.valkyrienskies.mod.common.util.toJOMLD
import kotlin.math.*

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)
@JsonIgnoreProperties(ignoreUnknown = true)
 class Stabilizer: ShipForcesInducer, ServerShipUser, Ticked {

    @JsonIgnore
    // 船舶实例
    override var ship: ServerShip? = null

    @delegate:JsonIgnore
    // 控制玩家
    private val controllingPlayer by shipValue<SeatedControllingPlayer>()

    // 额外力量
    private var extraForce = 0.0
    // 对齐标志
    var aligning = false
    // 拆解标志，也会影响位置
    var disassembling = false
    // 物理消耗
    private var physConsumption = 0f
    // 锚定状态
    private val anchored get() = anchorsActive > 0

    // 对齐角度
    private var angleUntilAligned = 0.0
    // 对齐位置
    private var positionUntilAligned = Vector3d()
    // 对齐目标
    private var alignTarget = 0
    // 是否可以拆解
    val canDisassemble
        get() = ship != null &&
                disassembling &&
                abs(angleUntilAligned) < DISASSEMBLE_THRESHOLD &&
                positionUntilAligned.distanceSquared(this.ship!!.transform.positionInWorld) < 4.0
    // 对齐方向
    val aligningTo: Direction get() = Direction.from2DDataValue(alignTarget)
    // 消耗
    var consumed = 0f
        private set

    // 巡航按键状态
    private var wasCruisePressed = false
    @JsonProperty("cruise")
    // 巡航状态
    var isCruising = false
    // 控制数据
    private var controlData: ControlData? = null

    @JsonIgnore
    // 坐下的玩家
    var seatedPlayer: Player? = null

    // 控制数据类
    private data class ControlData(
        val seatInDirection: Direction,
        var forwardImpulse: Float = 0.0f,
        var leftImpulse: Float = 0.0f,
        var upImpulse: Float = 0.0f,
        var sprintOn: Boolean = false
    ) {
        companion object {
            // 创建控制数据
            fun create(player: SeatedControllingPlayer): ControlData {
                return ControlData(
                    player.seatInDirection,
                    player.forwardImpulse,
                    player.leftImpulse,
                    player.upImpulse,
                    player.sprintOn
                )
            }
        }
    }


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

        val ship = ship ?: return
        val mass = physShip.inertia.shipMass
        val moiTensor = physShip.inertia.momentOfInertiaTensor
        val segment = physShip.segments.segments[0]?.segmentDisplacement!!
        val omega: Vector3dc = SegmentUtils.getOmega(physShip.poseVel, segment, Vector3d())
        val vel = SegmentUtils.getVelocity(physShip.poseVel, segment, Vector3d())
        val balloonForceProvided = balloons * forcePerBalloon


        val buoyantFactorPerFloater = min(
            EngineConfig.SERVER.floaterBuoyantFactorPerKg / 15 / mass,
            EngineConfig.SERVER.maxFloaterBuoyantFactor
        )

        // 设置物理船的浮力因子
        physShip.buoyantFactor = 1.0 + floaters * buoyantFactorPerFloater
        // 重新访问引擎控制代码
        // [x] 移动扭矩稳定化代码
        // [x] 移动线性稳定化代码
        // [x] 重新访问玩家控制的扭矩
        // [x] 重新访问玩家控制的线性力
        // [x] 锚定冻结
        // [x] 重写对齐代码
        // [x] 重新访问升降代码
        // [x] 气球限制器
        // [ ] 添加巡航代码
        // [ ] 基于船大小的旋转
        // [x] 引擎消耗
        // [ ] 修复升降灵敏度

        // 对齐区域

        val invRotation = physShip.poseVel.rot.invert(Quaterniond())
        val invRotationAxisAngle = AxisAngle4d(invRotation)
        // Floor使一个数字为0到3，对应于方向
        alignTarget = floor((invRotationAxisAngle.angle / (PI * 0.5)) + 4.5).toInt() % 4
        angleUntilAligned = (alignTarget.toDouble() * (0.5 * Math.PI)) - invRotationAxisAngle.angle
        // 如果正在拆解，计算对齐位置
        if (disassembling) {
            val pos = ship.transform.positionInWorld
            positionUntilAligned = pos.floor(Vector3d())
            val direction = pos.sub(positionUntilAligned, Vector3d())
            physShip.applyInvariantForce(direction)
        }
        // 如果正在对齐并且对齐角度大于阈值，计算理想扭矩并应用
        if ((aligning) && abs(angleUntilAligned) > ALIGN_THRESHOLD) {
            if (angleUntilAligned < 0.3 && angleUntilAligned > 0.0) angleUntilAligned = 0.3
            if (angleUntilAligned > -0.3 && angleUntilAligned < 0.0) angleUntilAligned = -0.3

            val idealOmega = Vector3d(invRotationAxisAngle.x, invRotationAxisAngle.y, invRotationAxisAngle.z)
                .mul(-angleUntilAligned)
                .mul(EngineConfig.SERVER.stabilizationSpeed)

            val idealTorque = moiTensor.transform(idealOmega)

            physShip.applyInvariantTorque(idealTorque)
        }
        // 结束对齐区域

        // 稳定化
        stabilize(
            physShip,
            omega,
            vel,
            segment,
            physShip,
            controllingPlayer == null && !aligning,
            controllingPlayer == null
        )

        var idealUpwardVel = Vector3d(0.0, 0.0, 0.0)

        val player = controllingPlayer

        // 如果玩家存在
        if (player != null) {
            val currentControlData = ControlData.create(player)

            // 如果玩家当前正在控制船
            if (!wasCruisePressed && player.cruise) {
                // 玩家按下了巡航按钮
                isCruising = !isCruising
                showCruiseStatus()
            } else if (!player.cruise
                && isCruising
                && (player.leftImpulse != 0.0f || player.sprintOn || player.upImpulse != 0.0f || player.forwardImpulse != 0.0f)
                && currentControlData != controlData
            ) {
                // 玩家按下了另一个按钮
                isCruising = false
                showCruiseStatus()
            }

            // 如果不在巡航状态，只接受最新的控制数据
            if (!isCruising) {
                controlData = currentControlData
            }

            wasCruisePressed = player.cruise
        } else if (!isCruising) {
            // 如果玩家不在控制船，并且不在巡航状态，重置控制数据
            controlData = null
        }


        controlData?.let { control ->
            // 区域 玩家控制的旋转
            val transform = physShip.transform
            val aabb = ship.worldAABB
            val center = transform.positionInWorld
            val stw = transform.shipToWorld
            val wts = transform.worldToShip

            // 计算最大距离
            val largestDistance = run {
                var dist = center.distance(aabb.minX(), center.y(), aabb.minZ())
                dist = max(dist, center.distance(aabb.minX(), center.y(), aabb.maxZ()))
                dist = max(dist, center.distance(aabb.maxX(), center.y(), aabb.minZ()))
                dist = max(dist, center.distance(aabb.maxX(), center.y(), aabb.maxZ()))

                dist
            }.coerceIn(0.5, EngineConfig.SERVER.maxSizeForTurnSpeedPenalty)

            val maxLinearAcceleration = EngineConfig.SERVER.turnAcceleration
            val maxLinearSpeed = EngineConfig.SERVER.turnSpeed +
                    extraForce / EngineConfig.SERVER.enginePower * EngineConfig.SERVER.engineTurnPower

            // acceleration = alpha * r
            // 因此: maxAlpha = maxAcceleration / r
            val maxOmegaY = maxLinearSpeed / largestDistance
            val maxAlphaY = maxLinearAcceleration / largestDistance

            val isBelowMaxTurnSpeed = abs(omega.y()) < maxOmegaY

            val normalizedAlphaYMultiplier =
                if (isBelowMaxTurnSpeed && control.leftImpulse != 0.0f) control.leftImpulse.toDouble()
                else -omega.y().coerceIn(-1.0, 1.0)

            val idealAlphaY = normalizedAlphaYMultiplier * maxAlphaY

            val alpha = Vector3d(0.0, idealAlphaY, 0.0)
            val angularImpulse =
                stw.transformDirection(moiTensor.transform(wts.transformDirection(Vector3d(alpha))))

            val torque = Vector3d(angularImpulse)
            physShip.applyInvariantTorque(torque)
            // 结束区域

            // 区域 玩家控制的倾斜
            val rotationVector = control.seatInDirection.normal.toJOMLD()

            physShip.poseVel.transformDirection(rotationVector)

            rotationVector.y = 0.0

            rotationVector.mul(idealAlphaY * -1.5)

            SegmentUtils.transformDirectionWithScale(
                physShip.poseVel,
                segment,
                moiTensor.transform(
                    SegmentUtils.invTransformDirectionWithScale(
                        physShip.poseVel,
                        segment,
                        rotationVector,
                        rotationVector
                    )
                ),
                rotationVector
            )

            physShip.applyInvariantTorque(rotationVector)

            val forwardVector = control.seatInDirection.normal.toJOMLD()
            SegmentUtils.transformDirectionWithoutScale(
                physShip.poseVel,
                segment,
                forwardVector,
                forwardVector
            )
            forwardVector.y *= 0.1 // 减少垂直推力
            forwardVector.normalize()

            forwardVector.mul(control.forwardImpulse.toDouble())

            val playerUpDirection = physShip.poseVel.transformDirection(Vector3d(0.0, 1.0, 0.0))
            val velOrthogonalToPlayerUp =
                vel.sub(playerUpDirection.mul(playerUpDirection.dot(vel), Vector3d()), Vector3d())

            // 这是船只总是可以出去的速度，没有引擎
            val baseForwardVel = Vector3d(forwardVector).mul(EngineConfig.SERVER.baseSpeed)
            val baseForwardForce = Vector3d(baseForwardVel).sub(velOrthogonalToPlayerUp).mul(mass * 10)

            // 这是我们在任何情况下都想去的最大速度（当不冲刺时）
            val idealForwardVel = Vector3d(forwardVector).mul(EngineConfig.SERVER.maxCasualSpeed)
            val idealForwardForce = Vector3d(idealForwardVel).sub(velOrthogonalToPlayerUp).mul(mass * 10)

            val extraForceNeeded = Vector3d(idealForwardForce).sub(baseForwardForce)
            val actualExtraForce = Vector3d(baseForwardForce)

            if (extraForce != 0.0) {
                actualExtraForce.fma(min(extraForce / extraForceNeeded.length(), 1.0), extraForceNeeded)
            }


            // 应用力量
            physShip.applyInvariantForce(actualExtraForce)
            // 结束区域

            // 玩家控制的升降
            if (control.upImpulse != 0.0f) {
                // 计算理想的向上速度
                idealUpwardVel = Vector3d(0.0, 1.0, 0.0)
                    .mul(control.upImpulse.toDouble())
                    .mul(EngineConfig.SERVER.baseImpulseElevationRate +
                            // 平滑处理，当你接近气球最大速度时，升降速度如何缩放
                            smoothing(2.0, EngineConfig.SERVER.balloonElevationMaxSpeed, balloonForceProvided / mass)
                    )
            }
        }

        // 区域 升降
        // 计算理想的向上力量
        val idealUpwardForce = Vector3d(
            0.0,
            idealUpwardVel.y() - vel.y() - (GRAVITY / EngineConfig.SERVER.elevationSnappiness),
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
        physShip.applyInvariantForce(Vector3d(vel.y()).mul(-mass))
    }

    // 显示巡航状态
    private fun showCruiseStatus() {
        val cruiseKey = if (isCruising) "hud.vs_engine.start_cruising" else "hud.vs_engine.stop_cruising"
        seatedPlayer?.displayClientMessage(TranslatableComponent(cruiseKey), true)
    }

    // 动力
    var power = 0.0
    // 锚的数量
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
        extraForce = power
        power = 0.0
        consumed = physConsumption * /* should be phyics ticks based*/ 0.1f
        physConsumption = 0.0f
    }

    // 如果为空则删除
    private fun deleteIfEmpty() {
        if (helms <= 0 && floaters <= 0 && anchors <= 0 && balloons <= 0) {
            ship?.saveAttachment<Stabilizer>(null)
        }
    }

    /**
     * f(x) = max - smoothing / (x + (smoothing / max))
     * 平滑函数
     */
    private fun smoothing(smoothing: Double, max: Double, x: Double): Double = max - smoothing / (x + (smoothing / max))

    companion object {
        // 获取或创建
        fun getOrCreate(ship: ServerShip): Stabilizer {
            return ship.getAttachment<Stabilizer>()
                ?: Stabilizer().also { ship.saveAttachment(it) }
        }

        // 对齐阈值
        private const val ALIGN_THRESHOLD = 0.01
        // 拆解阈值
        private const val DISASSEMBLE_THRESHOLD = 0.02
        // 每个气球的力量
        private val forcePerBalloon get() = EngineConfig.SERVER.massPerBalloon * -GRAVITY

        // 重力
        private const val GRAVITY = -10.0
    }
 }