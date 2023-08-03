package org.valkyrienskies.engine.fabric;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.model.BakedModelManagerHelper;
import net.fabricmc.fabric.api.client.model.ModelLoadingRegistry;
import net.fabricmc.fabric.api.client.rendereregistry.v1.BlockEntityRendererRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import org.valkyrienskies.core.impl.config.VSConfigClass;
import org.valkyrienskies.engine.EngineBlockEntities;
import org.valkyrienskies.engine.EngineConfig;
import org.valkyrienskies.engine.EngineMod;
import org.valkyrienskies.engine.block.WoodType;
import org.valkyrienskies.engine.blockentity.renderer.ShipHelmBlockEntityRenderer;
import org.valkyrienskies.engine.blockentity.renderer.WheelModels;
import org.valkyrienskies.mod.compat.clothconfig.VSClothConfig;
import org.valkyrienskies.mod.fabric.common.ValkyrienSkiesModFabric;

public class EngineModFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        // force VS2 to load before engine
        new ValkyrienSkiesModFabric().onInitialize();

        EngineMod.init();
    }

    @Environment(EnvType.CLIENT)
    public static class Client implements ClientModInitializer {

        @Override
        public void onInitializeClient() {
            EngineMod.initClient();
            BlockEntityRendererRegistry.INSTANCE.register(
                    EngineBlockEntities.INSTANCE.getSHIP_HELM().get(),
                    ShipHelmBlockEntityRenderer::new
            );

            ModelLoadingRegistry.INSTANCE.registerModelProvider((manager, out) -> {
                for (WoodType woodType : WoodType.values()) {
                    out.accept(new ResourceLocation(EngineMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel"));
                }
            });

            WheelModels.INSTANCE.setModelGetter(woodType ->
                BakedModelManagerHelper.getModel(Minecraft.getInstance().getModelManager(),
                    new ResourceLocation(EngineMod.MOD_ID, "block/" + woodType.getResourceName() + "_ship_helm_wheel")));
        }
    }

    public static class ModMenu implements ModMenuApi {
        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
            return (parent) -> VSClothConfig.createConfigScreenFor(
                    parent,
                    VSConfigClass.Companion.getRegisteredConfig(EngineConfig.class)
            );
        }
    }
}
