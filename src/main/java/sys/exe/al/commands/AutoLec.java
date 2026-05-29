package sys.exe.al.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Holder;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import org.jetbrains.annotations.NotNull;
import sys.exe.al.ALAutoTrade;
import sys.exe.al.ALGoal;
import sys.exe.al.ALState;
import sys.exe.al.AutoLectern;

import static sys.exe.al.commands.ClientCommandManager.addClientSideCommand;



public class AutoLec {

    private static Holder.Reference<@NotNull Enchantment> getEnchantment(CommandContext<ClientSuggestionProvider> context) {
        @SuppressWarnings("unchecked")
        Holder.Reference<@NotNull Enchantment> reference = (Holder.Reference<@NotNull Enchantment>)context.getArgument("enchantment", Holder.Reference.class);
        ResourceKey<@NotNull Enchantment> registryKey = reference.key();
        if (registryKey.isFor(Registries.ENCHANTMENT))
            return reference;
        return null;
    }
    private static LiteralArgumentBuilder<ClientSuggestionProvider> literal(String literal) {
        return LiteralArgumentBuilder.literal(literal);
    }

    private static <T> RequiredArgumentBuilder<ClientSuggestionProvider, T> argument(String name, ArgumentType<T> type) {
        return RequiredArgumentBuilder.argument(name, type);
    }
    public static void register(CommandDispatcher<ClientSuggestionProvider> dispatcher, CommandBuildContext registryAccess) {
        addClientSideCommand("autolec");

        dispatcher.register(literal("autolec")
                .then(literal("start")
                        .executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            if(AL.getState() != ALState.STOPPED) {
                                ((FakeCommandSource) ctx.getSource()).mc.gui.getChat().addMessage(Component.literal("[Auto Lectern] ")
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(
                                                Component.literal("Please stop before starting again.")
                                                        .withStyle(ChatFormatting.RED)
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
                                ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                                        .withStyle(ChatFormatting.YELLOW)
                                        .append(
                                                Component.literal("Already stopped.")
                                                        .withStyle(ChatFormatting.RED)
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
                                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                                            .withStyle(ChatFormatting.YELLOW)
                                            .append(Component.literal("Item Sync is now " + (AL.itemSync ? "ON" : "OFF"))
                                                    .withStyle(ChatFormatting.WHITE)
                                            )
                                    );
                                    return 0;
                                })
                        )
                )
                .then(literal("breakCooldown").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.breakCooldown = !AL.breakCooldown;
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Break cooldown is now " + (AL.breakCooldown ? "ON" : "OFF"))
                                    .withStyle(ChatFormatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("autoTrade")
                        .then(literal("ENCHANT").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.ENCHANT;
                            ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal("Auto Trading Mode is now set to ENCHANT.")
                                            .withStyle(ChatFormatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                        .then(literal("CHEAPEST").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.CHEAPEST;
                            ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal("Auto Trading Mode is now set to CHEAPEST.")
                                            .withStyle(ChatFormatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                        .then(literal("OFF").executes(ctx -> {
                            final var AL = AutoLectern.getInstance();
                            AL.autoTrade = ALAutoTrade.OFF;
                            ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                                    .withStyle(ChatFormatting.YELLOW)
                                    .append(Component.literal("Auto Trading Mode is now set to OFF.")
                                            .withStyle(ChatFormatting.WHITE)
                                    )
                            );
                            return 0;
                        }))
                )
                .then(literal("log").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.logTrade = !AL.logTrade;
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Logging is now " + (AL.logTrade ? "ON" : "OFF"))
                                    .withStyle(ChatFormatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("preBreak").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.preBreaking = !AL.preBreaking;
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Pre breaking is now " + (AL.preBreaking ? "ON" : "OFF"))
                                    .withStyle(ChatFormatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("preserveTool").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.preserveTool = !AL.preserveTool;
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Tool preserving is now " + (AL.preserveTool ? "ON" : "OFF"))
                                    .withStyle(ChatFormatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(createAddGoalSubcommand(registryAccess))
                .then(literal("clear").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.getGoals().clear();
                    AL.incrementUUID();
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(
                                    Component.literal("All goals removed.")
                                            .withStyle(ChatFormatting.GREEN)
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
                .then(literal("autoRemove").executes(ctx -> {
                    final var AL = AutoLectern.getInstance();
                    AL.autoRemove = !AL.autoRemove;
                    ((FakeCommandSource)ctx.getSource()).sendMessage(Component.literal("[Auto Lectern] ")
                            .withStyle(ChatFormatting.YELLOW)
                            .append(Component.literal("Auto remove is now " + (AL.autoRemove ? "ON" : "OFF"))
                                    .withStyle(ChatFormatting.WHITE)
                            )
                    );
                    return 0;
                }))
                .then(literal("list").executes(ctx -> listGoals(ctx.getSource())))
        );
    }

    private static ArgumentBuilder<ClientSuggestionProvider, ?> createLvlSubCommand(final ArgumentBuilder<ClientSuggestionProvider, ?> arg, final boolean has_enchantment) {
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

    private static void addToGoal(final AutoLectern AL, final CommandContext<ClientSuggestionProvider> ctx, final Holder<@NotNull Enchantment> enchantment, final int minLvl, final int maxLvl, final int minPrice, final int maxPrice) {
        final int newMinLvl;
        final int newMaxLvl;
        if(minLvl == -2) {
            newMinLvl = IntegerArgumentType.getInteger(ctx, "minLevel");
            newMaxLvl = IntegerArgumentType.getInteger(ctx, "maxLevel");
        } else {
            newMinLvl = minLvl;
            newMaxLvl = maxLvl;
        }
        final var world = ((FakeCommandSource)ctx.getSource()).mc.level;
        assert world != null;
        if(enchantment != null) {
            assert enchantment.unwrapKey().isPresent();
            AL.getGoals().add(new ALGoal(enchantment.unwrapKey().get().identifier(), newMinLvl, newMaxLvl, minPrice, maxPrice));
        } else {
            world.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getTagOrEmpty(EnchantmentTags.TRADEABLE)
                    .forEach(anyEnchant -> {
                                assert anyEnchant.unwrapKey().isPresent();
                                AL.getGoals()
                                        .add(
                                                new ALGoal(
                                                        anyEnchant.unwrapKey().get().identifier(),
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
    private static ArgumentBuilder<ClientSuggestionProvider, ?> createPriceSubCommand(final ArgumentBuilder<ClientSuggestionProvider, ?> arg, final boolean has_enchantment, final int minLvl, final int maxLvl) {
        return arg.then(literal("min").executes(ctx -> {
            addToGoal(
                    AutoLectern.getInstance(),
                    ctx,
                    has_enchantment ? getEnchantment(ctx) : null,
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
                    has_enchantment ? getEnchantment(ctx) : null,
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
                    has_enchantment ? getEnchantment(ctx) : null,
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
                                    has_enchantment ? getEnchantment(ctx) : null,
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

    private static ArgumentBuilder<ClientSuggestionProvider, ?> createAddGoalSubcommand(CommandBuildContext registryAccess) {
        final var subCmd = literal("add");
        subCmd.then(createLvlSubCommand(argument("enchantment", ResourceArgument.resource(registryAccess, Registries.ENCHANTMENT)), true));
        return createLvlSubCommand(subCmd, false);
    }

    @SuppressWarnings("SameReturnValue")
    private static int removeGoal(final ClientSuggestionProvider src, final int index, final int uuid) {
        final var AL = AutoLectern.getInstance();
        final var chat = ((FakeCommandSource)src).mc.gui.getChat();
        if (uuid != -1 && uuid != AL.getUUID()) {
            ((FakeCommandSource)src).sendMessage(Component.literal("[Auto Lectern] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(Component.literal("Command Expired.\nType \"/autolec list\" then choose again.")
                            .withStyle(ChatFormatting.RED)
                    )
            );
            chat.resetChatScroll();
            return 0;
        }
        final var goals = AL.getGoals();
        final var max = goals.size() - 1;
        if(max >= index) {
            goals.set(index, goals.get(max));
            goals.remove(max);
            AL.incrementUUID();
            listGoals(src);
            chat.resetChatScroll();
            return 0;
        }
        ((FakeCommandSource)src).sendMessage(Component.literal("[Auto Lectern] ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Index out of bounds.")
                        .withStyle(ChatFormatting.RED)
                )
        );
        chat.resetChatScroll();
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
    private static int listGoals(final ClientSuggestionProvider source) {
        ((FakeCommandSource)source).sendMessage(Component.literal("[Auto Lectern] ")
                .withStyle(ChatFormatting.YELLOW)
                .append(Component.literal("Goals:")
                        .withStyle(ChatFormatting.WHITE)
                )
        );
        int i = 0;
        final var AL = AutoLectern.getInstance();
        final var goals = AL.getGoals();
        final var world = ((FakeCommandSource)source).mc.level;
        assert world != null;
        for (var goal : goals) {
            final var enchant = AutoLectern.enchantFromIdentifier(world, goal.enchant());
            if(enchant == null)
                continue;
            ((FakeCommandSource)source).sendMessage(Component.literal("[" + i + "] ")
                    .withStyle(ChatFormatting.YELLOW)
                    .append(
                            enchant.value().description().copy().append(
                                    Component.literal(
                                            enchantLvlInfo(goal.lvlMin(), goal.lvlMax()) +
                                                    priceInfo(goal.priceMin(), goal.priceMax())
                                    )
                                ).withStyle(ChatFormatting.WHITE)
                                .append(Component.literal(" [REMOVE]")
                                        .setStyle(Style.EMPTY
                                                .withClickEvent(new ClickEvent.RunCommand(
                                                    "/autolec remove " + i + " " + AL.getUUID()
                                                ))
                                        ).withStyle(ChatFormatting.RED)
                                )
                    )
            );
            ++i;
        }
        return 0;
    }
}
