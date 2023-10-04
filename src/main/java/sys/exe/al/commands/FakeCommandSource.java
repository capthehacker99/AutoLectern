package sys.exe.al.commands;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.server.command.ServerCommandSource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

public class FakeCommandSource extends ServerCommandSource {

    public MinecraftClient mc;
    public FakeCommandSource(final MinecraftClient mc, final ClientPlayerEntity player) {
        super(player, player.getPos(), player.getRotationClient(), null, 0, player.getEntityName(), player.getName(), null, player);
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