package sys.exe.al.mixin;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Mixin(MultiPlayerGameMode.class)
public class MultiPlayerGameModeMixin {
    @Shadow private int destroyDelay;

    @Shadow private float destroyProgress;

    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        final var AL = AutoLectern.getInstance();
        if(!AL.breakCooldown && (AL.getState() == ALState.BREAKING || (AL.preBreaking && (AL.getState() == ALState.WAITING_PROF || AL.getState() == ALState.WAITING_TRADE))))
            this.destroyDelay = 0;
    }

    @Redirect(method = "continueDestroyBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;getDestroyProgress(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;)F"))
    private float antiBlockBreaking(final BlockState blockState, final Player playerEntity, final BlockGetter blockView, final BlockPos blockPos) {
        final var beforeRet = blockState.getDestroyProgress(playerEntity, blockView, blockPos);
        if((this.destroyProgress + beforeRet) < 1.f)
            return beforeRet;
        final var AL = AutoLectern.getInstance();
        final ALState state;
        if(AL.preBreaking && (state = AL.getState()) != ALState.STOPPED && state != ALState.BREAKING)
            return 0.f;
        return beforeRet;
    }

}
