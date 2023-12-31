package org.valkyrienskies.engine

import com.github.imifou.jsonschema.module.addon.annotation.JsonSchema

object EngineConfig {
    @JvmField
    val CLIENT = Client()

    @JvmField
    val SERVER = Server()

    class Client

    class Server {

        @JsonSchema(description = "Movement power per engine when heated fully")
        val enginePower: Float = 2000000f

        @JsonSchema(description = "Movement power per engine with minimal heat")
        val minEnginePower: Float = 10000f

        @JsonSchema(description = "Turning power per engine when heated fully")
        val engineTurnPower = 1f

        @JsonSchema(description = "The amount of heat a engine loses per tick")
        val engineHeatLoss = 0.01f

        @JsonSchema(description = "The amount of heat a gain per tick (when burning)")
        val engineHeatGain = 0.03f

        @JsonSchema(description = "Increases heat gained at low heat level, and increased heat decreases when at high heat and not consuming fuel")
        val engineHeatChangeExponent = 0.1f

        @JsonSchema(description = "Avoids consuming fuel when heat is 100%")
        val engineFuelSaving = false

        @JsonSchema(description = "Increasing this value will result in more items being able to converted to fuel")
        val engineMinCapacity = 2000

        @JsonSchema(description = "Fuel burn time multiplier")
        val engineFuelMultiplier = 2f

        @JsonSchema(description = "Max speed of a ship without boosting")
        val maxCasualSpeed = 15.0

        @JsonSchema(description = "The speed at which the ship stabilizes")
        var stabilizationSpeed = 10.0

        @JsonSchema(description = "The amount extra that each floater will make the ship float, per kg mass")
        var floaterBuoyantFactorPerKg = 50_000.0

        @JsonSchema(description = "The maximum amount extra each floater will multiply the buoyant force by, irrespective of mass")
        var maxFloaterBuoyantFactor = 1.0

        // The velocity any ship at least can move at.
        @JsonSchema(description = "The speed a ship with no engines can move at")
        var baseSpeed = 10.0

        // Sensitivity of the up/down impulse buttons.
        // TODO maybe should be moved to VS2 client-side config?
        @JsonSchema(description = "Vertical sensitivity up ascend/descend")
        var baseImpulseElevationRate = 2.0

        @JsonSchema(description = "The max elevation speed boost gained by having extra extra balloons")
        var balloonElevationMaxSpeed = 5.5

        // Higher numbers make the ship accelerate to max speed faster
        @JsonSchema(description = "Ascend and descend acceleration")
        var elevationSnappiness = 1.0

        // Allow Engine controlled ships to be affected by fluid drag
        @JsonSchema(description = "Allow Engine controlled ships to be affected by fluid drag")
        var doFluidDrag = false

        // Do i need to explain? the mass 1 baloon gets to float
        @JsonSchema(description = "Amount of mass in kg a balloon can lift")
        var massPerBalloon = 5000.0

        // The amount of speed that the ship can move at when the left/right impulse button is held down.
        @JsonSchema(description = "The maximum linear velocity at any point on the ship caused by helm torque")
        var turnSpeed = 6.0

        @JsonSchema(description = "The maximum linear acceleration at any point on the ship caused by helm torque")
        var turnAcceleration = 10.0

        @JsonSchema(description = "The maximum distance from center of mass to one end of the ship considered by " +
                "the turn speed. At it's default of 16, it ensures that really large ships will turn at the same " +
                "speed as a ship with a center of mass only 16 blocks away from the farthest point in the ship. " +
                "That way, large ships do not turn painfully slowly")
        var maxSizeForTurnSpeedPenalty = 16.0

        // The strength used when trying to level the ship
        @JsonSchema(description = "How much torque a ship will apply to try and keep level")
        var stabilizationTorqueConstant = 15.0

        // Max anti-velocity used when trying to stop the ship
        @JsonSchema(description = "How fast a ship will stop. 1 = fast stop, 0 = slow stop")
        var linearStabilizeMaxAntiVelocity = 1.0

        // Anti-velocity mass relevance when stopping the ship
        // Max 10.0 (means no mass irrelevance)
        @JsonSchema(description = "How much inertia affects Engine ships. Max 10 = full inertia")
        var antiVelocityMassRelevance = 0.8

        // Chance that if side will pop, its this chance per side
        @JsonSchema(description = "Chance for popped balloons to pop adjacent balloons, per side")
        var popSideBalloonChance = 0.3

        // Blacklist of blocks that don't get added for ship building
        @JsonSchema(description = "Blacklist of blocks that don't get assembled")
        val ballastWeight: Double = 10000.0

        @JsonSchema(description = "Weight of ballast when highest redstone power")
        val ballastNoWeight: Double = 1000.0

        @JsonSchema(description = "Whether or not disassembly is permitted")
        val allowDisassembly = true

        @JsonSchema(description = "Maximum number of blocks allowed in a ship. Set to 0 for no limit")
        val maxShipBlocks = 320*320*320
    }
}
