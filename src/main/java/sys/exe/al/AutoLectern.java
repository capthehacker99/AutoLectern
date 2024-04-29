package sys.exe.al;


import com.google.common.base.Charsets;
import com.google.common.base.Splitter;
import com.google.common.io.Files;
import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.AxeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sys.exe.al.commands.AutoLec;
import sys.exe.al.commands.ClientCommandManager;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;

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
    public int attempts;
    private int UUID;
    private int signals;
    private int lastGoalMet;
    private int prevSelectedSlot;
    private BlockPos lecternPos;
    private Direction lecternSide;
    private ALState curState;
    private int tickCoolDown;
    private VillagerEntity updatedVillager;
    private ArrayList<ALGoal> goals;
    private File configFile;

    private float fakePitch;
    private float fakeYaw;
    private Vec3d forcedPos;
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
    public static int getMostExpensiveVillagerEnchant(final Enchantment enchant) {
        final var k = enchant.getMaxLevel();
        int minPrice = (k * 3) + 2 + (4 + k * 10);
        if (enchant.isTreasure())
            minPrice *= 2;
        return (minPrice > 64) ? 64 : minPrice;
    }
    @SuppressWarnings("ManualMinMaxCalculation")
    public static int getCheapestVillagerEnchant(final Enchantment enchant) {
        int minPrice = (enchant.getMaxLevel() * 3) + 2;
        if (enchant.isTreasure())
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

    private boolean isGoalPriceMet(final Enchantment enchant, final int priceMin, final int priceMax, final int price) {
        if(priceMin == -1) {
            if(priceMax == -1)
                return true;
            return price <= getCheapestVillagerEnchant(enchant);
        }
        if(priceMax == -1)
            return price >= getMostExpensiveVillagerEnchant(enchant);
        return price >= priceMin && price <= priceMax;
    }
    public int getGoalMet(final int price, final Enchantment enchant, final int lvl) {
        int idx = 0;
        for(final var curGoal : goals) {
            final var enc = curGoal.enchant();
            if (enc == enchant &&
                isGoalLevelMet(
                    enc.getMaxLevel(),
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
    public final VillagerEntity getUpdatedVillager() {
        return updatedVillager;
    }

    public void setUpdatedVillager(final VillagerEntity updatedVillager) {
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
        return tool.isDamageable() && tool.getDamage() + 2 >= tool.getMaxDamage();
    }

    private boolean equipWorkingTool(final @NotNull ClientPlayerEntity plr) {
        final var inventory = plr.getInventory();
        for(int i = 0;i < 9;++i) {
            final var stack = inventory.getStack(i);
            if(!(stack.getItem() instanceof AxeItem))
                continue;
            if(toolNearBreak(stack))
                continue;
            inventory.selectedSlot = i;
            return true;
        }
        return false;
    }

    private boolean checkPreserveTool(final @NotNull ClientPlayerEntity plr) {
        final var tool = plr.getMainHandStack();
        if(!toolNearBreak(tool))
            return false;
        if(equipWorkingTool(plr))
            return false;
        curState = ALState.STOPPING;
        return true;
    }

    @Nullable
    private BlockHitResult getLookingAt(final ClientPlayerEntity plr) {
        final var oldPitch = plr.getPitch();
        final var oldYaw = plr.getYaw();
        plr.setPitch(fakePitch);
        plr.setYaw(fakeYaw);
        final var hitResult = plr.raycast(4.5f, 0, false);
        plr.setPitch(oldPitch);
        plr.setYaw(oldYaw);
        if(hitResult.getType() != HitResult.Type.BLOCK)
            return null;
        return (BlockHitResult) hitResult;
    }

    @Nullable
    private Hand equipLectern(final ClientPlayerEntity plr) {
        if(plr.getOffHandStack().isOf(Items.LECTERN))
            return Hand.OFF_HAND;
        if(plr.getMainHandStack().isOf(Items.LECTERN))
            return Hand.MAIN_HAND;
        final var plrInv = plr.getInventory();
        int idx = 0;
        for(final var itmStk : plrInv.main) {
            if(itmStk.getItem() != Items.LECTERN) {
                ++idx;
                continue;
            }
            if(!PlayerInventory.isValidHotbarIndex(idx))
                break;
            prevSelectedSlot = plrInv.selectedSlot;
            plrInv.selectedSlot = idx;
            return Hand.MAIN_HAND;
        }
        return null;
    }

    private void preBreak(final ClientPlayerEntity plr, @Nullable final ClientPlayerInteractionManager interactionManager, @Nullable final ParticleManager partMan) {
        if(prevSelectedSlot != -1) {
            plr.getInventory().selectedSlot = prevSelectedSlot;
            prevSelectedSlot = -1;
        }
        if(preserveTool)
            checkPreserveTool(plr);
        else if(plr.getMainHandStack().isEmpty())
            equipWorkingTool(plr);
        if(partMan != null)
            partMan.addBlockBreakingParticles(lecternPos, lecternSide);
        if(interactionManager == null)
            return;
        interactionManager.updateBlockBreakingProgress(lecternPos, lecternSide);
        plr.swingHand(Hand.MAIN_HAND);
    }
    public void MinecraftTickHead(final MinecraftClient mc) {
        if(curState == ALState.STOPPED)
            return;
        final var plr = mc.player;
        if(plr == null || !(plr.currentScreenHandler instanceof PlayerScreenHandler))
            curState = ALState.STOPPING;
        while(true) {
            switch (curState) {
                case STOPPING -> {
                    final ClientWorld world;
                    if(plr != null && (world = mc.world) != null) {
                        if(this.lecternPos != null) {
                            plr.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, lecternPos, lecternSide));
                            world.setBlockBreakingInfo(plr.getId(), lecternPos, -1);
                        }
                        plr.input = new KeyboardInput(mc.options);
                    }
                    forcedPos = null;
                    prevSelectedSlot = -1;
                    signals = 0;
                    lecternPos = null;
                    lecternSide = null;
                    updatedVillager = null;
                    mc.inGameHud.getChatHud().addMessage(
                            Text.literal("[Auto Lectern] ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal("Stopped.")
                                            .formatted(Formatting.RED)
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
                    final ClientWorld world;
                    if ((world = mc.world) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    final var crosshairTarget = mc.crosshairTarget;
                    if(!(crosshairTarget instanceof final BlockHitResult blockHitResult) ||
                            world.getBlockState(blockHitResult.getBlockPos()).getBlock() != Blocks.LECTERN){
                        mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ")
                                .formatted(Formatting.YELLOW)
                                .append(
                                        Text.literal("Please look at a lectern before running this command.")
                                                .formatted(Formatting.RED)
                                )
                        );
                        curState = ALState.STOPPING;
                        continue;
                    }
                    plr.input = new DummyInput();
                    fakePitch = plr.getPitch();
                    fakeYaw = plr.getYaw();
                    forcedPos = plr.getPos();
                    lecternPos = blockHitResult.getBlockPos();
                    lecternSide = blockHitResult.getSide();
                    curState = ALState.BREAKING;
                }
                case BREAKING -> {
                    final ClientWorld world;
                    final ClientPlayerInteractionManager interactionManager;
                    if ((world = mc.world) == null ||
                            (interactionManager = mc.interactionManager) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    plr.move(MovementType.SELF, new Vec3d(forcedPos.getX()-plr.getX(), -0.00001, forcedPos.getZ()-plr.getZ()));
                    if(prevSelectedSlot != -1) {
                        plr.getInventory().selectedSlot = prevSelectedSlot;
                        prevSelectedSlot = -1;
                    }
                    if(preserveTool) {
                        if (checkPreserveTool(plr))
                            continue;
                    } else if(plr.getMainHandStack().isEmpty())
                        equipWorkingTool(plr);
                    final var partMan = mc.particleManager;
                    if(partMan != null)
                        partMan.addBlockBreakingParticles(lecternPos, lecternSide);
                    interactionManager.updateBlockBreakingProgress(lecternPos, lecternSide);
                    plr.swingHand(Hand.MAIN_HAND);
                    if(world.getBlockState(lecternPos).isAir()) {
                        curState = itemSync ? ALState.WAITING_ITEM : ALState.PLACING;
                        return; //Take a break. ;)
                    }
                    return;
                }
                case WAITING_ITEM -> {
                    plr.move(MovementType.SELF, new Vec3d(forcedPos.getX()-plr.getX(), -0.00001, forcedPos.getZ()-plr.getZ()));
                    if((signals & SIGNAL_ITEM) != 0) {
                        curState = ALState.PLACING;
                        continue;
                    }
                    return;
                }
                case PLACING -> {
                    final ClientWorld world;
                    if ((world = mc.world) == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    if(world.getBlockState(lecternPos).isOf(Blocks.LECTERN)) {
                        updatedVillager = null;
                        tickCoolDown = 40;
                        signals = 0;
                        curState = ALState.WAITING_PROF;
                    }
                    final ClientPlayerInteractionManager interactionManager;
                    if((interactionManager = mc.interactionManager) == null) {
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
                        final var actionResult = interactionManager.interactBlock(plr, lecternHand, blockHitResult);
                        if(actionResult.isAccepted() && actionResult.shouldSwingHand())
                            plr.swingHand(lecternHand);
                    }
                    if(!world.getBlockState(lecternPos).isOf(Blocks.LECTERN))
                        return;
                    updatedVillager = null;
                    tickCoolDown = 40;
                    signals = 0;
                    curState = ALState.WAITING_PROF;
                }
                case WAITING_PROF -> {
                    if((signals & SIGNAL_PROF) == 0) {
                        plr.move(MovementType.SELF, new Vec3d(forcedPos.getX()-plr.getX(), -0.00001, forcedPos.getZ()-plr.getZ()));
                        final var world = mc.world;
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
                                preBreak(plr, mc.interactionManager, mc.particleManager);
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
                    final var interactionManager = mc.interactionManager;
                    if(interactionManager == null) {
                        curState = ALState.STOPPING;
                        continue;
                    }
                    tickCoolDown = 5;
                    signals = 0;
                    curState = ALState.WAITING_TRADE;
                    final var actionResult = interactionManager.interactEntity(plr, updatedVillager, Hand.MAIN_HAND);
                    if(actionResult.isAccepted() && actionResult.shouldSwingHand())
                        plr.swingHand(Hand.MAIN_HAND);
                }
                case WAITING_TRADE -> {
                    if((signals & SIGNAL_TRADE) != 0) {
                        if((signals & SIGNAL_TRADE_OK) == 0) {
                            curState = ALState.BREAKING;
                            continue;
                        }
                        mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1));
                        GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
                        final var goal = goals.get(lastGoalMet);
                        mc.inGameHud.getChatHud().addMessage(
                                Text.literal("[Auto Lectern] ")
                                        .formatted(Formatting.YELLOW)
                                        .append(
                                                Text.literal("Goal met: ")
                                                        .formatted(Formatting.WHITE)
                                        ).append(
                                                Text.translatable(
                                                    goal.enchant().getTranslationKey()
                                                ).formatted(goal.enchant().isCursed() ? Formatting.RED : Formatting.GRAY).append(Text.literal(" [REMOVE]")
                                                    .setStyle(Style.EMPTY
                                                        .withClickEvent(new ClickEvent(
                                                                ClickEvent.Action.RUN_COMMAND,
                                                                "/autolec remove " + lastGoalMet + " " + getUUID()
                                                        ))
                                                    ).formatted(Formatting.RED)
                                                )
                                        )
                        );
                        mc.inGameHud.getChatHud().addMessage(
                                Text.literal("[Auto Lectern] ")
                                        .formatted(Formatting.YELLOW)
                                        .append(
                                                Text.literal("Completed.")
                                                        .formatted(Formatting.GREEN)
                                        )
                        );
                        curState = ALState.STOPPING;
                        continue;
                    }
                    plr.move(MovementType.SELF, new Vec3d(forcedPos.getX()-plr.getX(), -0.00001, forcedPos.getZ()-plr.getZ()));
                    if(tickCoolDown > 0) {
                        if(preBreaking)
                            preBreak(plr, mc.interactionManager, mc.particleManager);
                        --tickCoolDown;
                        return;
                    }
                    curState = ALState.INTERACT_VIL;
                }
            }
        }
    }

    public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
        ClientCommandManager.clearClientSideCommands();
        AutoLec.register(dispatcher);
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
            pw.write("preBreaking=");
            pw.write(preBreaking ? "true\n" : "false\n");
            pw.write("preserveTool=");
            pw.write(preserveTool ? "true\n" : "false\n");
            pw.write("autoTrade=");
            pw.write(autoTrade.name());
            pw.write('\n');
            pw.write("goals=");
            for(final var goal : goals) {
                final var e = Registries.ENCHANTMENT.getId(goal.enchant());
                if(e == null)
                    continue;
                pw.write(e.toString());
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
        autoTrade = ALAutoTrade.OFF;
        goals = new ArrayList<>();
        // Load config
        try {
            try (final var bufferedReader = Files.newReader(configFile, Charsets.UTF_8)) {
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
                            case "autoTrade" -> autoTrade = value.equals("ENCHANT") ? ALAutoTrade.ENCHANT : (value.equals("CHEAPEST") ? ALAutoTrade.CHEAPEST : ALAutoTrade.OFF);
                            case "goals" -> {
                                final var gIt = Splitter.on(';').split(value).iterator();
                                final var COMMA_SPLITTER = Splitter.on(',');
                                while (gIt.hasNext()) {
                                    final var goalData = gIt.next();
                                    if(goalData.isEmpty())
                                        continue;
                                    final var gdIt = COMMA_SPLITTER.split(goalData).iterator();
                                    goals.add(new ALGoal(Registries.ENCHANTMENT.get(new Identifier(gdIt.next())), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next()), Integer.parseInt(gdIt.next())));
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
