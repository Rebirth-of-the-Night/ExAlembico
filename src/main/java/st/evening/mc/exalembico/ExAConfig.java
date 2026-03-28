package st.evening.mc.exalembico;

import net.minecraftforge.common.config.Config;

// needs to be a java class so the forge config system doesn't pick up the kotlin object instance field
@Config(modid = ExAlembicoConsts.MOD_ID)
public class ExAConfig {
    @Config.Comment("Zen: Foundry temperatures, in centikelvins, for each heater heat level.")
    @Config.RangeInt(min = 1)
    public static int[] zenFoundryHeaterTemps = {
            100000, 200000, 300000, 400000, 500000, 600000, 700000, 800000,
            900000, 1000000, 1100000, 1200000, 1300000, 1400000, 1500000,
            1600000, 1700000, 1800000, 1900000, 2000000, 2100000, 2200000, 2300000,
            2400000, 2500000, 2600000, 2700000, 2800000, 2900000, 3000000
    };

    @Config.Comment("The light level of each heater block state.")
    @Config.RangeInt(min = 0, max = 15)
    public static int[] heaterLightLevels = {
            0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15
    };

    @Config.Comment("Should the nether alembic inherit recipes from the alembic?")
    @Config.RequiresMcRestart
    public static boolean netherAlembicInherit = true;
}
