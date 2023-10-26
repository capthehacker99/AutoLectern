package sys.exe.al.mixin;

import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @SuppressWarnings("unused")
    @Shadow
    private long lastWindowFocusedTime;

    @Inject(method = "render(FJZ)V", at = @At("HEAD"))
    public void render(float tickDelta, long startTime, boolean tick, CallbackInfo ci) {
        if (AutoLectern.getInstance().getState() != ALState.STOPPED)
            lastWindowFocusedTime = Util.getMeasuringTimeMs();
    }
}
