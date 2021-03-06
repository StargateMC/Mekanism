package mekanism.generators.common.tile.turbine;

import java.util.Collections;
import javax.annotation.Nonnull;
import mekanism.common.capabilities.holder.fluid.IFluidTankHolder;
import mekanism.common.tile.base.SubstanceType;
import mekanism.common.util.PipeUtils;
import mekanism.generators.common.registries.GeneratorsBlocks;

public class TileEntityTurbineVent extends TileEntityTurbineCasing {

    public TileEntityTurbineVent() {
        super(GeneratorsBlocks.TURBINE_VENT);
    }

    @Nonnull
    @Override
    protected IFluidTankHolder getInitialFluidTanks() {
        return side -> structure == null ? Collections.emptyList() : structure.ventTanks;
    }

    @Override
    protected void onUpdateServer() {
        super.onUpdateServer();
        if (structure != null) {
            PipeUtils.emit(structure.ventTank, this);
        }
    }

    @Override
    public boolean persists(SubstanceType type) {
        //Do not handle fluid when it comes to syncing it/saving this tile to disk
        if (type == SubstanceType.FLUID) {
            return false;
        }
        return super.persists(type);
    }
}