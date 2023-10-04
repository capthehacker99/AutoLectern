package net.fabricmc.example.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.commands.ClientCommandManager;
import net.fabricmc.example.interfaces.IVillagerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.telemetry.WorldSession;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {


    @Shadow
    private ClientWorld world;
    @Final
    @Shadow
    private MinecraftClient client;
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(MinecraftClient client, Screen screen, ClientConnection connection, ServerInfo serverInfo, GameProfile profile, WorldSession worldSession, CallbackInfo ci) {
        ExampleMod.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }

    @Inject(method = "onCommandTree", at = @At("TAIL"))
    public void onOnCommandTree(CommandTreeS2CPacket packet, CallbackInfo ci) {
        ExampleMod.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }

    @ModifyVariable(method = "onEntityTrackerUpdate", at = @At("STORE"), ordinal = 0)
    private Entity onEntityTrackerUpdatePre(Entity entity) {
        if (entity instanceof VillagerEntity) {

            ((IVillagerEntity) entity).setprevProfession(((VillagerEntity) entity).getVillagerData().getProfession());
        }
        return entity;
    }

    @Inject(method = "onItemPickupAnimation", at = @At("HEAD"))
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo ci) {
        Entity entity = world.getEntityById(packet.getEntityId());
        LivingEntity livingEntity = (LivingEntity) world.getEntityById(packet.getCollectorEntityId());
        if (entity != null && (livingEntity == null || livingEntity == client.player) && entity instanceof ItemEntity && ((ItemEntity) entity).getStack().getItem() == Items.LECTERN) {
            ExampleMod.ALhasitemdropped = true;
        }
    }


    @Inject(method = "onEntityTrackerUpdate", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILSOFT)
    public void onEntityTrackerUpdatePost(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci, Entity entity) {
        if (!ExampleMod.ALdotrackpro) return;
        if (entity instanceof VillagerEntity) {
            BlockPos lecloc = ExampleMod.lecloc;
            //System.out.println(((VillagerEntity)entity).getVillagerData().getProfession().toString());
            //System.out.println(((IVillagerEntity)entity).getprevProfession().toString());
            //System.out.println(entity.squaredDistanceTo(lecloc.getX(),lecloc.getY(),lecloc.getZ()));
            if (entity.squaredDistanceTo(lecloc.getX(), lecloc.getY(), lecloc.getZ()) <= 8) {
                if (((VillagerEntity) entity).getVillagerData().getProfession() == VillagerProfession.LIBRARIAN && ((IVillagerEntity) entity).getprevProfession() != VillagerProfession.LIBRARIAN) {
                    ExampleMod.yevilldatgotupdated = (VillagerEntity) entity;
                    ExampleMod.ALvillupdate = true;
                }
            }
        }
    }

    @Inject(method = "onSetTradeOffers", at = @At("TAIL"))
    void onSetTradeOffers(SetTradeOffersS2CPacket packet, CallbackInfo ci) {
        if (!ExampleMod.ALdotracktrades) return;
        ExampleMod.NVI.price = 9999;
        ExampleMod.NVI.VE = null;
        for (TradeOffer tof : packet.getOffers()) {
            if (tof.getSellItem().getItem() == Items.ENCHANTED_BOOK) {
                Map<Enchantment, Integer> encs = EnchantmentHelper.get(tof.getSellItem());
                if (!encs.isEmpty()) {
                    Map.Entry<Enchantment, Integer> entry = encs.entrySet().iterator().next();
                    if (entry.getKey().getMaxLevel() == entry.getValue()) {
                        ExampleMod.NVI.price = tof.getOriginalFirstBuyItem().getCount();
                        ExampleMod.NVI.VE = entry.getKey();

                    }
                }
                break;
            }
        }
        ExampleMod.ALofferupdate = true;
    }

    @Inject(method = "Lnet/minecraft/client/network/ClientPlayNetworkHandler;sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(String message, CallbackInfo ci) {
        StringReader reader = new StringReader(message);
        String commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(reader, message);
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void sendCommand(String command, CallbackInfoReturnable<Boolean> cir) {
        StringReader reader = new StringReader(command);
        String commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(reader, command);
            cir.setReturnValue(true);
        }
    }
}
