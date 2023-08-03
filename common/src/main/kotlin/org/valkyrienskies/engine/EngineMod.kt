package org.valkyrienskies.engine

import org.valkyrienskies.core.impl.config.VSConfigClass


object EngineMod {
    const val MOD_ID = "vs_engine"

    @JvmStatic
    fun init() {
        EngineBlocks.register()
        EngineBlockEntities.register()
        EngineItems.register()
        EngineScreens.register()
        EngineEntities.register()
        EngineWeights.register()
        VSConfigClass.registerConfig("vs_engine", EngineConfig::class.java)
    }

    @JvmStatic
    fun initClient() {
        EngineClientScreens.register()
    }
}
