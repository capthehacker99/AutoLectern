package net.fabricmc.example.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.example.ALGoal;
import net.fabricmc.example.ExampleMod;
import net.minecraft.client.MinecraftClient;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
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
                .then(createAddGoalSubcommand())
                .then(literal("cleargoals").executes(ctx -> clearALgoals(ctx.getSource())))
                .then(literal("removegoal").then(argument("index", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                        .then(argument("uuid", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> ALremovegoal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), IntegerArgumentType.getInteger(ctx, "uuid"))))
                        .executes(ctx -> ALremovegoal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), 0))))
                .then(literal("listgoals").executes(ctx -> listALgoals(ctx.getSource())))
        );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createGoalSubcommand(LiteralArgumentBuilder<ServerCommandSource> literal, Enchantment enchantment) {
        return literal.then(literal("cheapest").executes(ctx -> ALaddgoal(ctx.getSource(), enchantment, (byte) 1)))
                .then(literal("any").executes(ctx -> ALaddgoal(ctx.getSource(), enchantment, (byte) 0)))
                .then(argument("minPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                        .then(argument("maxPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> ALaddgoal(ctx.getSource(),
                                        enchantment,
                                        IntegerArgumentType.getInteger(ctx, "minPrice"),
                                        IntegerArgumentType.getInteger(ctx, "maxPrice"))
                                )
                        )
                );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> createAddGoalSubcommand() {
        var subCMD = literal("addgoal");
        for (Enchantment enchantment : Registries.ENCHANTMENT) {
            if(!enchantment.isAvailableForEnchantedBookOffer())
                continue;
            String transKey = enchantment.getTranslationKey();
            int idx = transKey.lastIndexOf('.');
            if(transKey.length()-idx > 1)
                idx++;
            subCMD = subCMD.then(createGoalSubcommand(literal(transKey.substring(Math.max(idx, 0))), enchantment));
        }
        subCMD.then(literal("cheapest").executes(ctx -> ALallcheapest(ctx.getSource())));

        return subCMD;
    }

    private static int ALallcheapest(ServerCommandSource source) {
        for (Enchantment enchantment : Registries.ENCHANTMENT) {
            ExampleMod.ALcurgoal.add(new ALGoal((byte) 1, enchantment));
        }
        ExampleMod.UUID++;
        return 0;
    }

    private static int ALremovegoal(ServerCommandSource source, Integer index, Integer uuid) {
        if (uuid != 0 && !uuid.equals(ExampleMod.UUID)) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Command Expired.\nType \"/autolec listgoals\" then choose again.").formatted(Formatting.RED)));
            mc.inGameHud.getChatHud().resetScroll();
            return 0;
        }
        if (ExampleMod.ALcurgoal.size() > index) {
            ExampleMod.ALcurgoal.remove((int) index);
            ExampleMod.UUID++;
            listALgoals(source);
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().resetScroll();
        } else {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Index out of bounds.").formatted(Formatting.RED)));
            mc.inGameHud.getChatHud().resetScroll();
        }
        return 0;
    }

    private static int ALaddgoal(ServerCommandSource source, Enchantment ve, Integer minPrice, Integer maxPrice) {
        if(minPrice > maxPrice){
            minPrice ^= maxPrice;
            maxPrice ^= minPrice;
            minPrice ^= maxPrice;
        }
        ExampleMod.ALcurgoal.add(new ALGoal(ve, minPrice, maxPrice));
        ExampleMod.UUID++;
        return 0;
    }

    private static int ALaddgoal(ServerCommandSource source, Enchantment ve, byte type) {
        ExampleMod.ALcurgoal.add(new ALGoal(type, ve));
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
        for (ALGoal alg : ExampleMod.ALcurgoal) {
            mc.inGameHud.getChatHud().addMessage(Text.literal("[" + i + "] ").formatted(Formatting.YELLOW).append(Text.translatable(alg.enchant.getTranslationKey()).append(Text.literal((alg.type == 0 ? " any" : (alg.type == 1 ?" cheapest" : " from " + alg.min + " to " + alg.max + " emeralds.")))).formatted(Formatting.WHITE).append(Text.literal(" [REMOVE]").setStyle(Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/autolec removegoal " + i + " " + ExampleMod.UUID))).formatted(Formatting.RED))));
            i++;
        }
        return 0;
    }
}
