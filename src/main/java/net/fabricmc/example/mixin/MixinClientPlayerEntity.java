package net.fabricmc.example.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.StringReader;

import net.fabricmc.example.commands.ClientCommandManager;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import net.minecraft.network.encryption.PlayerPublicKey;
import net.minecraft.network.message.ChatMessageSigner;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public class MixinClientPlayerEntity extends AbstractClientPlayerEntity {


    public MixinClientPlayerEntity(ClientWorld world, GameProfile profile, @Nullable PlayerPublicKey publicKey) {
        super(world, profile, publicKey);
    }

    @Inject(method = "sendCommand(Lnet/minecraft/network/message/ChatMessageSigner;Ljava/lang/String;Lnet/minecraft/text/Text;)V", at = @At("HEAD"), cancellable = true)
    private void onSendCommand(ChatMessageSigner signer, String message, @Nullable Text preview, CallbackInfo ci) {
        StringReader reader = new StringReader(message);
        //int cursor = reader.getCursor();
        String commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(reader, message);
            ci.cancel();
        }
    }
    /*
    @Inject(method = "sendChatMessage", at = @At("HEAD"), cancellable = true)
    private void onSendChatMessage(String message, CallbackInfo ci) {
        if (message.startsWith("/")) {
            StringReader reader = new StringReader(message);
            reader.skip();
            int cursor = reader.getCursor();
            String commandName = reader.canRead() ? reader.readUnquotedString() : "";
            reader.setCursor(cursor);
            if (ClientCommandManager.isClientSideCommand(commandName)) {
                ClientCommandManager.executeCommand(reader, message);
                ci.cancel();
            }
        }
    }
*/

}