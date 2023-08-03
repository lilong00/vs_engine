package org.valkyrienskies.engine.ship

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
import org.valkyrienskies.core.impl.pipelines.SegmentUtils
import org.valkyrienskies.core.impl.util.y
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.common.util.toJOMLD

@JsonAutoDetect(
    fieldVisibility = JsonAutoDetect.Visibility.ANY,
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.NONE
)

class ThrusterShipControl : ShipForcesInducer, ServerShipUser, Ticked {
    @JsonIgnore
    override var ship: ServerShip? = null

    private var extraForce = 0.0
    private var physConsumption = 0f
    private val farters = mutableListOf<Pair<Vector3i, Direction>>()
    var consumed = 0f
        private set

    override fun applyForces(physShip: PhysShip) {
        if (ship == null) return

        val forcesApplier = physShip

        physShip as PhysShipImpl

        physShip.buoyantFactor = 1.0

        //physShip
        val shipCoordsinworld: Vector3dc = physShip.poseVel.pos

        val shipCoords = ship!!.transform.positionInShip
        var falloffamount = 0.0
        var falloff = 1.0

        if (shipCoordsinworld.y > 256) {
            falloffamount = shipCoordsinworld.y - 256
            falloff = falloffamount*(1.5)

        } else {
            falloff = 1.0
        }
        val actualUpwardForce = Vector3d(0.0, (5000.0/falloff), 0.0)
        balloonpos.forEach {
            if (actualUpwardForce.isFinite) {
                forcesApplier.applyInvariantForceToPos(actualUpwardForce, Vector3d(it).sub(shipCoords))
            }
        }

        farters.forEach {
            val (pos, dir) = it

            val tPos = Vector3d(pos).add( 0.5, 0.5, 0.5).sub(ship!!.transform.positionInShip)

            if (tPos.isFinite) {
                physShip.applyRotDependentForceToPos(dir.normal.toJOMLD().mul(-10000.0), tPos)
            }
        }
    }
    var power = 0.0

    var balloons = 0 // Amount of balloons
        set(v) {
            field = v; deleteIfEmpty()
        }

    var balloonpos = mutableListOf<Vector3dc>()

    override fun tick() {
        extraForce = power
        power = 0.0
        consumed = physConsumption * /* should be phyics ticks based*/ 0.1f
        physConsumption = 0.0f
    }

    private fun deleteIfEmpty() {
        if (balloons == 0) {
            ship?.saveAttachment<ThrusterShipControl>(null)
        }
    }

    fun addFarter(pos: BlockPos, dir: Direction) {
        farters.add(pos.toJOML() to dir)
    }

    fun removeFarter(pos: BlockPos, dir: Direction) {
        farters.remove(pos.toJOML() to dir)
    }

    companion object {
        fun getOrCreate(ship: ServerShip): ThrusterShipControl {
            return ship.getAttachment<ThrusterShipControl>()
                ?: ThrusterShipControl().also { ship.saveAttachment(it) }
        }
    }
}