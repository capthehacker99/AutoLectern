package sys.exe.al.mixin;

import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
    @Shadow private int blockBreakingCooldown;

    @Shadow private float currentBreakingProgress;

    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(final BlockPos pos, final Direction direction, final CallbackInfoReturnable<Boolean> cir) {
        final var AL = AutoLectern.getInstance();
        if(!AL.breakCooldown && AL.getState() == ALState.BREAKING)
            this.blockBreakingCooldown = 0;
    }

    @Redirect(method = "updateBlockBreakingProgress", at = @At(value = "INVOKE", target = "Lnet/minecraft/block/BlockState;calcBlockBreakingDelta(Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/world/BlockView;Lnet/minecraft/util/math/BlockPos;)F"))
    private float antiBlockBreaking(final BlockState blockState, final PlayerEntity playerEntity, final BlockView blockView, final BlockPos blockPos) {
        final var beforeRet = blockState.calcBlockBreakingDelta(playerEntity, blockView, blockPos);
        if((this.currentBreakingProgress + beforeRet) < 1.f)
            return beforeRet;
        final var AL = AutoLectern.getInstance();
        final ALState state;
        if(AL.preBreaking && (state = AL.getState()) != ALState.STOPPED && state != ALState.BREAKING)
            return 0.f;
        return beforeRet;
    }

}
