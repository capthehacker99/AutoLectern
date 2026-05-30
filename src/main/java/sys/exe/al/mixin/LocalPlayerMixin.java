package sys.exe.al.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Environment(EnvType.CLIENT)
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {
    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float onGetYRot(LocalPlayer instance, Operation<Float> original) {
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            return original.call(instance);
        return AL.getYaw();
    }

    @WrapOperation(method = "sendPosition", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float onGetXRot(LocalPlayer instance, Operation<Float> original) {
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            return original.call(instance);
        return AL.getPitch();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getYRot()F"))
    private float onTickGetYRot(LocalPlayer instance, Operation<Float> original) {
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            return original.call(instance);
        return AL.getYaw();
    }

    @WrapOperation(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getXRot()F"))
    private float onTickGetXRot(LocalPlayer instance, Operation<Float> original) {
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            return original.call(instance);
        return AL.getPitch();
    }
}
