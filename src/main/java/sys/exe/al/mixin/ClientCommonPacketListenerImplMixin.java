package sys.exe.al.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Environment(EnvType.CLIENT)
@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Redirect(method = "send", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;send(Lnet/minecraft/network/protocol/Packet;)V"))
    private void onSendPacket(final Connection instance, final Packet<?> packet){
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            instance.send(packet);
        else if(packet instanceof ServerboundMovePlayerPacket.PosRot mp)
            instance.send(new ServerboundMovePlayerPacket.PosRot(mp.getX(0), mp.getY(0), mp.getZ(0), AL.getYaw(), AL.getPitch(), mp.isOnGround(), mp.horizontalCollision()));
        else if (packet instanceof ServerboundMovePlayerPacket.Rot mp)
            instance.send(new ServerboundMovePlayerPacket.Rot(AL.getYaw(), AL.getPitch(), mp.isOnGround(), mp.horizontalCollision()));
        else
            instance.send(packet);
    }
}
