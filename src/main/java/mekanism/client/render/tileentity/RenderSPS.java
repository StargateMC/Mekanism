package mekanism.client.render.tileentity;

import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import com.google.common.base.Objects;
import com.mojang.blaze3d.matrix.MatrixStack;
import mekanism.client.render.bolt.BoltEffect;
import mekanism.client.render.bolt.BoltRenderer;
import mekanism.client.render.bolt.BoltRenderer.BoltData;
import mekanism.client.render.bolt.BoltRenderer.SpawnFunction;
import mekanism.client.render.custom.BillboardingEffectRenderer;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.sps.SPSMultiblockData;
import mekanism.common.content.sps.SPSMultiblockData.CoilData;
import mekanism.common.lib.Color;
import mekanism.common.lib.math.Plane;
import mekanism.common.lib.math.voxel.VoxelCuboid;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidSide;
import mekanism.common.particle.custom.CustomEffect;
import mekanism.common.particle.custom.SPSOrbitEffect;
import mekanism.common.tile.multiblock.TileEntitySPSCasing;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ParametersAreNonnullByDefault
public class RenderSPS extends MekanismTileEntityRenderer<TileEntitySPSCasing> {

    private static final CustomEffect CORE = new CustomEffect(MekanismUtils.getResource(ResourceType.RENDER, "energy_effect.png"));
    private static final Random rand = new Random();
    private static float MIN_SCALE = 0.5F, MAX_SCALE = 4F;

    private Minecraft minecraft = Minecraft.getInstance();
    private BoltRenderer bolts = BoltRenderer.create(BoltEffect.ELECTRICITY, 12, SpawnFunction.delay(6));
    private BoltRenderer edgeBolts = BoltRenderer.create(BoltEffect.ELECTRICITY, 8, SpawnFunction.NO_DELAY);

    public RenderSPS(TileEntityRendererDispatcher renderer) {
        super(renderer);
        CORE.setColor(Color.rgba(255, 255, 255, 240));
    }

    @Override
    protected void render(TileEntitySPSCasing tile, float partialTick, MatrixStack matrix, IRenderTypeBuffer renderer, int light, int overlayLight, IProfiler profiler) {
        if (tile.isMaster && tile.getMultiblock().isFormed() && tile.getMultiblock().renderLocation != null) {
            Vec3d center = new Vec3d(tile.getMultiblock().minLocation).add(new Vec3d(tile.getMultiblock().maxLocation)).add(new Vec3d(1, 1, 1)).scale(0.5);
            Vec3d renderCenter = center.subtract(tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
            if (!minecraft.isGamePaused()) {
                for (CoilData data : tile.getMultiblock().coilData.coilMap.values()) {
                    if (data.prevLevel > 0) {
                        bolts.update(data.coilPos.hashCode(), getBoltFromData(data, tile.getPos(), tile.getMultiblock(), renderCenter), partialTick);
                    }
                }
            }

            int targetEffectCount = 0;

            if (!minecraft.isGamePaused() && !tile.getMultiblock().lastReceivedEnergy.isZero()) {
                double rate = Math.log10(tile.getMultiblock().lastReceivedEnergy.doubleValue());
                if (rand.nextDouble() < (rate / 16F)) {
                    CuboidSide side = CuboidSide.SIDES[rand.nextInt(6)];
                    Plane plane = Plane.getInnerCuboidPlane(new VoxelCuboid(tile.getMultiblock().minLocation, tile.getMultiblock().maxLocation), side);
                    Vec3d endPos = plane.getRandomPoint(rand).subtract(tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
                    BoltData data = new BoltData(renderCenter, endPos, 1, 15, 0.01F * (float) rate);
                    edgeBolts.update(Objects.hashCode(side.hashCode(), endPos.hashCode()), data, partialTick);
                }
                targetEffectCount = (int) (rate * 20);
            }

            if (tile.orbitEffects.size() > targetEffectCount) {
                tile.orbitEffects.poll();
            } else if (tile.orbitEffects.size() < targetEffectCount && rand.nextDouble() < 0.2) {
                tile.orbitEffects.add(new SPSOrbitEffect(tile.getMultiblock(), center));
            }

            bolts.render(partialTick, matrix, renderer);
            edgeBolts.render(partialTick, matrix, renderer);

            tile.orbitEffects.forEach(effect -> BillboardingEffectRenderer.render(effect, tile.getPos(), matrix, renderer, tile.getWorld().getGameTime(), partialTick));

            if (tile.getMultiblock().lastProcessed > 0) {
                double scaledEnergy = (Math.log10(tile.getMultiblock().lastProcessed) + 4) / 5D;
                float scale = MIN_SCALE + (float)Math.min(1, Math.max(0, scaledEnergy)) * (MAX_SCALE - MIN_SCALE);
                CORE.setPos(center);
                CORE.setScale(scale);
                BillboardingEffectRenderer.render(CORE, tile.getPos(), matrix, renderer, tile.getWorld().getGameTime(), partialTick);
            }
        }
    }

    private static BoltData getBoltFromData(CoilData data, BlockPos pos, SPSMultiblockData multiblock, Vec3d center) {
        Vec3d start = new Vec3d(data.coilPos.offset(data.side)).add(0.5, 0.5, 0.5);
        start = start.add(new Vec3d(data.side.getDirectionVec()).scale(0.5));
        int count = 1 + (data.prevLevel - 1) / 2;
        float size = 0.01F * data.prevLevel;
        return new BoltData(start.subtract(pos.getX(), pos.getY(), pos.getZ()), center, count, 10, size);
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.SPS;
    }

    @Override
    public boolean isGlobalRenderer(TileEntitySPSCasing tile) {
        return tile.isMaster && tile.getMultiblock().isFormed() && tile.getMultiblock().renderLocation != null;
    }
}