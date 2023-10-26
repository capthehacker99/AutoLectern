package sys.exe.al;

import net.minecraft.enchantment.Enchantment;

public record ALGoal(Enchantment enchant, int lvlMin, int lvlMax, int priceMin, int priceMax) {}