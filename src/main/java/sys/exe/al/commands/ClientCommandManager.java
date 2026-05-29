package sys.exe.al.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.ChatFormatting;
import sys.exe.al.AutoLectern;

import java.util.HashSet;
import java.util.Set;

public class ClientCommandManager {

    private static final Set<String> clientSideCommands = new HashSet<>();

    public static void clearClientSideCommands() {
        clientSideCommands.clear();
    }

    public static void addClientSideCommand(final String name) {
        clientSideCommands.add(name);
    }

    public static boolean isClientSideCommand(final String name) {
        return clientSideCommands.contains(name);
    }


    public static void sendError(final Component error) {
        sendFeedback(error.copy().withStyle(ChatFormatting.RED));
    }

    public static void sendFeedback(final Component message) {
        Minecraft.getInstance().gui.getChat().addMessage(message);
    }

    public static void executeCommand(final Minecraft mc, final StringReader reader, final String command) {
        final var player = mc.player;
        if(player == null)
            return;
        try {
            player.connection.getCommands().execute(reader, new FakeCommandSource(mc, player));
        } catch (final CommandSyntaxException e) {
            ClientCommandManager.sendError(ComponentUtils.fromMessage(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                final int cursor = Math.min(e.getCursor(), e.getInput().length());
                final MutableComponent text = Component.literal("").withStyle(ChatFormatting.GRAY)
                        .withStyle(style -> style.withClickEvent(new ClickEvent.SuggestCommand(command)));
                if (cursor > 10)
                    text.append("...");

                text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    text.append(Component.literal(e.getInput().substring(cursor)).withStyle(ChatFormatting.RED, ChatFormatting.UNDERLINE));
                }

                text.append(Component.translatable("command.context.here").withStyle(ChatFormatting.RED, ChatFormatting.ITALIC));
                ClientCommandManager.sendError(text);
            }
        } catch (final Exception e) {
            final var error = Component.literal(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            ClientCommandManager.sendError(Component.translatable("command.failed")
                    .withStyle(style -> style.withHoverEvent(new HoverEvent.ShowText(error))));
            AutoLectern.LOGGER.error("An error occurred: {}", e.getMessage(), e);
        }
    }
}