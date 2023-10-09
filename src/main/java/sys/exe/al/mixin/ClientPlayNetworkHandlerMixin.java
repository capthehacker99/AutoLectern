package sys.exe.al.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.EnchantedBookItem;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;
import sys.exe.al.commands.ClientCommandManager;
import sys.exe.al.interfaces.ExtraVillagerData;

import static sys.exe.al.AutoLectern.SIGNAL_ITEM;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Unique
    private int merchantSyncId = -1;
    @SuppressWarnings("unused")
    protected ClientPlayNetworkHandlerMixin(final MinecraftClient client, final ClientConnection connection, final ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(final MinecraftClient client, final ClientConnection clientConnection, final ClientConnectionState clientConnectionState, final CallbackInfo ci) {
        AutoLectern.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(final OpenScreenS2CPacket packet, final CallbackInfo ci) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_TRADE) return;
        if(packet.getScreenHandlerType() != ScreenHandlerType.MERCHANT)
            return;
        merchantSyncId = packet.getSyncId();
        ci.cancel();
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"), cancellable = true)
    private void onCloseScreen(final CloseScreenS2CPacket packet, final CallbackInfo ci) {
        if(packet.getSyncId() != merchantSyncId)
            return;
        merchantSyncId = -1;
        ci.cancel();
    }
    @SuppressWarnings("unchecked")
    @Inject(method = "onCommandTree", at = @At("TAIL"))
    private void onOnCommandTree(final CommandTreeS2CPacket packet, final CallbackInfo ci) {
        AutoLectern.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher);
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket;trackedValues()Ljava/util/List;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPreEntityTrackerUpdate(final EntityTrackerUpdateS2CPacket packet, final CallbackInfo ci, final Entity entity) {
        if (entity instanceof final VillagerEntity vil)
            ((ExtraVillagerData)vil).autolec$setPrevProfession(vil.getVillagerData().getProfession());
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;writeUpdatedEntries(Ljava/util/List;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPostEntityTrackerUpdate(final EntityTrackerUpdateS2CPacket packet, final CallbackInfo ci, final Entity entity) {
        final var AL = AutoLectern.getInstance();
        if(AL.getState() != ALState.WAITING_PROF ||
            AL.getUpdatedVillager() != null)
            return;
        if (!(entity instanceof final VillagerEntity vil))
            return;
        if(AL.getLecternPos().getSquaredDistance(vil.getPos()) > 8)
            return;
        if(vil.getVillagerData().getProfession() != VillagerProfession.LIBRARIAN)
            return;
        if(((ExtraVillagerData)vil).autolec$getPrevProfession() == VillagerProfession.LIBRARIAN)
            return;
        AL.setUpdatedVillager(vil);
        AL.signal(AutoLectern.SIGNAL_PROF);
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getStack()Lnet/minecraft/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo ci, Entity entity, LivingEntity livingEntity, ItemEntity itemEntity) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_ITEM) return;
        if(itemEntity.getStack().getItem() == Items.LECTERN)
            AL.signal(SIGNAL_ITEM);
    }

    @Inject(method = "onSetTradeOffers", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onSetTradeOffers(final SetTradeOffersS2CPacket packet, final CallbackInfo ci) {
        if(packet.getSyncId() != merchantSyncId) return;
        sendPacket(new CloseHandledScreenC2SPacket(merchantSyncId));
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_TRADE) return;
        ++AL.attempts;
        for(final var offer : packet.getOffers()) {
            final var sellItem = offer.getSellItem();
            if(sellItem.getItem() != Items.ENCHANTED_BOOK)
                continue;
            for(final var entry : EnchantedBookItem.getEnchantmentNbt(sellItem)) {
                final var compound = (NbtCompound)entry;
                final var opt = Registries.ENCHANTMENT.getOrEmpty(EnchantmentHelper.getIdFromNbt(compound));
                if(opt.isEmpty())
                    continue;
                final var enchant = opt.get();
                final var lvl = EnchantmentHelper.getLevelFromNbt(compound);
                if(AL.logTrade) {
                    final var plr = this.client.player;
                    if(plr != null) {
                        plr.sendMessage(Text.literal("[Auto Lectern] ")
                                .formatted(Formatting.YELLOW)
                                .append(
                                        Text.literal(String.valueOf(offer.getOriginalFirstBuyItem().getCount()))
                                            .formatted(Formatting.GREEN).append(
                                                Text.literal(" emeralds for ")
                                                        .formatted(Formatting.WHITE)
                                                        .append(enchant.getName(lvl)).append(Text.literal(" [" + AL.attempts + "]")
                                                                .formatted(Formatting.WHITE)
                                                        )
                                                )
                                )
                        );
                    }
                }
                if(AL.isGoalMet(offer.getOriginalFirstBuyItem().getCount(), enchant, lvl)) {
                    AL.signal(AutoLectern.SIGNAL_TRADE | AutoLectern.SIGNAL_TRADE_OK);
                    ci.cancel();
                    return;
                }
            }
        }
        AL.signal(AutoLectern.SIGNAL_TRADE);
        ci.cancel();
    }

    @Inject(method = "sendChatCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(final String message, final CallbackInfo ci) {
        final var reader = new StringReader(message);
        final var commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(client, reader, message);
            ci.cancel();
        }
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)Z", at = @At("HEAD"), cancellable = true)
    private void sendCommand(final String command, final CallbackInfoReturnable<Boolean> cir) {
        final var reader = new StringReader(command);
        final var commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(client, reader, command);
            cir.setReturnValue(true);
        }
    }
}
