package sys.exe.al.commands;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.*;
import net.minecraft.util.Formatting;
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


    public static void sendError(final Text error) {
        sendFeedback(error.copy().formatted(Formatting.RED));
    }

    public static void sendFeedback(final Text message) {
        MinecraftClient.getInstance().inGameHud.getChatHud().addMessage(message);
    }

    public static void executeCommand(final MinecraftClient mc, final StringReader reader, final String command) {
        final var player = mc.player;
        if(player == null)
            return;
        try {
            player.networkHandler.getCommandDispatcher().execute(reader, new FakeCommandSource(mc, player));
        } catch (final CommandSyntaxException e) {
            ClientCommandManager.sendError(Texts.toText(e.getRawMessage()));
            if (e.getInput() != null && e.getCursor() >= 0) {
                final int cursor = Math.min(e.getCursor(), e.getInput().length());
                final MutableText text = Text.literal("").formatted(Formatting.GRAY)
                        .styled(style -> style.withClickEvent(new ClickEvent.SuggestCommand(command)));
                if (cursor > 10)
                    text.append("...");

                text.append(e.getInput().substring(Math.max(0, cursor - 10), cursor));
                if (cursor < e.getInput().length()) {
                    text.append(Text.literal(e.getInput().substring(cursor)).formatted(Formatting.RED, Formatting.UNDERLINE));
                }

                text.append(Text.translatable("command.context.here").formatted(Formatting.RED, Formatting.ITALIC));
                ClientCommandManager.sendError(text);
            }
        } catch (final Exception e) {
            final var error = Text.literal(e.getMessage() == null ? e.getClass().getName() : e.getMessage());
            ClientCommandManager.sendError(Text.translatable("command.failed")
                    .styled(style -> style.withHoverEvent(new HoverEvent.ShowText(error))));
            AutoLectern.LOGGER.error("An error occurred: {}", e.getMessage(), e);
        }
    }
}