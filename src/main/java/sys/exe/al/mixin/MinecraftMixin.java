package sys.exe.al.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.util.Util;
import org.spongepowered.asm.mixin.Shadow;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Shadow
    private long lastActiveTime;

    @Shadow private volatile boolean running;

    @Inject(method = "tick()V", at = @At("HEAD"))
    private void onClientTick(CallbackInfo ci) {
        if (AutoLectern.getInstance().getState() != ALState.STOPPED)
            lastActiveTime = Util.getMillis();
        AutoLectern.getInstance().MinecraftTickHead((Minecraft) (Object) this);
    }

    @Inject(method = "stop", at = @At("HEAD"))
    private void onStop(CallbackInfo ci){
        if(running)
            AutoLectern.getInstance().saveConfig();
    }

    @WrapOperation(method = "continueAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/multiplayer/MultiPlayerGameMode;stopDestroyBlock()V"))
    private void onTryStopDestroying(MultiPlayerGameMode instance, Operation<Void> original) {
        if(AutoLectern.getInstance().getState() != ALState.STOPPED)
            return;
        original.call(instance);
    }
}
