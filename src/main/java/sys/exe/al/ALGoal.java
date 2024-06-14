package sys.exe.al;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.Nullable;

public record ALGoal(@Nullable RegistryEntry<Enchantment> enchant, @Nullable String enchant_id, int lvlMin, int lvlMax, int priceMin, int priceMax) {}