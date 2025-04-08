package sys.exe.al.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommonNetworkHandler;
import net.minecraft.client.network.ClientConnectionState;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.command.CommandSource;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket;
import net.minecraft.network.packet.c2s.play.SelectMerchantTradeC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.registry.BuiltinRegistries;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.screen.sync.ComponentChangesHash;
import net.minecraft.screen.sync.ItemStackHash;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.village.TradeOfferList;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sys.exe.al.ALAutoTrade;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;
import sys.exe.al.commands.ClientCommandManager;
import sys.exe.al.interfaces.ExtraVillagerData;

import static sys.exe.al.AutoLectern.SIGNAL_ITEM;


@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixin extends ClientCommonNetworkHandler {
    @Shadow
    private CommandDispatcher<CommandSource> commandDispatcher;

    @Shadow
    @Final
    private ComponentChangesHash.ComponentHasher componentHasher;

    @Unique
    private int merchantSyncId = -1;

    @SuppressWarnings("unused")
    protected ClientPlayNetworkHandlerMixin(final MinecraftClient client, final ClientConnection connection, final ClientConnectionState connectionState) {
        super(client, connection, connectionState);
    }

    @Unique
    private static int findCheapestTrade(final TradeOfferList offers, final ClientPlayerEntity plr) {
        int availEmerald = 0;
        int availBook = 0;
        int availPaper = 0;
        for (final var stack : plr.getInventory().getMainStacks()) {
            if (stack.isEmpty())
                continue;
            final var itm = stack.getItem();
            if (itm == Items.EMERALD) availEmerald += stack.getCount();
            else if (itm == Items.BOOK) availBook += stack.getCount();
            else if (itm == Items.PAPER) availPaper += stack.getCount();
        }
        int minIdx = -1;
        float minCost = Float.MAX_VALUE;
        int curIdx = minIdx;
        for (final var offer : offers) {
            ++curIdx;
            float curCost = 0;
            final var first = offer.getDisplayedFirstBuyItem();
            final var firstItem = first.getItem();
            if (firstItem == Items.EMERALD) {
                final var count = first.getCount();
                if (count > availEmerald) {
                    continue;
                }
                curCost += count;
            } else if (firstItem == Items.PAPER) {
                final var count = first.getCount();
                if (first.getCount() > availPaper) {
                    continue;
                }
                curCost += count * 0.015625f;
            } else if (firstItem == Items.BOOK) {
                final var count = first.getCount();
                if (first.getCount() > availBook) {
                    continue;
                }
                curCost += count * 0.146875f;
            } else
                continue;
            if (curCost < minCost) {
                minIdx = curIdx;
                minCost = curCost;
            }
        }
        return minIdx;
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(final MinecraftClient client, final ClientConnection clientConnection, final ClientConnectionState clientConnectionState, final CallbackInfo ci) {
        AutoLectern.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher, CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup()));
    }

    @Inject(method = "onOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(final OpenScreenS2CPacket packet, final CallbackInfo ci) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_TRADE) return;
        if (packet.getScreenHandlerType() != ScreenHandlerType.MERCHANT)
            return;
        merchantSyncId = packet.getSyncId();
        ci.cancel();
    }

    @Inject(method = "onCloseScreen", at = @At("HEAD"), cancellable = true)
    private void onCloseScreen(final CloseScreenS2CPacket packet, final CallbackInfo ci) {
        if (packet.getSyncId() != merchantSyncId)
            return;
        merchantSyncId = -1;
        ci.cancel();
    }

    @SuppressWarnings("unchecked")
    @Inject(method = "onCommandTree", at = @At("TAIL"))
    private void onOnCommandTree(final CommandTreeS2CPacket packet, final CallbackInfo ci) {
        AutoLectern.registerCommands((CommandDispatcher<ServerCommandSource>) (Object) commandDispatcher, CommandManager.createRegistryAccess(BuiltinRegistries.createWrapperLookup()));
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/packet/s2c/play/EntityTrackerUpdateS2CPacket;trackedValues()Ljava/util/List;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPreEntityTrackerUpdate(final EntityTrackerUpdateS2CPacket packet, final CallbackInfo ci, final Entity entity) {
        if (entity instanceof final VillagerEntity vil)
            ((ExtraVillagerData) vil).autolec$setPrevProfession(vil.getVillagerData().profession());
    }

    @Inject(method = "onEntityTrackerUpdate", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/data/DataTracker;writeUpdatedEntries(Ljava/util/List;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPostEntityTrackerUpdate(final EntityTrackerUpdateS2CPacket packet, final CallbackInfo ci, final Entity entity) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_PROF ||
                AL.getUpdatedVillager() != null)
            return;
        if (!(entity instanceof final VillagerEntity vil))
            return;
        if (AL.getLecternPos().getSquaredDistance(vil.getPos()) > 8)
            return;
        if (!vil.getVillagerData().profession().matchesKey(VillagerProfession.LIBRARIAN))
            return;
        if (((ExtraVillagerData) vil).autolec$getPrevProfession().matchesKey(VillagerProfession.LIBRARIAN))
            return;
        AL.setUpdatedVillager(vil);
        AL.signal(AutoLectern.SIGNAL_PROF);
    }

    @Inject(method = "onItemPickupAnimation", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/ItemEntity;getStack()Lnet/minecraft/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onItemPickupAnimation(ItemPickupAnimationS2CPacket packet, CallbackInfo ci, Entity entity, LivingEntity livingEntity, ItemEntity itemEntity) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_ITEM) return;
        if (itemEntity.getStack().getItem() == Items.LECTERN)
            AL.signal(SIGNAL_ITEM);
    }

    @Unique
    private int checkTrades(final AutoLectern AL, final TradeOfferList offers, final ClientPlayerEntity plr) {
        int curIdx = 0;
        for (final var offer : offers) {
            final var sellItem = offer.getSellItem();
            if (sellItem.getItem() != Items.ENCHANTED_BOOK)
                continue;
            if (!EnchantmentHelper.canHaveEnchantments(sellItem))
                continue;
            final var enchantments = EnchantmentHelper.getEnchantments(sellItem);
            if (enchantments.isEmpty())
                continue;
            for (final var entry : enchantments.getEnchantmentEntries()) {
                final var enchant = entry.getKey();
                final var lvl = entry.getIntValue();
                if (AL.logTrade) {
                    plr.sendMessage(Text.literal("[Auto Lectern] ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal(String.valueOf(offer.getOriginalFirstBuyItem().getCount()))
                                            .formatted(Formatting.GREEN).append(Text.literal(" emeralds for ")
                                                    .formatted(Formatting.WHITE)
                                                    .append(Enchantment.getName(enchant, lvl)).append(Text.literal(" [" + AL.attempts + "]")
                                                            .formatted(Formatting.WHITE)
                                                    )
                                            )
                                    )
                            , false);
                }
                assert enchant.getKey().isPresent();
                final var goalMet = AL.getGoalMet(plr.getWorld(), offer.getOriginalFirstBuyItem().getCount(), enchant.getKey().get().getValue(), lvl);
                if (goalMet != -1) {
                    AL.setLastGoalMet(goalMet);
                    return curIdx;
                }
            }
            ++curIdx;
        }
        return -1;
    }

    @Unique
    private void attemptLockTrade(final AutoLectern AL, final TradeOfferList offers, final ClientPlayerEntity plr, int tarIdx) {
        final var emptySlot = plr.getInventory().getEmptySlot();
        if (emptySlot == -1)
            return;
        if (AL.autoTrade == ALAutoTrade.ENCHANT) {
            int availEmerald = 0;
            int availBook = 0;
            for (final var stack : plr.getInventory().getMainStacks()) {
                if (stack.isEmpty())
                    continue;
                final var itm = stack.getItem();
                if (itm == Items.EMERALD) availEmerald += stack.getCount();
                else if (itm == Items.BOOK) availBook += stack.getCount();
            }
            final var tarOffer = offers.get(tarIdx);
            final var first = tarOffer.getDisplayedFirstBuyItem();
            if (first.getItem() != Items.EMERALD)
                return;
            final var second = tarOffer.getDisplayedSecondBuyItem();
            if (second.getItem() != Items.BOOK)
                return;
            if (first.getCount() > availEmerald || second.getCount() > availBook)
                return;
            sendPacket(new SelectMerchantTradeC2SPacket(tarIdx));
            final var map = new Int2ObjectOpenHashMap<ItemStackHash>(0);
            sendPacket(new ClickSlotC2SPacket(merchantSyncId, 6, (short)2, (byte)0, SlotActionType.PICKUP, map, ItemStackHash.fromItemStack(tarOffer.getSellItem(), this.componentHasher)));
            sendPacket(new ClickSlotC2SPacket(merchantSyncId, 6, (short)emptySlot, (byte)0, SlotActionType.PICKUP, map, ItemStackHash.fromItemStack(ItemStack.EMPTY, this.componentHasher)));
        } else {
            final var minIdx = findCheapestTrade(offers, plr);
            if (minIdx == -1)
                return;
            sendPacket(new SelectMerchantTradeC2SPacket(minIdx));
            final var map = new Int2ObjectOpenHashMap<ItemStackHash>(0);
            sendPacket(new ClickSlotC2SPacket(merchantSyncId, 6, (short)2, (byte)0, SlotActionType.PICKUP, map, ItemStackHash.fromItemStack(offers.get(minIdx).getSellItem(), this.componentHasher)));
            sendPacket(new ClickSlotC2SPacket(merchantSyncId, 6, (short)emptySlot, (byte)0, SlotActionType.PICKUP, map, ItemStackHash.fromItemStack(ItemStack.EMPTY, this.componentHasher)));
        }

    }

    @Unique
    private void determineTrades(final TradeOfferList offers) {
        final var AL = AutoLectern.getInstance();
        final ClientPlayerEntity plr;
        if (AL.getState() != ALState.WAITING_TRADE || (plr = this.client.player) == null)
            return;
        ++AL.attempts;
        int tarIdx = checkTrades(AL, offers, plr);
        if (tarIdx == -1) {
            AL.signal(AutoLectern.SIGNAL_TRADE);
            return;
        }
        AL.signal(AutoLectern.SIGNAL_TRADE | AutoLectern.SIGNAL_TRADE_OK);
        if (AL.autoTrade != ALAutoTrade.OFF)
            attemptLockTrade(AL, offers, plr, tarIdx);
    }


    @Inject(method = "onSetTradeOffers", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/NetworkThreadUtils;forceMainThread(Lnet/minecraft/network/packet/Packet;Lnet/minecraft/network/listener/PacketListener;Lnet/minecraft/util/thread/ThreadExecutor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onSetTradeOffers(final SetTradeOffersS2CPacket packet, final CallbackInfo ci) {
        if (packet.getSyncId() != merchantSyncId) return;
        determineTrades(packet.getOffers());
        sendPacket(new CloseHandledScreenC2SPacket(merchantSyncId));
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
