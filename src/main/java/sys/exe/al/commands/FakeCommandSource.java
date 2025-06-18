package sys.exe.al.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientCommandSource;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class FakeCommandSource extends ClientCommandSource {

    public final MinecraftClient mc;
    private final ClientPlayerEntity player;
    public FakeCommandSource(final MinecraftClient mc, final ClientPlayerEntity player) {
        super(mc.getNetworkHandler(), mc, true);
//        super(new CommandOutput() {
//            @Override
//            public void sendMessage(Text message) {
//                player.sendMessage(message, false);
//            }
//
//            @Override
//            public boolean shouldReceiveFeedback() {
//                return true;
//            }
//
//            @Override
//            public boolean shouldTrackOutput() {
//                return false;
//            }
//
//            @Override
//            public boolean shouldBroadcastConsoleToOps() {
//                return false;
//            }
//        }, player.getPos(), player.getRotationClient(), null, 0, player.getNameForScoreboard(), player.getName(), null, player);
        this.mc = mc;
        this.player = player;
    }

    @Override
    public CompletableFuture<Suggestions> getCompletions(CommandContext<?> context) {
        return Suggestions.empty();
    }

    public void sendMessage(Text message) {
        this.player.sendMessage(message, false);
    }
    @Override
    public Collection<String> getPlayerNames() {
        final var networkHandler = mc.getNetworkHandler();
        if(networkHandler == null)
            return new ArrayList<>();
        return networkHandler.getPlayerList().stream().map(e -> e.getProfile().getName()).collect(Collectors.toList());
    }
}