package de.cxlina.capefix.mixin;

import de.cxlina.capefix.util.Fix;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class MixinGameRenderer {

    @Inject(method = "render", at = @At("HEAD"))
    public void frameStart(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        Fix.frameStart();
    }
}
