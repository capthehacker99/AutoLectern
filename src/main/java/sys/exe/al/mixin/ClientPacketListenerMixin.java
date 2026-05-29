package sys.exe.al.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.StringReader;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.multiplayer.CommonListenerCookie;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.protocol.game.ClientboundCommandsPacket;
import net.minecraft.network.protocol.game.ClientboundContainerClosePacket;
import net.minecraft.network.protocol.game.ClientboundMerchantOffersPacket;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.protocol.game.ClientboundTakeItemEntityPacket;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.network.HashedPatchMap;
import net.minecraft.network.HashedStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;
import sys.exe.al.ALAutoTrade;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;
import sys.exe.al.commands.ClientCommandManager;
import sys.exe.al.interfaces.ExtraVillagerData;

import static sys.exe.al.AutoLectern.SIGNAL_ITEM;

@Mixin(ClientPacketListener.class)
public abstract class ClientPacketListenerMixin extends ClientCommonPacketListenerImpl {
    @Shadow
    private CommandDispatcher<ClientSuggestionProvider> commands;

    @Shadow
    @Final
    private HashedPatchMap.HashGenerator decoratedHashOpsGenerator;

    @Unique
    private int merchantSyncId = -1;

    @SuppressWarnings("unused")
    protected ClientPacketListenerMixin(final Minecraft client, final Connection connection, final CommonListenerCookie connectionState) {
        super(client, connection, connectionState);
    }

    @Unique
    private static int findCheapestTrade(final MerchantOffers offers, final LocalPlayer plr) {
        int availEmerald = 0;
        int availBook = 0;
        int availPaper = 0;
        for (final var stack : plr.getInventory().getNonEquipmentItems()) {
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
            final var first = offer.getCostA();
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

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(final Minecraft client, final Connection clientConnection, final CommonListenerCookie clientConnectionState, final CallbackInfo ci) {
        AutoLectern.registerCommands(commands, Commands.createValidationContext(VanillaRegistries.createLookup()));
    }

    @Inject(method = "handleOpenScreen", at = @At("HEAD"), cancellable = true)
    private void onOpenScreen(final ClientboundOpenScreenPacket packet, final CallbackInfo ci) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_TRADE) return;
        if (packet.getType() != MenuType.MERCHANT)
            return;
        merchantSyncId = packet.getContainerId();
        ci.cancel();
    }

    @Inject(method = "handleContainerClose", at = @At("HEAD"), cancellable = true)
    private void onCloseScreen(final ClientboundContainerClosePacket packet, final CallbackInfo ci) {
        if (packet.getContainerId() != merchantSyncId)
            return;
        merchantSyncId = -1;
        ci.cancel();
    }

    @Inject(method = "handleCommands", at = @At("TAIL"))
    private void onOnCommandTree(final ClientboundCommandsPacket packet, final CallbackInfo ci) {
        AutoLectern.registerCommands(commands, Commands.createValidationContext(VanillaRegistries.createLookup()));
    }

    @Inject(method = "handleSetEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/game/ClientboundSetEntityDataPacket;packedItems()Ljava/util/List;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPreEntityTrackerUpdate(final ClientboundSetEntityDataPacket packet, final CallbackInfo ci, final Entity entity) {
        if (entity instanceof final Villager vil)
            ((ExtraVillagerData) vil).autolec$setPrevProfession(vil.getVillagerData().profession());
    }

    @Inject(method = "handleSetEntityData", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/syncher/SynchedEntityData;assignValues(Ljava/util/List;)V", shift = At.Shift.AFTER), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onPostEntityTrackerUpdate(final ClientboundSetEntityDataPacket packet, final CallbackInfo ci, final Entity entity) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_PROF ||
                AL.getUpdatedVillager() != null)
            return;
        if (!(entity instanceof final Villager vil))
            return;
        if (AL.getLecternPos().distToCenterSqr(vil.position()) > 8)
            return;
        if (!vil.getVillagerData().profession().is(VillagerProfession.LIBRARIAN))
            return;
        if (((ExtraVillagerData) vil).autolec$getPrevProfession().is(VillagerProfession.LIBRARIAN))
            return;
        AL.setUpdatedVillager(vil);
        AL.signal(AutoLectern.SIGNAL_PROF);
    }

    @Inject(method = "handleTakeItemEntity", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/item/ItemEntity;getItem()Lnet/minecraft/world/item/ItemStack;"), locals = LocalCapture.CAPTURE_FAILSOFT)
    private void onItemPickupAnimation(final ClientboundTakeItemEntityPacket packet, final CallbackInfo ci, final Entity entity, final LivingEntity livingEntity, final EntityRenderState entityRenderState, final ItemEntity itemEntity) {
        final var AL = AutoLectern.getInstance();
        if (AL.getState() != ALState.WAITING_ITEM) return;
        if (itemEntity.getItem().getItem() == Items.LECTERN)
            AL.signal(SIGNAL_ITEM);
    }

    @Unique
    private int checkTrades(final AutoLectern AL, final MerchantOffers offers, final LocalPlayer plr) {
        for (int curIdx = 0; curIdx < offers.size(); ++curIdx) {
            final var offer = offers.get(curIdx);
            final var sellItem = offer.getResult();
            if (sellItem.getItem() != Items.ENCHANTED_BOOK)
                continue;
            if (!EnchantmentHelper.canStoreEnchantments(sellItem))
                continue;
            final var enchantments = EnchantmentHelper.getEnchantmentsForCrafting(sellItem);
            if (enchantments.isEmpty())
                continue;
            for (final var entry : enchantments.entrySet()) {
                final var enchant = entry.getKey();
                final var lvl = entry.getIntValue();
                if (AL.logTrade) {
                    plr.sendSystemMessage(Component.literal("[Auto Lectern] ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal(String.valueOf(offer.getBaseCostA().getCount()))
                                            .withStyle(ChatFormatting.GREEN).append(Component.literal(" emeralds for ")
                                                    .withStyle(ChatFormatting.WHITE)
                                                    .append(Enchantment.getFullname(enchant, lvl)).append(Component.literal(" [" + AL.attempts + "]")
                                                            .withStyle(ChatFormatting.WHITE)
                                                    )
                                            )
                                    )
                            );
                }
                assert enchant.unwrapKey().isPresent();
                final var goalMet = AL.getGoalMet(plr.level(), offer.getBaseCostA().getCount(), enchant.unwrapKey().get().identifier(), lvl);
                if (goalMet != -1) {
                    AL.setLastGoalMet(goalMet);
                    return curIdx;
                }
            }
        }
        return -1;
    }

    @Unique
    private void attemptLockTrade(final AutoLectern AL, final MerchantOffers offers, final LocalPlayer plr, int tarIdx) {
        final var emptySlot = plr.getInventory().getFreeSlot();
        if (emptySlot == -1)
            return;
        if (AL.autoTrade == ALAutoTrade.ENCHANT) {
            int availEmerald = 0;
            int availBook = 0;
            for (final var stack : plr.getInventory().getNonEquipmentItems()) {
                if (stack.isEmpty())
                    continue;
                final var itm = stack.getItem();
                if (itm == Items.EMERALD) availEmerald += stack.getCount();
                else if (itm == Items.BOOK) availBook += stack.getCount();
            }
            final var tarOffer = offers.get(tarIdx);
            final var first = tarOffer.getCostA();
            if (first.getItem() != Items.EMERALD)
                return;
            final var second = tarOffer.getCostB();
            if (second.getItem() != Items.BOOK)
                return;
            if (first.getCount() > availEmerald || second.getCount() > availBook)
                return;
            send(new ServerboundSelectTradePacket(tarIdx));
            final var map = new Int2ObjectOpenHashMap<HashedStack>(0);
            send(new ServerboundContainerClickPacket(merchantSyncId, 6, (short)2, (byte)0, ContainerInput.PICKUP, map, HashedStack.create(tarOffer.getResult(), this.decoratedHashOpsGenerator)));
            send(new ServerboundContainerClickPacket(merchantSyncId, 6, (short)emptySlot, (byte)0, ContainerInput.PICKUP, map, HashedStack.create(ItemStack.EMPTY, this.decoratedHashOpsGenerator)));
        } else {
            final var minIdx = findCheapestTrade(offers, plr);
            if (minIdx == -1)
                return;
            send(new ServerboundSelectTradePacket(minIdx));
            final var map = new Int2ObjectOpenHashMap<HashedStack>(0);
            send(new ServerboundContainerClickPacket(merchantSyncId, 6, (short)2, (byte)0, ContainerInput.PICKUP, map, HashedStack.create(offers.get(minIdx).getResult(), this.decoratedHashOpsGenerator)));
            send(new ServerboundContainerClickPacket(merchantSyncId, 6, (short)emptySlot, (byte)0, ContainerInput.PICKUP, map, HashedStack.create(ItemStack.EMPTY, this.decoratedHashOpsGenerator)));
        }

    }

    @Unique
    private void determineTrades(final MerchantOffers offers) {
        final var AL = AutoLectern.getInstance();
        final LocalPlayer plr;
        if (AL.getState() != ALState.WAITING_TRADE || (plr = this.minecraft.player) == null)
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


    @Inject(method = "handleMerchantOffers", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/protocol/PacketUtils;ensureRunningOnSameThread(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/network/PacketListener;Lnet/minecraft/network/PacketProcessor;)V", shift = At.Shift.AFTER), cancellable = true)
    private void onSetTradeOffers(final ClientboundMerchantOffersPacket packet, final CallbackInfo ci) {
        if (packet.getContainerId() != merchantSyncId) return;
        determineTrades(packet.getOffers());
        send(new ServerboundContainerClosePacket(merchantSyncId));
        ci.cancel();
    }

    @Inject(method = "sendCommand(Ljava/lang/String;)V", at = @At("HEAD"), cancellable = true)
    private void onSendChatCommand(final String message, final CallbackInfo ci) {
        final var reader = new StringReader(message);
        final var commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(minecraft, reader, message);
            ci.cancel();
        }
    }

    @Inject(method = "sendUnattendedCommand", at = @At("HEAD"), cancellable = true)
    private void sendCommand(final String command, final Screen afterActionScreen, final CallbackInfo ci) {
        final var reader = new StringReader(command);
        final var commandName = reader.canRead() ? reader.readUnquotedString() : "";
        reader.setCursor(0);
        if (ClientCommandManager.isClientSideCommand(commandName)) {
            ClientCommandManager.executeCommand(minecraft, reader, command);
            ci.cancel();
        }
    }
}
