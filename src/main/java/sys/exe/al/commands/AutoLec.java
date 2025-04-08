package sys.exe.al.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.RegistryEntryReferenceArgumentType;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.EnchantmentTags;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import sys.exe.al.ALAutoTrade;
import sys.exe.al.ALGoal;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;


import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;
import static sys.exe.al.commands.ClientCommandManager.addClientSideCommand;



public class AutoLec {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
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
                                    final var AL = AutoLectern.getInstance();
                                    AL.itemSync = !AL.itemSync;
                                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                                            .formatted(Formatting.YELLOW)
                                            .append(Text.literal("Item Sync is now " + (AL.itemSync ? "ON" : "OFF"))
                                                    .formatted(Formatting.WHITE)
                                            )
                                    );
                                    return 0;
                                })
                        )
                )
                .then(literal("breakCooldown").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.breakCooldown = !AL.breakCooldown;
                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(Text.literal("Break cooldown is now " + (AL.breakCooldown ? "ON" : "OFF"))
                                    .formatted(Formatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("autoTrade")
                        .then(literal("ENCHANT").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.ENCHANT;
                            ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal("Auto Trading Mode is now set to ENCHANT.")
                                            .formatted(Formatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                        .then(literal("CHEAPEST").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.CHEAPEST;
                            ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal("Auto Trading Mode is now set to CHEAPEST.")
                                            .formatted(Formatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                        .then(literal("OFF").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.OFF;
                            ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                                    .formatted(Formatting.YELLOW)
                                    .append(Text.literal("Auto Trading Mode is now set to OFF.")
                                            .formatted(Formatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                )
                .then(literal("log").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.logTrade = !AL.logTrade;
                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(Text.literal("Logging is now " + (AL.logTrade ? "ON" : "OFF"))
                                    .formatted(Formatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("preBreak").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.preBreaking = !AL.preBreaking;
                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(Text.literal("Pre breaking is now " + (AL.preBreaking ? "ON" : "OFF"))
                                    .formatted(Formatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("preserveTool").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.preserveTool = !AL.preserveTool;
                    ctx.getSource().sendMessage(Text.literal("[Auto Lectern] ")
                            .formatted(Formatting.YELLOW)
                            .append(Text.literal("Tool preserving is now " + (AL.preserveTool ? "ON" : "OFF"))
                                    .formatted(Formatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(createAddGoalSubcommand(registryAccess))
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
                                .executes(ctx -> removeGoal(
                                            ctx.getSource(),
                                            IntegerArgumentType.getInteger(ctx, "index"),
                                            -1
                                        )
                                )
                                .then(argument("uuid", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                                        .executes(ctx -> removeGoal(ctx.getSource(), IntegerArgumentType.getInteger(ctx, "index"), IntegerArgumentType.getInteger(ctx, "uuid")))
                                )
                        )
                )
                .then(literal("list").executes(ctx -> listGoals(ctx.getSource())))
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> createLvlSubCommand(final ArgumentBuilder<ServerCommandSource, ?> arg, final boolean has_enchantment) {
        return arg.then(createPriceSubCommand(literal("min"), has_enchantment, -1, 0))
        .then(createPriceSubCommand(literal("max"), has_enchantment, 0, -1))
        .then(createPriceSubCommand(literal("any"), has_enchantment, -1, -1))
        .then(argument("minLevel", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .then(createPriceSubCommand(
                        argument("maxLevel", IntegerArgumentType.integer(0, Integer.MAX_VALUE)),
                        has_enchantment,
                        -2,
                        0
                        )
                )
        );
    }

    private static void addToGoal(final AutoLectern AL, final CommandContext<ServerCommandSource> ctx, final RegistryEntry<Enchantment> enchantment, final int minLvl, final int maxLvl, final int minPrice, final int maxPrice) {
        final int newMinLvl;
        final int newMaxLvl;
        if(minLvl == -2) {
            newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
            newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
        } else {
            newMinLvl = minLvl;
            newMaxLvl = maxLvl;
        }
        final var world = ((FakeCommandSource)ctx.getSource()).mc.world;
        assert world != null;
        if(enchantment != null) {
            assert enchantment.getKey().isPresent();
            AL.getGoals().add(new ALGoal(enchantment.getKey().get().getValue(), newMinLvl, newMaxLvl, minPrice, maxPrice));
        } else {
            world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT)
                    .iterateEntries(EnchantmentTags.TRADEABLE)
                    .forEach(anyEnchant -> {
                                assert anyEnchant.getKey().isPresent();
                                AL.getGoals()
                                        .add(
                                                new ALGoal(
                                                        anyEnchant.getKey().get().getValue(),
                                                        newMinLvl,
                                                        newMaxLvl,
                                                        minPrice,
                                                        maxPrice
                                                )
                                        );
                            }
                    );
        }
        AL.incrementUUID();
    }
    private static ArgumentBuilder<ServerCommandSource, ?> createPriceSubCommand(final ArgumentBuilder<ServerCommandSource, ?> arg, final boolean has_enchantment, final int minLvl, final int maxLvl) {
        return arg.then(literal("min").executes(ctx -> {
            addToGoal(
                    AutoLectern.getInstance(),
                    ctx,
                    has_enchantment ? RegistryEntryReferenceArgumentType.getEnchantment(ctx, "enchantment") : null,
                    minLvl,
                    maxLvl,
                    -1,
                    0
            );
            return 0;
        }))
        .then(literal("max").executes(ctx -> {
            addToGoal(
                    AutoLectern.getInstance(),
                    ctx,
                    has_enchantment ? RegistryEntryReferenceArgumentType.getEnchantment(ctx, "enchantment") : null,
                    minLvl,
                    maxLvl,
                    0,
                    -1
            );
            return 0;
        }))
        .then(literal("any").executes(ctx -> {
            addToGoal(
                    AutoLectern.getInstance(),
                    ctx,
                    has_enchantment ? RegistryEntryReferenceArgumentType.getEnchantment(ctx, "enchantment") : null,
                    minLvl,
                    maxLvl,
                    -1,
                    -1
            );
            return 0;
        }))
        .then(argument("minPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                .then(argument("maxPrice", IntegerArgumentType.integer(0, Integer.MAX_VALUE))
                        .executes(ctx -> {
                            addToGoal(
                                    AutoLectern.getInstance(),
                                    ctx,
                                    has_enchantment ? RegistryEntryReferenceArgumentType.getEnchantment(ctx, "enchantment") : null,
                                    minLvl,
                                    maxLvl,
                                    IntegerArgumentType.getInteger(ctx, "minPrice"),
                                    IntegerArgumentType.getInteger(ctx, "maxPrice")
                            );
                            return 0;
                        })
                )
        );
    }

    private static ArgumentBuilder<ServerCommandSource, ?> createAddGoalSubcommand(CommandRegistryAccess registryAccess) {
        final var subCmd = literal("add");
        subCmd.then(createLvlSubCommand(argument("enchantment", RegistryEntryReferenceArgumentType.registryEntry(registryAccess, RegistryKeys.ENCHANTMENT)), true));
        return createLvlSubCommand(subCmd, false);
    }

    @SuppressWarnings("SameReturnValue")
    private static int removeGoal(final ServerCommandSource src, final int index, final int uuid) {
        final var AL = AutoLectern.getInstance();
        final var chat = ((FakeCommandSource)src).mc.inGameHud.getChatHud();
        if (uuid != -1 && uuid != AL.getUUID()) {
            src.sendMessage(Text.literal("[Auto Lectern] ")
                    .formatted(Formatting.YELLOW)
                    .append(Text.literal("Command Expired.\nType \"/autolec list\" then choose again.")
                            .formatted(Formatting.RED)
                    )
            );
            chat.resetScroll();
            return 0;
        }
        final var goals = AL.getGoals();
        final var max = goals.size() - 1;
        if(max >= index) {
            goals.set(index, goals.get(max));
            goals.remove(max);
            AL.incrementUUID();
            listGoals(src);
            chat.resetScroll();
            return 0;
        }
        src.sendMessage(Text.literal("[Auto Lectern] ")
                .formatted(Formatting.YELLOW)
                .append(Text.literal("Index out of bounds.")
                        .formatted(Formatting.RED)
                )
        );
        chat.resetScroll();
        return 0;
    }


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
    @SuppressWarnings("SameReturnValue")
    private static int listGoals(final ServerCommandSource source) {
        source.sendMessage(Text.literal("[Auto Lectern] ")
                .formatted(Formatting.YELLOW)
                .append(Text.literal("Goals:")
                        .formatted(Formatting.WHITE)
                )
        );
        int i = 0;
        final var AL = AutoLectern.getInstance();
        final var goals = AL.getGoals();
        final var world = ((FakeCommandSource)source).mc.world;
        assert world != null;
        for (var goal : goals) {
            final var enchant = AutoLectern.enchantFromIdentifier(world, goal.enchant());
            if(enchant == null)
                continue;
            source.sendMessage(Text.literal("[" + i + "] ")
                    .formatted(Formatting.YELLOW)
                    .append(
                            enchant.value().description().copy().append(
                                    Text.literal(
                                            enchantLvlInfo(goal.lvlMin(), goal.lvlMax()) +
                                                    priceInfo(goal.priceMin(), goal.priceMax())
                                    )
                                ).formatted(Formatting.WHITE)
                                .append(Text.literal(" [REMOVE]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent.RunCommand(
                                                    "/autolec remove " + i + " " + AL.getUUID()
                                                ))
                                        ).formatted(Formatting.RED)
                                )
                    )
            );
            ++i;
        }
        return 0;
    }
}
