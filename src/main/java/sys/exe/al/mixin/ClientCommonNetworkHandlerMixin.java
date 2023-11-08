package sys.exe.al.mixin;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

@Environment(EnvType.CLIENT)
@Mixin(ClientCommonNetworkHandler.class)
public class ClientCommonNetworkHandlerMixin {
    @Redirect(method = "sendPacket", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;send(Lnet/minecraft/network/packet/Packet;)V"))
    private void onSendPacket(final ClientConnection instance, final Packet<?> packet){
        final var AL = AutoLectern.getInstance();
        if(AL.getState() == ALState.STOPPED)
            instance.send(packet);
        else if(packet instanceof PlayerMoveC2SPacket.Full mp)
            instance.send(new PlayerMoveC2SPacket.Full(mp.getX(0), mp.getY(0), mp.getZ(0), AL.getYaw(), AL.getPitch(), mp.isOnGround()));
        else if (packet instanceof PlayerMoveC2SPacket.LookAndOnGround mp)
            instance.send(new PlayerMoveC2SPacket.LookAndOnGround(AL.getYaw(), AL.getPitch(), mp.isOnGround()));
        else
            instance.send(packet);
    }
}
