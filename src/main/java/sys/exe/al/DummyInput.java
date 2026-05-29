package sys.exe.al;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;

public final class DummyInput extends ClientInput {
    public DummyInput() {
        this.moveVector = Vec2.ZERO;
    }
}
