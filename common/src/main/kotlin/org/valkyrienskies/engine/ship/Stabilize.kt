package org.valkyrienskies.engine.ship

import org.apache.commons.compress.harmony.unpack200.SegmentUtils
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.PhysShip
import org.valkyrienskies.core.impl.game.ships.PhysShipImpl
import org.valkyrienskies.engine.EngineConfig

fun stabilize(
    ship: PhysShipImpl,  // 船只实现
    omega: Vector3dc,  // 角速度
    vel: Vector3dc,  // 速度
    forces: PhysShip,  // 物理船只
    linear: Boolean,  // 线性
    yaw: Boolean  // 偏航
) {
    val shipUp = Vector3d(0.0, 1.0, 0.0)  // 船只向上的向量
    val worldUp = Vector3d(0.0, 1.0, 0.0)  // 世界向上的向量
    ship.poseVel.rot.transform(shipUp)

    val angleBetween = shipUp.angle(worldUp)  // 船只向上的向量和世界向上的向量之间的角度
    val idealAngularAcceleration = Vector3d()  // 理想的角加速度
    if (angleBetween > .01) {
        val stabilizationRotationAxisNormalized = shipUp.cross(worldUp, Vector3d()).normalize()  // 稳定旋转轴归一化
        idealAngularAcceleration.add(
            stabilizationRotationAxisNormalized.mul(
                angleBetween,
                stabilizationRotationAxisNormalized
            )
        )
    }

    idealAngularAcceleration.sub(
        omega.x(),
        if (!yaw) 0.0 else omega.y(),
        omega.z()
    )

    val stabilizationTorque = ship.poseVel.rot.transform(
        ship.inertia.momentOfInertiaTensor.transform(
            ship.poseVel.rot.transformInverse(idealAngularAcceleration)
        )
    )

    stabilizationTorque.mul(EngineConfig.SERVER.stabilizationTorqueConstant)  // 稳定扭矩乘以稳定扭矩常数
    forces.applyInvariantTorque(stabilizationTorque)  // 应用不变扭矩

    if (linear) {
        val idealVelocity = Vector3d(vel).negate()  // 理想-+速度
        idealVelocity.y = 0.0

        if (idealVelocity.lengthSquared() > (EngineConfig.SERVER.linearStabilizeMaxAntiVelocity * EngineConfig.SERVER.linearStabilizeMaxAntiVelocity))
            idealVelocity.normalize(EngineConfig.SERVER.linearStabilizeMaxAntiVelocity)  // 如果理想速度的平方长度大于线性稳定最大反速度的平方，则将理想速度归一化

        idealVelocity.mul(ship.inertia.shipMass * (10 - EngineConfig.SERVER.antiVelocityMassRelevance))  // 理想速度乘以船只质量和反速度质量相关性的差
        forces.applyInvariantForce(idealVelocity)  // 应用不变力
    }
}

