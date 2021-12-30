package net.fabricmc.example.mixin;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.interfaces.IVillagerEntity;
import net.fabricmc.example.villagerenchants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.util.telemetry.TelemetrySender;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.CommandTreeS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityTrackerUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.SetTradeOffersS2CPacket;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.village.TradeOffer;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Mixin(ClientPlayNetworkHandler.class)
public class ClientPlayNetworkHandlerMixin {


    @Shadow
    private ClientWorld world;
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(MinecraftClient client, Screen screen, ClientConnection connection, GameProfile profile, TelemetrySender telemetrySender, CallbackInfo ci){
        ExampleMod.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }
    @Inject(method = "onCommandTree", at = @At("TAIL"))
    public void onOnCommandTree(CommandTreeS2CPacket packet, CallbackInfo ci) {
        ExampleMod.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }
    @ModifyVariable(method = "onEntityTrackerUpdate", at = @At("STORE"),ordinal = 0)
    private Entity onEntityTrackerUpdatePre(Entity entity) {
        if(entity != null && entity instanceof VillagerEntity){

            ((IVillagerEntity)entity).setprevProfession(((VillagerEntity)entity).getVillagerData().getProfession());
        }
        return entity;
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At("TAIL"),locals = LocalCapture.CAPTURE_FAILSOFT)
    public void onEntityTrackerUpdatePost(EntityTrackerUpdateS2CPacket packet, CallbackInfo ci,Entity entity) {
        if(!ExampleMod.ALdotrackpro) return;
        if (entity != null && entity instanceof VillagerEntity) {
            BlockPos lecloc = ExampleMod.lecloc;
            //System.out.println(((VillagerEntity)entity).getVillagerData().getProfession().toString());
            //System.out.println(((IVillagerEntity)entity).getprevProfession().toString());
            //System.out.println(entity.squaredDistanceTo(lecloc.getX(),lecloc.getY(),lecloc.getZ()));
            if(entity.squaredDistanceTo(lecloc.getX(),lecloc.getY(),lecloc.getZ()) <= 8){
                if(((VillagerEntity)entity).getVillagerData().getProfession() == VillagerProfession.LIBRARIAN && ((IVillagerEntity)entity).getprevProfession() != VillagerProfession.LIBRARIAN){
                    ExampleMod.yevilldatgotupdated = (VillagerEntity)entity;
                    ExampleMod.ALvillupdate = true;
                }
            }
        }
    }

    @Inject(method="onSetTradeOffers",at = @At("TAIL"))
    void onSetTradeOffers(SetTradeOffersS2CPacket packet,CallbackInfo ci) {
        if(!ExampleMod.ALdotracktrades) return;
        ExampleMod.NVI.price = 9999;
        ExampleMod.NVI.VE = villagerenchants.NONE;
        for(TradeOffer tof : packet.getOffers()){
            if(tof.getSellItem().getItem() == Items.ENCHANTED_BOOK){
                Map<Enchantment,Integer> encs = EnchantmentHelper.get(tof.getSellItem());
                if(!encs.isEmpty()){
                    Map.Entry<Enchantment,Integer> entry = encs.entrySet().iterator().next();
                    if(entry.getKey().getMaxLevel() == entry.getValue()) {
                        ExampleMod.NVI.price = tof.getAdjustedFirstBuyItem().getCount();
                        ExampleMod.NVI.VE = ExampleMod.e2ve.getOrDefault(entry.getKey(),villagerenchants.NONE);

                    }
                }
                break;
            }
        }
        ExampleMod.ALofferupdate = true;
    }
}
