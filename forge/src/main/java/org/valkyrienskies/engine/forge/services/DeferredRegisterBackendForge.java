package org.valkyrienskies.engine.forge.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.valkyrienskies.engine.forge.DeferredRegisterImpl;
import org.valkyrienskies.engine.registry.DeferredRegister;
import org.valkyrienskies.engine.services.DeferredRegisterBackend;

public class DeferredRegisterBackendForge implements DeferredRegisterBackend {

    @NotNull
    @Override
    public <T> DeferredRegister<T> makeDeferredRegister(
            @NotNull final String id,
            @NotNull final ResourceKey<Registry<T>> registry
    ) {
        return new DeferredRegisterImpl(id, registry);
    }
}
