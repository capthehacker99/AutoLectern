package net.fabricmc.example;

import com.mojang.brigadier.CommandDispatcher;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.example.commands.AutoLec;
import net.fabricmc.example.commands.ClientCommandManager;
import net.fabricmc.example.mixin.MinecraftClientAccessor;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.ingame.MerchantScreen;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.passive.VillagerEntity;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Util;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;


import java.io.File;
import java.util.Map;
import java.util.Vector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExampleMod implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.

	public static Map<Enchantment,villagerenchants> e2ve = Stream.of(new Object[][] {
			{ Enchantments.PROTECTION,villagerenchants.protection },
			{Enchantments.FIRE_PROTECTION,villagerenchants.fire_protection},
			{Enchantments.FEATHER_FALLING,villagerenchants.feather_falling},
			{Enchantments.BLAST_PROTECTION,villagerenchants.blast_protection},
			{Enchantments.PROJECTILE_PROTECTION,villagerenchants.projectile_protection},
			{Enchantments.RESPIRATION,villagerenchants.respiration},
			{Enchantments.AQUA_AFFINITY,villagerenchants.aqua_affinity},
			{Enchantments.THORNS,villagerenchants.thorns},
			{Enchantments.DEPTH_STRIDER,villagerenchants.depth_strider},
			{Enchantments.FROST_WALKER,villagerenchants.frost_walker},
			{Enchantments.BINDING_CURSE,villagerenchants.curse_of_binding},
			{Enchantments.SHARPNESS,villagerenchants.sharpness},
			{Enchantments.SMITE,villagerenchants.smite},
			{Enchantments.BANE_OF_ARTHROPODS,villagerenchants.bane_of_arthropods},
			{Enchantments.KNOCKBACK,villagerenchants.knockback},
			{Enchantments.FIRE_ASPECT,villagerenchants.fire_aspect},
			{Enchantments.LOOTING,villagerenchants.looting},
			{Enchantments.SWEEPING,villagerenchants.sweeping_edge},
			{Enchantments.EFFICIENCY,villagerenchants.efficiency},
			{Enchantments.SILK_TOUCH,villagerenchants.silk_touch},
			{Enchantments.UNBREAKING,villagerenchants.unbreaking},
			{Enchantments.FORTUNE,villagerenchants.fortune},
			{Enchantments.POWER,villagerenchants.power},
			{Enchantments.PUNCH,villagerenchants.punch},
			{Enchantments.FLAME,villagerenchants.flame},
			{Enchantments.INFINITY,villagerenchants.infinity},
			{Enchantments.LUCK_OF_THE_SEA,villagerenchants.luck_of_the_sea},
			{Enchantments.LURE,villagerenchants.lure},
			{Enchantments.LOYALTY,villagerenchants.loyalty},
			{Enchantments.IMPALING,villagerenchants.impaling},
			{Enchantments.RIPTIDE,villagerenchants.riptide},
			{Enchantments.CHANNELING,villagerenchants.channeling},
			{Enchantments.MULTISHOT,villagerenchants.multishot},
			{Enchantments.QUICK_CHARGE,villagerenchants.quick_charge},
			{Enchantments.PIERCING,villagerenchants.piercing},
			{Enchantments.MENDING,villagerenchants.mending},
			{Enchantments.VANISHING_CURSE,villagerenchants.curse_of_vanishing}
	}).collect(Collectors.toMap(data -> (Enchantment) data[0], data -> (villagerenchants) data[1]));

	public static File configDir;
	public static final ExampleMod INSTANCE = new ExampleMod();
	public static boolean ALstart = false;
	public static boolean ALstop = false;
	public boolean ALissneak = false;
	public static boolean ALvillupdate = false;
	public static boolean ALofferupdate = false;
	public static VillagerEntity yevilldatgotupdated;
	public static NewVillagerInfo NVI = new NewVillagerInfo(villagerenchants.NONE,999);
	public static Vector<ALGoal> ALcurgoal = new Vector<ALGoal>(2,1);
	public static boolean ALdotrackpro = false;
	public static boolean ALdotracktrades = false;
	public long stage4start = 0;
	public long stage3start = 0;
	public static int stage = 0;
	public double stageayaw = 0;
	public double stageapitch = 0;
	public Vec3d plroripos = Vec3d.ZERO;
	public Direction lecside = Direction.SOUTH;
	public static BlockPos lecloc = null;
	public ExampleMod(){};

	public Integer getCheapestVE(villagerenchants ve){
		switch(ve){
			case aqua_affinity:
			case channeling:
			case flame:
			case infinity:
			case multishot:
			case silk_touch:
				return 5;
			case bane_of_arthropods:
			case efficiency:
			case impaling:
			case power:
			case sharpness:
			case smite:
				return 17;
			case blast_protection:
			case feather_falling:
			case fire_protection:
			case piercing:
			case projectile_protection:
			case protection:
				return 14;
			case curse_of_binding:
			case curse_of_vanishing:
			case mending:
				return 10;
			case depth_strider:
			case fortune:
			case looting:
			case loyalty:
			case luck_of_the_sea:
			case lure:
			case quick_charge:
			case respiration:
			case riptide:
			case sweeping_edge:
			case thorns:
			case unbreaking:
				return 11;
			case fire_aspect:
			case knockback:
			case punch:
				return 8;
			case frost_walker:
				return 16;

			default:
				return 0;
		}
	}

	public void MinecraftTickHead(MinecraftClient mc){


		if(ALstart){
			if(mc.crosshairTarget.getType() == HitResult.Type.BLOCK && mc.world.getBlockState(((BlockHitResult)mc.crosshairTarget).getBlockPos()).getBlock() == Blocks.LECTERN){
				stageayaw = mc.player.getYaw();
				stageapitch = mc.player.getPitch();
				plroripos = mc.player.getPos();
				lecloc = ((BlockHitResult)mc.crosshairTarget).getBlockPos();
				lecside = ((BlockHitResult)mc.crosshairTarget).getSide();
				if(mc.player.isInSneakingPose()){
					ALissneak = true;
				}
				mc.inGameHud.getChatHud().addMessage(new LiteralText("Started").formatted(Formatting.GREEN));

				stage = 1;
			}else{
				mc.inGameHud.getChatHud().addMessage(new LiteralText("Please look at a lectern before running this command.").formatted(Formatting.RED));
			}
			ALstart = false;
		}
        if(ALstop){
            mc.inGameHud.getChatHud().addMessage(new LiteralText("Stopping...").formatted(Formatting.RED));

        }
		if(stage != 0){

			if (mc.player == null) {
				stage = 0;
			}
		}
		if(stage == 1){
			mc.options.keyForward.setPressed(false);
			mc.options.keyBack.setPressed(false);
			mc.options.keyLeft.setPressed(false);
			mc.options.keyRight.setPressed(false);
			mc.options.keySneak.setPressed(ALissneak);
			mc.player.setPosition(plroripos);
			if (mc.currentScreen != null && mc.currentScreen instanceof MerchantScreen) {
				mc.player.closeHandledScreen();
			}
			if(mc.world.getBlockState(lecloc).getBlock() == Blocks.AIR){
				//System.out.println("=> stage 2");
				stage = 2;
				mc.options.keyAttack.setPressed(false);
				mc.interactionManager.cancelBlockBreaking();
			}else{
				mc.player.setYaw((float)stageayaw);
				mc.player.setPitch((float)stageapitch);
				mc.options.keyAttack.setPressed(true);
				mc.interactionManager.updateBlockBreakingProgress(lecloc, lecside);
			}
			if(ALstop){
				mc.interactionManager.cancelBlockBreaking();
				ALstop = false;
				mc.options.keyAttack.setPressed(false);
				stage = 0;
                mc.inGameHud.getChatHud().addMessage(new LiteralText("Stopped.").formatted(Formatting.RED));
			}
		}else if(stage == 2){
			mc.options.keyForward.setPressed(false);
			mc.options.keyBack.setPressed(false);
			mc.options.keyLeft.setPressed(false);
			mc.options.keyRight.setPressed(false);
			mc.options.keySneak.setPressed(ALissneak);
			mc.player.setPosition(plroripos);
			if(mc.world.getBlockState(lecloc).getBlock() != Blocks.LECTERN) {
				if(mc.world.getBlockState(lecloc).getBlock() != Blocks.AIR){
					mc.options.keyUse.setPressed(false);
					//System.out.println("=> stage 1");
					stage = 1;
				}else {
					mc.player.setYaw((float) stageayaw);
					mc.player.setPitch((float) stageapitch);
					mc.options.keyUse.setPressed(true);
					if(((MinecraftClientAccessor)mc).getItemUseCooldown() <= 0) {
						((MinecraftClientAccessor) mc).invokedoItemUse();
					}
				}
			}else{
				mc.options.keyUse.setPressed(false);
				//System.out.println("=> stage 3");
				stage = 3;
				ALdotrackpro = true;
				stage3start = System.currentTimeMillis();
			}
			if(ALstop){
				ALstop = false;
				mc.options.keyUse.setPressed(false);
				stage = 0;
                mc.inGameHud.getChatHud().addMessage(new LiteralText("Stopped.").formatted(Formatting.RED));
			}
		}else if(stage == 3){

			mc.options.keyForward.setPressed(false);
			mc.options.keyBack.setPressed(false);
			mc.options.keyLeft.setPressed(false);
			mc.options.keyRight.setPressed(false);
			mc.options.keySneak.setPressed(ALissneak);
			mc.player.setPosition(plroripos);
			boolean ischatscreen = false;
			if (mc.currentScreen != null && mc.currentScreen instanceof ChatScreen) {
				ischatscreen = true;
				stage3start = System.currentTimeMillis();
			}
			if(ALvillupdate && !ischatscreen) {

				//System.out.println("=> stage 4");
				mc.interactionManager.interactEntity(mc.player, yevilldatgotupdated, Hand.MAIN_HAND);
				stage = 4;
				ALdotracktrades = true;
				ALvillupdate = false;
				ALdotrackpro = false;
				stage4start = System.currentTimeMillis();
			}
			if((System.currentTimeMillis()-stage3start)>=3000){
				//System.out.println("=> stage 1");
				ALdotrackpro = false;
				stage = 1;
			}
			if(ALstop){
				ALdotrackpro = false;
				ALstop = false;
				stage = 0;
                mc.inGameHud.getChatHud().addMessage(new LiteralText("Stopped.").formatted(Formatting.RED));
			}
		}else if(stage == 4){
			mc.options.keyForward.setPressed(false);
			mc.options.keyBack.setPressed(false);
			mc.options.keyLeft.setPressed(false);
			mc.options.keyRight.setPressed(false);
			mc.options.keySneak.setPressed(ALissneak);
			mc.player.setPosition(plroripos);
			if(ALofferupdate){
				ALofferupdate = false;
				if (mc.currentScreen != null && mc.currentScreen instanceof MerchantScreen) {
					mc.player.closeHandledScreen();
				}
				if(ALcurgoal.isEmpty()){
					if(NVI.VE != villagerenchants.NONE){
						ALdotracktrades = false;
						stage = 0;
						mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,1));
						GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
						mc.inGameHud.getChatHud().addMessage(new LiteralText("Completed.").formatted(Formatting.GREEN));
					}else{
						//System.out.println("=> stage 1");
						ALdotracktrades = false;
						stage = 1;
					}
				}else{
					for(ALGoal alg : ALcurgoal){
						if(alg.enchant == NVI.VE && (alg.type == 0 || (alg.type == 1 && getCheapestVE(NVI.VE) == NVI.price))){
							ALdotracktrades = false;
							stage = 0;
							mc.getSoundManager().play(PositionedSoundInstance.master(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,1));
							GLFW.glfwRequestWindowAttention(mc.getWindow().getHandle());
							mc.inGameHud.getChatHud().addMessage(new LiteralText("Completed.").formatted(Formatting.GREEN));
							break;
						}
					}
					if(stage != 0) {
						//System.out.println("=> stage 1");
						ALdotracktrades = false;
						stage = 1;
					}
				}

			}
			if((System.currentTimeMillis()-stage4start)>=3000){
				//System.out.println("=> stage 1");
				ALdotracktrades = false;
				stage = 1;
			}
			if(ALstop){
				ALdotracktrades = false;
				ALstop = false;
				stage = 0;
                mc.inGameHud.getChatHud().addMessage(new LiteralText("Stopped.").formatted(Formatting.RED));
			}
		}
	}
	public static void registerCommands(CommandDispatcher<ServerCommandSource> dispatcher) {
		ClientCommandManager.clearClientSideCommands();
		AutoLec.register(dispatcher);

	}
	@Override
	public void onInitialize() {
		configDir = new File(FabricLoader.getInstance().getConfigDirectory(), "autolectern");
		//noinspection ResultOfMethodCallIgnored
		configDir.mkdirs();
	}
}
