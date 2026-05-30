package sys.exe.al;


import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.KeyboardInput;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.ChatFormatting;
import net.minecraft.world.InteractionHand;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sys.exe.al.commands.AutoLec;
import sys.exe.al.commands.ClientCommandManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Objects;

public class AutoLectern implements ClientModInitializer {
    private static AutoLectern INSTANCE;
    public static AutoLectern getInstance() {
        return INSTANCE;
    }
    public static final Logger LOGGER = LoggerFactory.getLogger("Auto Lectern");
    public ALAutoTrade autoTrade;
    public boolean itemSync;
    public boolean breakCooldown;
    public boolean logTrade;
    public boolean preBreaking;
    public boolean preserveTool;
    public boolean autoRemove;
    public int attempts;
    private int UUID;
    private int signals;
    private int lastGoalMet;
    private int prevSelectedSlot;
    private BlockPos lecternPos;
    private Direction lecternSide;
    private ALState curState;
    private int tickCoolDown;
    private Villager updatedVillager;
    private ArrayList<ALGoal> goals;
    private File configFile;

    private float lecPitch;
    private float lecYaw;
    private float fakePitch;
    private float fakeYaw;
    private Vec3 forcedPos;
    public static final int SIGNAL_PROF = 1;
    public static final int SIGNAL_TRADE = 1 << 1;
    public static final int SIGNAL_TRADE_OK = 1 << 2;
    public static final int SIGNAL_ITEM = 1 << 3;
    public void signal(final int signalType) {
        signals |= signalType;
    }

    public int getUUID() {
        return UUID;
    }

    public void incrementUUID() {
        ++UUID;
    }
    public ArrayList<ALGoal> getGoals() {
        return goals;
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    public static int getMostExpensiveVillagerEnchant(final Holder<@NotNull Enchantment> enchant) {
        final var k = enchant.value().getMaxLevel();
        int minPrice = (k * 3) + 2 + (4 + k * 10);
        if (enchant.is(EnchantmentTags.DOUBLE_TRADE_PRICE))
            minPrice *= 2;
        return (minPrice > 64) ? 64 : minPrice;
    }
    @SuppressWarnings("ManualMinMaxCalculation")
    public static int getCheapestVillagerEnchant(final Holder<@NotNull Enchantment> enchant) {
        int minPrice = (enchant.value().getMaxLevel() * 3) + 2;
        if (enchant.is(EnchantmentTags.DOUBLE_TRADE_PRICE))
            minPrice *= 2;
        return (minPrice > 64) ? 64 : minPrice;
    }

    private boolean isGoalLevelMet(final int maxEncLvl, final int lvlMin, final int lvlMax, final int lvl) {
        if(lvlMin == -1) {
            if(lvlMax == -1)
                return true;
            return lvl <= 1;
        }
        if(lvlMax == -1)
            return lvl >= maxEncLvl;
        return lvl >= lvlMin && lvl <= lvlMax;
    }

    private boolean isGoalPriceMet(final Holder<@NotNull Enchantment> enchant, final int priceMin, final int priceMax, final int price) {
        if(priceMin == -1) {
            if(priceMax == -1)
                return true;
            return price <= getCheapestVillagerEnchant(enchant);
        }
        if(priceMax == -1)
            return price >= getMostExpensiveVillagerEnchant(enchant);
        return price >= priceMin && price <= priceMax;
    }

    public static @Nullable Holder<@NotNull Enchantment> enchantFromIdentifier(final Level world, final Identifier id) {
        final var enchants = world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        final var enchant = enchants.getValue(id);
        if(enchant == null)
            return null;
        return enchants.wrapAsHolder(enchant);
    }
    public int getGoalMet(final Level world, final int price, final Identifier enchant, final int lvl) {
        int idx = 0;
        for(final var curGoal : goals) {
            final var enc_id = curGoal.enchant();
            if(enc_id == null)
                continue;
            final var enc = enchantFromIdentifier(world, enc_id);
            if(enc == null)
                continue;
            if (enchant.equals(enc_id) &&
                isGoalLevelMet(
                    enc.value().getMaxLevel(),
                    curGoal.lvlMin(),
                    curGoal.lvlMax(),
                    lvl
                ) && isGoalPriceMet(
                    enc,
                    curGoal.priceMin(),
                    curGoal.priceMax(),
                    price
                ))
                return idx;
            ++idx;
        }
        return -1;
    }
    public final Villager getUpdatedVillager() {
        return updatedVillager;
    }

    public void setUpdatedVillager(final Villager updatedVillager) {
        this.updatedVillager = updatedVillager;
    }

    public final ALState getState() {
        return curState;
    }

    public void setState(final ALState newState) {
        curState = newState;
    }

    public BlockPos getLecternPos() {
        return lecternPos;
    }

    private static boolean toolNearBreak(final ItemStack tool) {
        return tool.isDamageableItem() && tool.getDamageValue() + 2 >= tool.getMaxDamage();
    }

    private boolean equipWorkingTool(final @NotNull LocalPlayer plr) {
        final var inventory = plr.getInventory();
        for(int i = 0;i < 9;++i) {
            final var stack = inventory.getItem(i);
            if(!(stack.getItem() instanceof AxeItem))
                continue;
            if(toolNearBreak(stack))
                continue;
            inventory.setSelectedSlot(i);
            return true;
        }
        return false;
    }

    private boolean checkPreserveTool(final @NotNull LocalPlayer plr) {
        final var tool = plr.getMainHandItem();
        if(!toolNearBreak(tool))
            return false;
        if(equipWorkingTool(plr))
            return false;
        curState = ALState.STOPPING;
        return true;
    }

    @Nullable
    private BlockHitResult getLookingAt(final LocalPlayer plr) {
        final var oldPitch = plr.getXRot();
        final var oldYaw = plr.getYRot();
        plr.setXRot(lecPitch);
        plr.setYRot(lecYaw);
        final var hitResult = plr.pick(4.5f, 0, false);
        plr.setXRot(oldPitch);
        plr.setYRot(oldYaw);
        if(hitResult.getType() != HitResult.Type.BLOCK)
            return null;
        return (BlockHitResult) hitResult;
    }

    @Nullable
    private InteractionHand equipLectern(final LocalPlayer plr) {
        if(plr.getOffhandItem().is(Items.LECTERN))
            return InteractionHand.OFF_HAND;
        if(plr.getMainHandItem().is(Items.LECTERN))
            return InteractionHand.MAIN_HAND;
        final var plrInv = plr.getInventory();
        int idx = 0;
        for(final var itmStk : plrInv.getNonEquipmentItems()) {
            if(itmStk.getItem() != Items.LECTERN) {
                ++idx;
                continue;
            }
            if(!Inventory.isHotbarSlot(idx))
                break;
            prevSelectedSlot = plrInv.getSelectedSlot();
            plrInv.setSelectedSlot(idx);
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    private void preBreak(final LocalPlayer plr, @Nullable final MultiPlayerGameMode interactionManager, final ClientLevel world) {
        if(prevSelectedSlot != -1) {
            plr.getInventory().setSelectedSlot(prevSelectedSlot);
            prevSelectedSlot = -1;
        }
        if(preserveTool)
            checkPreserveTool(plr);
        else if(plr.getMainHandItem().isEmpty())
            equipWorkingTool(plr);
        world.addBreakingBlockEffect(lecternPos, lecternSide);
        if(interactionManager == null)
            return;
        if(interactionManager.isDestroying())
            interactionManager.continueDestroyBlock(lecternPos, lecternSide);
        else
            interactionManager.startDestroyBlock(lecternPos, lecternSide);
        plr.swing(InteractionHand.MAIN_HAND);
    }
    public void MinecraftTickHead(final Minecraft mc) {
        if(curState == ALState.STOPPED)
            return;
        final var plr = mc.player;
        if(plr == null || !(plr.containerMenu instanceof InventoryMenu))
            curState = ALState.STOPPING;
        while(true) {
            switch (curState) {
                case STOPPING -> {
                    final ClientLevel world;
                    if(plr != null && (world = mc.level) != null) {
                        if(this.lecternPos != null) {
                            plr.connection.send(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, lecternPos, lecternSide));
                            world.destroyBlockProgress(plr.getId(), lecternPos, -1);
                        }
                        plr.input = new KeyboardInput(mc.options);
                    }
                    forcedPos = null;
                    prevSelectedSlot = -1;
                    signals = 0;
                    lecternPos = null;
                    lecternSide = null;
                    updatedVillager = null;
                    mc.gui.getChat().addClientSystemMessage(
                            Component.literal("[Auto Lectern] ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal("Stopped.")
                                            .withStyle(ChatFormatting.RED)
                                    )
                    );
                    curState = ALState.STOPPED;
                    return;
                }
                case STARTING -> {
                    attempts = 0;
                    prevSelectedSlot = -1;
                    signals = 0;
                    updatedVillager = null;
                    final ClientLevel world;
                    if ((world = mc.level) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    final var crosshairTarget = mc.hitResult;
                    if(!(crosshairTarget instanceof final BlockHitResult blockHitResult) ||
                            world.getBlockState(blockHitResult.getBlockPos()).getBlock() != Blocks.LECTERN){
                        mc.gui.getChat().addClientSystemMessage(Component.literal("[Auto Lectern] ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(
                                        Component.literal("Please look at a lectern before running this command.")
                                                .withStyle(ChatFormatting.RED)
                                )
                        );
                        curState = ALState.STOPPING;
                        continue;
                    }
                    plr.input = new DummyInput();
                    lecPitch = plr.getXRot();
                    lecYaw = plr.getYRot();
                    fakePitch = lecPitch;
                    fakeYaw = lecYaw;
                    forcedPos = plr.position();
                    lecternPos = blockHitResult.getBlockPos();
                    lecternSide = blockHitResult.getDirection();
                    curState = ALState.BREAKING;
                }
                case BREAKING -> {
                    final ClientLevel world;
                    final MultiPlayerGameMode interactionManager;
                    if ((world = mc.level) == null ||
                            (interactionManager = mc.gameMode) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    if(world.getBlockState(lecternPos).canBeReplaced()) {
                        curState = itemSync ? ALState.WAITING_ITEM : ALState.PLACING;
                        continue;
                    }
                    fakePitch = lecPitch;
                    fakeYaw = lecYaw;
                    plr.move(MoverType.SELF, new Vec3(forcedPos.x()-plr.getX(), -0.00001, forcedPos.z()-plr.getZ()));
                    if(prevSelectedSlot != -1) {
                        plr.getInventory().setSelectedSlot(prevSelectedSlot);
                        prevSelectedSlot = -1;
                    }
                    if(preserveTool) {
                        if (checkPreserveTool(plr))
                            continue;
                    } else if(plr.getMainHandItem().isEmpty())
                        equipWorkingTool(plr);
                    world.addBreakingBlockEffect(lecternPos, lecternSide);
                    if(interactionManager.isDestroying())
                        interactionManager.continueDestroyBlock(lecternPos, lecternSide);
                    else
                        interactionManager.startDestroyBlock(lecternPos, lecternSide);
                    plr.swing(InteractionHand.MAIN_HAND);
                    return;
                }
                case WAITING_ITEM -> {
                    plr.move(MoverType.SELF, new Vec3(forcedPos.x()-plr.getX(), -0.00001, forcedPos.z()-plr.getZ()));
                    if((signals & SIGNAL_ITEM) != 0) {
                        curState = ALState.PLACING;
                        continue;
                    }
                    return;
                }
                case PLACING -> {
                    final ClientLevel world;
                    if ((world = mc.level) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    fakePitch = lecPitch;
                    fakeYaw = lecYaw;
                    if(world.getBlockState(lecternPos).is(Blocks.LECTERN)) {
                        updatedVillager = null;
                        tickCoolDown = 40;
                        signals = 0;
                        curState = ALState.WAITING_PROF;
                    }
                    final MultiPlayerGameMode interactionManager;
                    if((interactionManager = mc.gameMode) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    final BlockHitResult blockHitResult;
                    if((blockHitResult = getLookingAt(plr)) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    final var lecternHand = equipLectern(plr);
                    if(lecternHand != null) {
                        final var actionResult = interactionManager.useItemOn(plr, lecternHand, blockHitResult);
                        if(actionResult instanceof InteractionResult.Success successActionResult &&
                                successActionResult.swingSource() == InteractionResult.SwingSource.CLIENT)
                            plr.swing(lecternHand);
                    }
                    if(!world.getBlockState(lecternPos).is(Blocks.LECTERN))
                        return;
                    updatedVillager = null;
                    tickCoolDown = 40;
                    signals = 0;
                    curState = ALState.WAITING_PROF;
                }
                case WAITING_PROF -> {
                    if((signals & SIGNAL_PROF) == 0) {
                        plr.move(MoverType.SELF, new Vec3(forcedPos.x()-plr.getX(), -0.00001, forcedPos.z()-plr.getZ()));
                        final var world = mc.level;
                        if(world == null) {
                            curState = ALState.STOPPING;
                            continue;
                        }
                        final var blkState = world.getBlockState(lecternPos);
                        if(blkState.isAir()) {
                            curState = ALState.PLACING;
                            continue;
                        }
                        if(blkState.getBlock() != Blocks.LECTERN) {
                            curState = ALState.BREAKING;
                            continue;
                        }
                        if(tickCoolDown > 0) {
                            if(preBreaking)
                                preBreak(plr, mc.gameMode, mc.level);
                            --tickCoolDown;
                            return;
                        }
                        curState = ALState.BREAKING;
                        continue;
                    }
                    curState = ALState.INTERACT_VIL;
                }
                case INTERACT_VIL -> {
                    if(updatedVillager == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    final var interactionManager = mc.gameMode;
                    if(interactionManager == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    tickCoolDown = 5;
                    signals = 0;
                    curState = ALState.WAITING_TRADE;
                    final var villagePos = updatedVillager.position();
                    final var eyePos = plr.getEyePosition();
                    final var box = plr.getBoundingBox().inflate(20.0, 20.0, 20.0);
                    final var hitResult = ProjectileUtil.getEntityHitResult(plr, eyePos, villagePos, box, x -> x.equals(updatedVillager), 20);
                    final var delta_pos = villagePos.subtract(eyePos);
                    fakeYaw = (float) ((Math.toDegrees(Math.atan2(delta_pos.z, delta_pos.x)) - 90) % 360);
                    double sqrt = Math.sqrt(delta_pos.x * delta_pos.x + delta_pos.z * delta_pos.z);
                    fakePitch = (float) -Math.toDegrees(Math.atan2(delta_pos.y, sqrt));
                    InteractionResult actionResult = interactionManager.interact(plr, updatedVillager, hitResult != null ? hitResult : new EntityHitResult(updatedVillager, villagePos), InteractionHand.MAIN_HAND);
                    if(actionResult instanceof InteractionResult.Success successActionResult &&
                            successActionResult.swingSource() == InteractionResult.SwingSource.CLIENT)
                        plr.swing(InteractionHand.MAIN_HAND);
                }
                case WAITING_TRADE -> {
                    if((signals & SIGNAL_TRADE) != 0) {
                        if((signals & SIGNAL_TRADE_OK) == 0) {
                            curState = ALState.BREAKING;
                            continue;
                        }
                        mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1));
                        GLFW.glfwRequestWindowAttention(mc.getWindow().handle());
                        final var goal = goals.get(lastGoalMet);
                        assert mc.level != null;
                        final MutableComponent message = Component.literal("[Auto Lectern] ")
                                .withStyle(ChatFormatting.YELLOW)
                                .append(
                                        Component.literal("Goal met: ")
                                                .withStyle(ChatFormatting.WHITE)
                                ).append(Objects.requireNonNull(enchantFromIdentifier(mc.level, goal.enchant()))
                                .value()
                                .description()
                                .copy()
                                .withStyle(ChatFormatting.GRAY));
                        if (!autoRemove) {
                            message.append(Component.literal(" [REMOVE]")
                                    .setStyle(Style.EMPTY
                                            .withClickEvent(new ClickEvent.RunCommand(
                                                    "/autolec remove " + lastGoalMet + " " + getUUID()
                                            ))
                                    ).withStyle(ChatFormatting.RED)
                            );
                        } else {
                            final var lastElemIdx = goals.size() - 1;
                            goals.set(lastGoalMet, goals.get(lastElemIdx));
                            goals.remove(lastElemIdx);
                            incrementUUID();
                            message.append(Component.literal(" [AUTO REMOVED]").withStyle(ChatFormatting.RED));
                        }

                        mc.gui.getChat().addClientSystemMessage(message);
                        mc.gui.getChat().addClientSystemMessage(
                                Component.literal("[Auto Lectern] ")
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(
                                                Component.literal("Completed.")
                                                        .withStyle(ChatFormatting.GREEN)
                                        )
                        );
                        curState = ALState.STOPPING;
                        continue;
                    }
                    plr.move(MoverType.SELF, new Vec3(forcedPos.x()-plr.getX(), -0.00001, forcedPos.z()-plr.getZ()));
                    if(tickCoolDown > 0) {
                        if(preBreaking)
                            preBreak(plr, mc.gameMode, mc.level);
                        --tickCoolDown;
                        return;
                    }
                    curState = ALState.INTERACT_VIL;
                }
            }
        }
    }

    public static void registerCommands(CommandDispatcher<ClientSuggestionProvider> dispatcher, CommandBuildContext registryAccess) {
        ClientCommandManager.clearClientSideCommands();
        AutoLec.register(dispatcher, registryAccess);
    }


    public void saveConfig(){
        LOGGER.info("Saving config...");
        try (final PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(configFile), StandardCharsets.UTF_8))){
            pw.write("itemSync=");
            pw.write(itemSync ? "true\n" : "false\n");
            pw.write("breakCooldown=");
            pw.write(breakCooldown ? "true\n" : "false\n");
            pw.write("logTrade=");
            pw.write(logTrade ? "true\n" : "false\n");
            pw.write("preBreak=");
            pw.write(preBreaking ? "true\n" : "false\n");
            pw.write("preserveTool=");
            pw.write(preserveTool ? "true\n" : "false\n");
            pw.write("autoRemove=");
            pw.write(autoRemove ? "true\n" : "false\n");
            pw.write("autoTrade=");
            pw.write(autoTrade.name());
            pw.write('\n');
            pw.write("goals=");
            for(final var goal : goals) {
                pw.write(goal.enchant().toString());
                pw.write(',');
                pw.print(goal.lvlMin());
                pw.write(',');
                pw.print(goal.lvlMax());
                pw.write(',');
                pw.print(goal.priceMin());
                pw.write(',');
                pw.print(goal.priceMax());
                pw.write(';');
            }
        } catch (final Exception exception) {
            LOGGER.error("Failed to save config.", exception);
            return;
        }
        LOGGER.info("Config saved!");
    }
    @Override
    public void onInitializeClient() {
        LOGGER.info("Loading...");
        configFile = FabricLoader.getInstance().getConfigDir().resolve("autolec.txt").toFile();
        // Defaults
        breakCooldown = false;
        itemSync = false;
        preserveTool = true;
        logTrade = false;
        preBreaking = true;
        autoRemove = false;
        autoTrade = ALAutoTrade.OFF;
        goals = new ArrayList<>();
        // Load config
        try {
            try (final var bufferedReader = Files.newReader(configFile, StandardCharsets.UTF_8)) {
                final var EQUAL_SPLITTER = Splitter.on('=').limit(2);
                final var lineIt = bufferedReader.lines().iterator();
                while(lineIt.hasNext()) {
                    final var line = lineIt.next();
                    try {
                        final var eIt = EQUAL_SPLITTER.split(line).iterator();
                        final var key = eIt.next();
                        final var value = eIt.next();
                        switch(key) {
                            case "breakCooldown" -> breakCooldown = (value.equals("true"));
                            case "itemSync" -> itemSync = (value.equals("true"));
                            case "preserveTool" -> preserveTool = (value.equals("true"));
                            case "logTrade" -> logTrade = (value.equals("true"));
                            case "preBreak" -> preBreaking = (value.equals("true"));
                            case "autoRemove" -> autoRemove = (value.equals("true"));
                            case "autoTrade" -> autoTrade = value.equals("ENCHANT") ? ALAutoTrade.ENCHANT : (value.equals("CHEAPEST") ? ALAutoTrade.CHEAPEST : ALAutoTrade.OFF);
                            case "goals" -> {
                                final var gIt = Splitter.on(';').split(value).iterator();
                                final var COMMA_SPLITTER = Splitter.on(',');
                                while (gIt.hasNext()) {
                                    final var goalData = gIt.next();
                                    if(goalData.isEmpty())
                                        continue;
                                    final var gdIt = COMMA_SPLITTER.split(goalData).iterator();
                                    goals.add(new ALGoal(Identifier.parse(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next())));
                                }
                            }
                        }
                    } catch (Exception exception) {
                        LOGGER.warn("Skipping bad option: {}", line);
                    }
                }
            }
        } catch(final Exception e) {
            LOGGER.info("Failed to load config.");
        }
        INSTANCE = this;
        UUID = 0;
        curState = ALState.STOPPED;
        LOGGER.info("Loaded!");
    }

    public float getPitch() {
        return fakePitch;
    }

    public float getYaw() {
        return fakeYaw;
    }

    public void setLastGoalMet(final int lastGoalMet) {
        this.lastGoalMet = lastGoalMet;
    }
}
