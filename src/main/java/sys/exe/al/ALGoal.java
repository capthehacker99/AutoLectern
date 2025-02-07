package sys.exe.al;

import net.minecraft.util.Identifier;

public record ALGoal(Identifier enchant, int lvlMin, int lvlMax, int priceMin, int priceMax) {}