package net.fabricmc.example.mixin;

import net.fabricmc.example.ExampleMod;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Shadow
    private long lastWindowFocusedTime;
    @Inject(method = "render(FJZ)V",at = @At("HEAD"))
    public void render(float tickDelta, long startTime, boolean tick, CallbackInfo ci){
        if(ExampleMod.stage != 0) {
            lastWindowFocusedTime = Util.getMeasuringTimeMs();
        }
    }
}
