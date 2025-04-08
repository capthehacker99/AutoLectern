package sys.exe.al;

import net.minecraft.client.input.Input;
import net.minecraft.util.math.Vec2f;

public final class DummyInput extends Input {
    public DummyInput() {
        this.movementVector = Vec2f.ZERO;
    }
}
