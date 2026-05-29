package sys.exe.al.mixin;

import net.minecraft.core.Holder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Unique;
import sys.exe.al.interfaces.ExtraVillagerData;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Villager.class)
public class VillagerMixin implements ExtraVillagerData {
    @Unique
    public Holder<@NotNull VillagerProfession> prevProfession;

    public Holder<@NotNull VillagerProfession> autolec$getPrevProfession() {
        return prevProfession;
    }

    public void autolec$setPrevProfession(final Holder<@NotNull VillagerProfession> pp) {
        prevProfession = pp;
    }
}
