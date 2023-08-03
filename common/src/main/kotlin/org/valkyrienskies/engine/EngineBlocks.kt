package org.valkyrienskies.engine

import net.minecraft.core.Registry
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.FireBlock
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockBehaviour
import net.minecraft.world.level.material.Material
import net.minecraft.world.level.material.MaterialColor
import org.valkyrienskies.engine.block.*
import org.valkyrienskies.engine.registry.DeferredRegister
import org.valkyrienskies.mod.common.hooks.VSGameEvents

@Suppress("unused")
object EngineBlocks {
    private val BLOCKS = DeferredRegister.create(EngineMod.MOD_ID, Registry.BLOCK_REGISTRY)

    val ANCHOR = BLOCKS.register("anchor", ::AnchorBlock)
    val ENGINE = BLOCKS.register("engine", ::EngineBlock)
    val FLOATER = BLOCKS.register("floater", ::FloaterBlock)
    val BALLAST = BLOCKS.register("ballast", ::BallastBlock)

    //val FARTER = BLOCKS.register("BaseThruster", ::ThrusterBlock)
    val FARTER = BLOCKS.register("farter", ::ThrusterBlock)
    //val ATMOSPHEREENGINE = BLOCKS.register("AtmosphereEngine", ::AtmosphereEngineBock)

    //陀螺仪
    val GYROS = BLOCKS.register("gyros", ::GyrosBlock)

    //驾驶控制器
    val CORE = BLOCKS.register("controllers") {
        ControllersBlock(
            BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD),
            WoodType.OAK
        )
    }

    // region Ship Helms
    val OAK_SHIP_HELM = BLOCKS.register("oak_ship_helm") {
        ShipHelmBlock(
            BlockBehaviour.Properties.of(Material.WOOD).strength(2.5F).sound(SoundType.WOOD),
            WoodType.OAK
        )
    }

    // region Balloons
    val BALLOON = BLOCKS.register("balloon") {
        BalloonBlock(
            BlockBehaviour.Properties.of(Material.WOOL, MaterialColor.WOOL).sound(SoundType.WOOL)
        )
    }
    // endregion

    fun register() {
        BLOCKS.applyAll()

        VSGameEvents.registriesCompleted.on { _, _ ->
            makeFlammables()
        }
    }

    // region Flammables
    // TODO make this part of the registration sequence
    fun flammableBlock(block: Block, flameOdds: Int, burnOdds: Int) {
        val fire = Blocks.FIRE as FireBlock
        fire.setFlammable(block, flameOdds, burnOdds)
    }

    fun makeFlammables() {
        flammableBlock(OAK_SHIP_HELM.get(), 5, 20)
        flammableBlock(BALLOON.get(), 30, 60)
        flammableBlock(FLOATER.get(), 5, 20)
    }
    // endregion

    // Blocks should also be registered as items, if you want them to be able to be held
    // aka all blocks
    fun registerItems(items: DeferredRegister<Item>) {
        BLOCKS.forEach {
            items.register(it.name) { BlockItem(it.get(), Item.Properties().tab(EngineItems.TAB)) }
        }
    }

}
