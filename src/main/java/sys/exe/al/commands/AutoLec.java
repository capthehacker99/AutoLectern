package sys.exe.al.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import sys.exe.al.ALGoal;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static sys.exe.al.commands.ClientCommandManager.addClientSideCommand;

public class AutoLec {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        addClientSideCommand("autolec");

        dispatcher.register(literal("autolec")
                .then(literal("start")
                        .executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            if(AL.getState() != ALState.STOPPED) {
                                ((FakeCommandSource) ctx.getSource()).mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ")
                                        .formatted(Formatting.YELLOW)
                                        .append(
                                                Text.literal("Please stop before starting again.")
                                                        .formatted(Formatting.RED)
                                        )
                                );
                                return 0;
                            }
                            AL.setState(ALState.STARTING);
                            return 0;
                        }
                    )
                )
                .then(literal("stop")
                        .executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            if(AL.getState() == ALState.STOPPED) {
                                ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                                        .formatted(Formatting.YELLOW)
                                        .append(
                                                Text.literal("Already stopped.")
                                                        .formatted(Formatting.RED)
                                        )
                                );
                                return 0;
                            }
                            AL.setState(ALState.STOPPING);
                            return 0;
                        }
                    )
                )
                .then(literal("sync")
                        .then(literal("item")
                                .executes(ctx -> {
                                    return 0; //TODO: SET CONFIG SYNC TO TRUE;
                                })
                        )
                )
                .then(createAddGoalSubcommand())
                .then(literal("clear").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.getGoals().clear();
                    AL.incrementUUID();
                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(
                                    Text.literal("All goals removed.")
                                            .formatted(Formatting.GREEN)
                            )
                    );
                    return 0;
                }))
                .then(literal("remove")
                        .then(argument("index", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                .executes(ctx -> removeGoal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), -1))
                                .then(argument("uuid", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                        .executes(ctx -> removeGoal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), IntegerArgumentType.getInteger(ctx, "uuid")))
                                )
                        )
                )
                .then(literal("list").executes(ctx -> listGoals(ctx.getSource())))
        );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> createLvlSubCommand(final LiteralArgumentBuilder<ServerCommandSource> arg, final @Nullable Enchantment enchantment) {
        return arg.then(createPriceSubCommand(literal("min"), enchantment, -1, 0))
        .then(createPriceSubCommand(literal("max"), enchantment, 0, -1))
        .then(createPriceSubCommand(literal("any"), enchantment, -1, -1))
        .then(argument("minLevel", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .then(createPriceSubCommand(
                        argument("maxLevel", IntegerArgumentType.integer(0, Integer.MAX_VALUE)),
                        enchantment,
                        -2,
                        0
                        )
                )
        );
    }
    private static ArgumentBuilder<ServerCommandSource, ?> createPriceSubCommand(final ArgumentBuilder<ServerCommandSource, ?> arg, final @Nullable Enchantment enchant, final int minLvl, final int maxLvl) {
        return arg.then(literal("min").executes(ctx -> {
            final int newMinLvl;
            final int newMaxLvl;
            if(minLvl == -2) {
                newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
                newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
            } else {
                newMinLvl = minLvl;
                newMaxLvl = maxLvl;
            }
            final var AL = AutoLectern.getInstance();
            if(enchant != null)
                AL.getGoals().add(new ALGoal(enchant, newMinLvl, newMaxLvl, -1, 0));
            else
                for (final var anyEnchant : Registries.ENCHANTMENT)
                    if(anyEnchant.isAvailableForEnchantedBookOffer())
                        AL.getGoals().add(new ALGoal(anyEnchant, newMinLvl, newMaxLvl, -1,0));
            AL.incrementUUID();
            return 0;
        }))
        .then(literal("max").executes(ctx -> {
            final int newMinLvl;
            final int newMaxLvl;
            if(minLvl == -2) {
                newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
                newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
            } else {
                newMinLvl = minLvl;
                newMaxLvl = maxLvl;
            }
            final var AL = AutoLectern.getInstance();
            if(enchant != null)
                AL.getGoals().add(new ALGoal(enchant, newMinLvl, newMaxLvl, 0, -1));
            else
                for (final var anyEnchant : Registries.ENCHANTMENT)
                    if(anyEnchant.isAvailableForEnchantedBookOffer())
                        AL.getGoals().add(new ALGoal(anyEnchant, newMinLvl, newMaxLvl, 0,-1));
            AL.incrementUUID();
            return 0;
        }))
        .then(literal("any").executes(ctx -> {
            final int newMinLvl;
            final int newMaxLvl;
            if(minLvl == -2) {
                newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
                newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
            } else {
                newMinLvl = minLvl;
                newMaxLvl = maxLvl;
            }
            final var AL = AutoLectern.getInstance();
            AL.getGoals().add(new ALGoal(enchant, newMinLvl, newMaxLvl, -1, -1));
            AL.incrementUUID();
            return 0;
        }))
        .then(argument("minPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .then(argument("maxPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                        .executes(ctx -> {
                            final int newMinLvl;
                            final int newMaxLvl;
                            if(minLvl == -2) {
                                newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
                                newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
                            } else {
                                newMinLvl = minLvl;
                                newMaxLvl = maxLvl;
                            }
                            final var AL = AutoLectern.getInstance();
                            AL.getGoals().add(new ALGoal(enchant,
                                    newMinLvl,
                                    newMaxLvl,
                                    IntegerArgumentType.getInteger(ctx, "minPrice"),
                                    IntegerArgumentType.getInteger(ctx, "maxPrice")
                            ));
                            AL.incrementUUID();
                            return 0;
                        })
                )
        );
    }

    @SuppressWarnings("ManualMinMaxCalculation")
    private static ArgumentBuilder<ServerCommandSource, ?> createAddGoalSubcommand() {
        var subCmd = literal("add");
        for (final var enchantment : Registries.ENCHANTMENT) {
            if(!enchantment.isAvailableForEnchantedBookOffer())
                continue;
            final var transKey = enchantment.getTranslationKey();
            int idx = transKey.lastIndexOf('.');
            if(transKey.length()-idx > 1)
                idx++;
            subCmd = subCmd.then(createLvlSubCommand(literal(transKey.substring(idx < 0 ? 0 : idx)), enchantment));
        }
        return createLvlSubCommand(subCmd, null);
    }

    @SuppressWarnings("SameReturnValue")
    private static int removeGoal(final ServerCommandSource src, final int index, final int uuid) {
        final var AL = AutoLectern.getInstance();
        if (uuid != -1 && uuid != AL.getUUID()) {
            src.sendMessage(Text.literal("[Auto Lectern] ")
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal("Command Expired.\nType \"/autolec list\" then choose again.")
                            .formatted(Formatting.RED)
                    )
            );
            ((FakeCommandSource)src).mc.inGameHud.getChatHud().resetScroll();
            return 0;
        }
        final var goals = AL.getGoals();
        if(goals.size() > index) {
            goals.remove(index);
            AL.incrementUUID();
            listGoals(src);
            ((FakeCommandSource)src).mc.inGameHud.getChatHud().resetScroll();
            return 0;
        }
        src.sendMessage(Text.literal("[Auto Lectern] ")
                .formatted(Formatting.YELLOW)
                .append(Text.literal("Index out of bounds.")
                        .formatted(Formatting.RED)
                )
        );
        ((FakeCommandSource)src).mc.inGameHud.getChatHud().resetScroll();
        return 0;
    }
//
//    private static int signalALitemsync(ServerCommandSource source) {
//        AutoLectern.ALitemsync = !AutoLectern.ALitemsync;
//        MinecraftClient mc = MinecraftClient.getInstance();
//        mc.inGameHud.getChatHud().addMessage(Text.literal("[Auto Lectern] ").formatted(Formatting.YELLOW).append(Text.literal("Item Sync is now " + (AutoLectern.ALitemsync ? "ON" : "OFF")).formatted(Formatting.WHITE)));
//        return 0;
//    }

    private static String enchantLvlInfo(final int min, final int max) {
        if(min == -1){
            if(max == -1)
                return " any level,";
            return " lowest level,";
        } else if(max == -1)
            return " max level,";
        return " level from " + min + " to " + max + ",";
    }
    private static String priceInfo(final int min, final int max) {
        if(min == -1){
            if(max == -1)
                return " any price.";
            return " cheapest.";
        } else if(max == -1)
            return " most expensive.";
        return " from " + min + " to " + max + " emeralds.";
    }
    private static int listGoals(final ServerCommandSource source) {
        source.sendMessage(Text.literal("[Auto Lectern] ")
                .formatted(Formatting.YELLOW)
                .append(Text.literal("Goals:")
                        .formatted(Formatting.WHITE)
                )
        );
        int i = 0;
        final var AL = AutoLectern.getInstance();
        for (final var goal : AL.getGoals()) {
            source.sendMessage(Text.literal("[" + i++ + "] ")
                    .formatted(Formatting.YELLOW)
                    .append(
                            Text.translatable(
                                            goal.enchant().getTranslationKey()
                                    ).append(
                                            Text.literal(
                                                    enchantLvlInfo(goal.lvlMin(), goal.lvlMax()) +
                                                            priceInfo(goal.priceMin(), goal.priceMax())
                                            )
                                    ).formatted(Formatting.WHITE)
                                    .append(Text.literal(" [REMOVE]")
                                            .setStyle(Style.EMPTY
                                                    .withClickEvent(new ClickEvent(
                                                            ClickEvent.Action.RUN_COMMAND,
                                                            "/autolec remove " + i + " " + AL.getUUID()
                                                    ))
                                            ).formatted(Formatting.RED)
                                    )
                    )
            );
        }
        return 0;
    }
}
