package sys.exe.al.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FakeCommandSource extends ClientSuggestionProvider {

    public final Minecraft mc;
    private final LocalPlayer player;
    public FakeCommandSource(final Minecraft mc, final LocalPlayer player) {
        super(Objects.requireNonNull(mc.getConnection()), mc, (perm) -> true);
        this.mc = mc;
        this.player = player;
    }

    @Override
    public @NotNull CompletableFuture<Suggestions> customSuggestion(@NotNull CommandContext<?> context) {
        return Suggestions.empty();
    }

    public void sendMessage(Component message) {
        this.player.displayClientMessage(message, false);
    }
    @Override
    public @NotNull Collection<String> getOnlinePlayerNames() {
        final var networkHandler = mc.getConnection();
        if(networkHandler == null)
            return new ArrayList<>();
        return networkHandler.getOnlinePlayers().stream().map(e -> e.getProfile().name()).collect(Collectors.toList());
    }
}