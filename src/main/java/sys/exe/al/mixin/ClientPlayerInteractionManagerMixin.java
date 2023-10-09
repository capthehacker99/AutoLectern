package sys.exe.al.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow private int blockBreakingCooldown;

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        final var AL = AutoLectern.getInstance();
        if(!AL.breakCooldown && AL.getState() == ALState.BREAKING)
            this.blockBreakingCooldown = 0;
    }
}
