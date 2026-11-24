package com.maneszn.styles.mixinopt;

import net.minecraft.client.gui.components.debugchart.ProfilerPieChart;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin({ProfilerPieChart.class})
public interface ProfilerPieChartAccess {
   @Accessor("profilerPieChartResults")
   ProfileResults maneszn$results();

   @Accessor("profilerTreePath")
   void maneszn$setPath(String var1);

   @Accessor("profilerTreePath")
   String maneszn$path();
}
