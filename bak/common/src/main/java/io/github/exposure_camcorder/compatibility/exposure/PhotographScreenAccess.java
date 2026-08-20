package io.github.exposure_camcorder.compatibility.exposure;

import io.github.mortuusars.exposure.client.gui.component.SteppedZoom;
import io.github.mortuusars.exposure.client.gui.screen.element.Pager;
import io.github.mortuusars.exposure.world.item.PhotographItem;
import io.github.mortuusars.exposure.world.item.util.ItemAndStack;

import java.util.ArrayList;
import java.util.List;

public interface PhotographScreenAccess {
    ArrayList<ItemAndStack<PhotographItem>> exposureCamcorder$getPhotographs();

    Pager exposureCamcorder$getPager();

    SteppedZoom exposureCamcorder$getZoom();

    void exposureCamcorder$setPhotographs(List<ItemAndStack<PhotographItem>> photographs);
}
