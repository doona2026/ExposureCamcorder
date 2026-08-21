package io.github.exposure_camcorder.compatibility.mixin.fabric;

import io.github.exposure_camcorder.compatibility.exposure.PhotographScreenAccess;
import io.github.mortuusars.exposure.client.gui.screen.PhotographScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PhotographScreen.class)
public interface PhotographScreenAccessor extends PhotographScreenAccess {
    @Override
    @Accessor("photographs")
    java.util.ArrayList<io.github.mortuusars.exposure.world.item.util.ItemAndStack<io.github.mortuusars.exposure.world.item.PhotographItem>> exposureCamcorder$getPhotographs();

    @Override
    @Accessor("pager")
    io.github.mortuusars.exposure.client.gui.screen.element.Pager exposureCamcorder$getPager();

    @Override
    @Accessor("zoom")
    io.github.mortuusars.exposure.client.gui.component.SteppedZoom exposureCamcorder$getZoom();

    @Override
    @Invoker("setPhotographs")
    void exposureCamcorder$setPhotographs(java.util.List<io.github.mortuusars.exposure.world.item.util.ItemAndStack<io.github.mortuusars.exposure.world.item.PhotographItem>> photographs);
}
