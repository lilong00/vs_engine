package org.valkyrienskies.engine

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.CreativeModeTab
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import org.valkyrienskies.engine.registry.CreativeTabs
import org.valkyrienskies.engine.registry.DeferredRegister

@Suppress("unused")
object EngineItems {
    private val ITEMS = DeferredRegister.create(EngineMod.MOD_ID, Registry.ITEM_REGISTRY)
    val TAB: CreativeModeTab = CreativeTabs.create(
        ResourceLocation(
            EngineMod.MOD_ID,
            "engine_tab"
        )
    ) { ItemStack(EngineBlocks.ASSEMBLYCORE.get()) }

    fun register() {
        EngineBlocks.registerItems(ITEMS)
        ITEMS.applyAll()
    }

    private infix fun Item.byName(name: String) = ITEMS.register(name) { this }
}
