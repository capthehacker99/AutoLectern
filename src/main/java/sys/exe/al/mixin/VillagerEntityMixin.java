package sys.exe.al.mixin;

import org.spongepowered.asm.mixin.Unique;
import sys.exe.al.interfaces.ExtraVillagerData;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.village.VillagerProfession;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VillagerEntity.class)
public class VillagerEntityMixin implements ExtraVillagerData {
    @Unique
    public VillagerProfession prevProfession;

    public VillagerProfession autolec$getPrevProfession() {
        return prevProfession;
    }

    public void autolec$setPrevProfession(VillagerProfession pp) {
        prevProfession = pp;
    }
}
