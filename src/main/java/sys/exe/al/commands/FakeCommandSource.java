package sys.exe.al.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.command.CommandOutput;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class FakeCommandSource extends ServerCommandSource {

    public final MinecraftClient mc;
    public FakeCommandSource(final MinecraftClient mc, final ClientPlayerEntity player) {
        super(new CommandOutput() {
            @Override
            public void sendMessage(Text message) {
                player.sendMessage(message, false);
            }

            @Override
            public boolean shouldReceiveFeedback() {
                return true;
            }

            @Override
            public boolean shouldTrackOutput() {
                return false;
            }

            @Override
            public boolean shouldBroadcastConsoleToOps() {
                return false;
            }
        }, player.getPos(), player.getRotationClient(), null, 0, player.getNameForScoreboard(), player.getName(), null, player);
        this.mc = mc;
    }

    @Override
    public Collection<String> getPlayerNames() {
        final var networkHandler = MinecraftClient.getInstance().getNetworkHandler();
        if(networkHandler == null)
            return new ArrayList<>();
        return networkHandler.getPlayerList().stream().map(e -> e.getProfile().getName()).collect(Collectors.toList());
    }
}