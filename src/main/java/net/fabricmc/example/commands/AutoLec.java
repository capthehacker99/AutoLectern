package net.fabricmc.example.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import net.fabricmc.example.ALGoal;
import net.fabricmc.example.ExampleMod;
import net.fabricmc.example.villagerenchants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import static net.fabricmc.example.commands.ClientCommandManager.addClientSideCommand;
import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class AutoLec {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        addClientSideCommand("autolec");

        dispatcher.register(literal("autolec")
                        .then(literal("start").executes(ctx -> signalALstart(ctx.getSource())))
                .then(literal("stop").executes(ctx -> signalALstop(ctx.getSource())))
                .then(literal("itemsync").executes(ctx -> signalALitemsync(ctx.getSource())))
                .then(createaddgoalSubcommand())
                .then(literal("cleargoals").executes(ctx -> clearALgoals(ctx.getSource())))
                .then(literal("removegoal").then(argument("index", IntegerArgumentType.integer(0,Integer.MAX_VALUE))
                                .then(argument("uuid", IntegerArgumentType.integer(0,Integer.MAX_VALUE))
                                        .executes(ctx -> ALremovegoal(ctx.getSource(),IntegerArgumentType.getInteger(ctx, "index"),IntegerArgumentType.getInteger(ctx, "uuid"))))
                        .executes(ctx -> ALremovegoal(ctx.getSource(),IntegerArgumentType.getInteger(ctx, "index"),0))))
                .then(literal("listgoals").executes(ctx -> listALgoals(ctx.getSource())))
        );
    }
    private static ArgumentBuilder<ServerCommandSource, ?> createaddgoalSubcommand() {
        var subcmd = literal("addgoal");
        subcmd.then(literal("aqua_affinity")
                    .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.aqua_affinity, (byte) 1)))
                    .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.aqua_affinity, (byte) 0))))
                .then(literal("bane_of_arthropods")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.bane_of_arthropods, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.bane_of_arthropods, (byte) 0))))
                .then(literal("blast_protection")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.blast_protection, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.blast_protection, (byte) 0))))
                .then(literal("channeling")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.channeling, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.channeling, (byte) 0))))
                .then(literal("curse_of_binding")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.curse_of_binding, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.curse_of_binding, (byte) 0))))
                .then(literal("curse_of_vanishing")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.curse_of_vanishing, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.curse_of_vanishing, (byte) 0))))
                .then(literal("depth_strider")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.depth_strider, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.depth_strider, (byte) 0))))
                .then(literal("efficiency")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.efficiency, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.efficiency, (byte) 0))))
                .then(literal("feather_falling")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.feather_falling, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.feather_falling, (byte) 0))))
                .then(literal("fire_aspect")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fire_aspect, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fire_aspect, (byte) 0))))
                .then(literal("fire_protection")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fire_protection, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fire_protection, (byte) 0))))
                .then(literal("flame")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.flame, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.flame, (byte) 0))))
                .then(literal("fortune")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fortune, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.fortune, (byte) 0))))
                .then(literal("frost_walker")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.frost_walker, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.frost_walker, (byte) 0))))
                .then(literal("impaling")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.impaling, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.impaling, (byte) 0))))
                .then(literal("infinity")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.infinity, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.infinity, (byte) 0))))
                .then(literal("knockback")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.knockback, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.knockback, (byte) 0))))
                .then(literal("looting")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.looting, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.looting, (byte) 0))))
                .then(literal("loyalty")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.loyalty, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.loyalty, (byte) 0))))
                .then(literal("luck_of_the_sea")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.luck_of_the_sea, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.luck_of_the_sea, (byte) 0))))
                .then(literal("lure")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.lure, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.lure, (byte) 0))))
                .then(literal("mending")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.mending, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.mending, (byte) 0))))
                .then(literal("multishot")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.multishot, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.multishot, (byte) 0))))
                .then(literal("piercing")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.piercing, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.piercing, (byte) 0))))
                .then(literal("power")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.power, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.power, (byte) 0))))
                .then(literal("projectile_protection")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.projectile_protection, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.projectile_protection, (byte) 0))))
                .then(literal("protection")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.protection, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.protection, (byte) 0))))
                .then(literal("punch")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.punch, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.punch, (byte) 0))))
                .then(literal("quick_charge")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.quick_charge, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.quick_charge, (byte) 0))))
                .then(literal("respiration")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.respiration, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.respiration, (byte) 0))))
                .then(literal("riptide")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.riptide, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.riptide, (byte) 0))))
                .then(literal("sharpness")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.sharpness, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.sharpness, (byte) 0))))
                .then(literal("silk_touch")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.silk_touch, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.silk_touch, (byte) 0))))
                .then(literal("smite")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.smite, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.smite, (byte) 0))))
                .then(literal("sweeping_edge")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.sweeping_edge, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.sweeping_edge, (byte) 0))))
                .then(literal("thorns")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.thorns, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.thorns, (byte) 0))))
                .then(literal("unbreaking")
                        .then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.unbreaking, (byte) 1)))
                        .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(),villagerenchants.unbreaking, (byte) 0))))
                .then(literal("cheapest").executes(ctx -> ALallcheapest(ctx.getSource())));



        return subcmd;
    }

    private static int ALallcheapest(ServerCommandSource source) {
        for (villagerenchants n : villagerenchants.values()) {
            if(n == villagerenchants.NONE) continue;
            ExampleMod.ALcurgoal.add(new ALGoal((byte) 1,n));
        }
        ExampleMod.UUID++;
        return 0;
    }

    private static int ALremovegoal(ServerCommandSource source, Integer index,Integer uuid) {
        //System.out.println(index + ", " + uuid);
        if(uuid != 0 && uuid != ExampleMod.UUID){
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Command Expired.\nType \"/autolec listgoals\" then choose again.").formatted(Formatting.RED)));
            mc.inGameHud.getChatHud().resetScroll();
            return 0;
        }
        if(ExampleMod.ALcurgoal.size() > index){
            ExampleMod.ALcurgoal.remove((int)index);
            ExampleMod.UUID++;
            listALgoals(source);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().resetScroll();
        }else{
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Index out of bounds.").formatted(Formatting.RED)));
            mc.inGameHud.getChatHud().resetScroll();
        }
        return 0;
    }

    private static int ALaddgoal(ServerCommandSource source, villagerenchants ve,byte type) {
        ExampleMod.ALcurgoal.add(new ALGoal(type,ve));
        ExampleMod.UUID++;
        return 0;
    }

    private static int signalALstart(ServerCommandSource source) {
        ExampleMod.ALstart = true;
        return 0;
    }

    private static int signalALstop(ServerCommandSource source) {
        ExampleMod.ALstop = true;
        return 0;
    }

    private static int signalALitemsync(ServerCommandSource source) {
        ExampleMod.ALitemsync = !ExampleMod.ALitemsync;
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Item Sync is now " + (ExampleMod.ALitemsync ? "ON" : "OFF")).formatted(Formatting.WHITE)));
        return 0;
    }

    private static int clearALgoals(ServerCommandSource source) {
        ExampleMod.ALcurgoal.clear();
        ExampleMod.UUID++;
        return 0;
    }
    private static int listALgoals(ServerCommandSource source) {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Goals:").formatted(Formatting.WHITE)));
        Integer i = 0;
        for(ALGoal alg : ExampleMod.ALcurgoal) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[" + i + "] ").formatted(Formatting.YELLOW).append(Text.literal(alg.enchant.name() + (alg.type == 0 ? " any" : " cheapest")).formatted(Formatting.WHITE).append(Text.literal(" [REMOVE]").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/autolec removegoal " + i + " " + ExampleMod.UUID))).formatted(Formatting.RED))));
            i++;
        }
        return 0;
    }
}
