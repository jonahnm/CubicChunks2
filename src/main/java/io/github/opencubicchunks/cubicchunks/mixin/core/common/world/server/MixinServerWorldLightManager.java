package io.github.opencubicchunks.cubicchunks.mixin.core.common.world.server;

import com.mojang.datafixers.util.Pair;
import io.github.opencubicchunks.cubicchunks.chunk.IChunkManager;
import io.github.opencubicchunks.cubicchunks.chunk.ICube;
import io.github.opencubicchunks.cubicchunks.chunk.ticket.CubeTaskPriorityQueueSorter;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import io.github.opencubicchunks.cubicchunks.mixin.core.common.world.lighting.MixinWorldLightManager;
import io.github.opencubicchunks.cubicchunks.utils.Coords;
import io.github.opencubicchunks.cubicchunks.world.server.IServerWorldLightManager;
import it.unimi.dsi.fastutil.objects.ObjectList;
import net.minecraft.util.Util;
import net.minecraft.util.concurrent.ITaskExecutor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.LightType;
import net.minecraft.world.chunk.ChunkSection;
import net.minecraft.world.chunk.NibbleArray;
import net.minecraft.world.server.ChunkManager;
import net.minecraft.world.server.ServerWorldLightManager;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;
import java.util.function.IntSupplier;

import javax.annotation.Nullable;

@Mixin(ServerWorldLightManager.class)
public abstract class MixinServerWorldLightManager extends MixinWorldLightManager implements IServerWorldLightManager {

    private ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor;

    @Shadow @Final private ChunkManager chunkManager;

    @Shadow @Final private ObjectList<Pair<ServerWorldLightManager.Phase, Runnable>> field_215606_c;

    @Shadow private volatile int field_215609_f;

    @Shadow protected abstract void func_215603_b();

    @Override public void postConstructorSetup(CubeTaskPriorityQueueSorter sorter,
            ITaskExecutor<CubeTaskPriorityQueueSorter.FunctionEntry<Runnable>> taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    /**
     * @author NotStirred
     * @reason lambdas
     */
    @Overwrite
    public void checkBlock(BlockPos blockPosIn)
    {
        BlockPos blockpos = blockPosIn.toImmutable();
        this.schedulePhaseTask(Coords.blockToCube(blockPosIn.getX()), Coords.blockToCube(blockPosIn.getY()), Coords.blockToCube(blockPosIn.getZ()),
                ServerWorldLightManager.Phase.POST_UPDATE,
                Util.namedRunnable(() -> {
            super.checkBlock(blockpos);
        }, () -> {
            return "checkBlock " + blockpos;
        }));
    }

    // func_215586_a
    private void schedulePhaseTask(int cubePosX, int cubePosY, int cubePosZ, ServerWorldLightManager.Phase phase, Runnable runnable) {
        this.schedulePhaseTask(cubePosX, cubePosY, cubePosZ, ((IChunkManager)this.chunkManager).getCompletedLevel(CubePos.of(cubePosX, cubePosY,
                cubePosZ).asLong()), phase, runnable);
    }

    // func_215600_a
    private void schedulePhaseTask(int cubePosX, int cubePosY, int cubePosZ, IntSupplier getCompletedLevel, ServerWorldLightManager.Phase p_215600_4_,
            Runnable p_215600_5_) {
        this.taskExecutor.enqueue(CubeTaskPriorityQueueSorter.createMsg(() -> {
            this.field_215606_c.add(Pair.of(p_215600_4_, p_215600_5_));
            if (this.field_215606_c.size() >= this.field_215609_f) {
                this.func_215603_b();
            }

        }, CubePos.asLong(cubePosX, cubePosY, cubePosZ), getCompletedLevel));
    }

    // updateChunkStatus
    public void setCubeStatusEmpty(CubePos cubePos) {
        this.schedulePhaseTask(cubePos.getX(), cubePos.getY(), cubePos.getZ(), () -> {
            return 0;
        }, ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            super.retainData(cubePos, false);
            super.enableLightSources(cubePos, false);


            for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
                super.setData(LightType.BLOCK, Coords.sectionPosByIndex(cubePos, i), (NibbleArray)null);
                super.setData(LightType.SKY, Coords.sectionPosByIndex(cubePos, i), (NibbleArray)null);
            }

            for(int j = 0; j < ICube.CUBE_SIZE; ++j) {
                super.updateSectionStatus(Coords.sectionPosByIndex(cubePos, j), true);
            }

        }, () -> {
            return "updateCubeStatus " + cubePos + " " + true;
        }));
    }

    // lightChunk
    @Override
    public CompletableFuture<ICube> lightCube(ICube icube, boolean p_215593_2_) {
        CubePos cubePos = icube.getCubePos();
        icube.setCubeLight(false);
        this.schedulePhaseTask(cubePos.getX(), cubePos.getY(), cubePos.getZ(), ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            for(int i = 0; i < ICube.CUBE_SIZE; ++i) {
                ChunkSection chunksection = icube.getCubeSections()[i];
                if (!ChunkSection.isEmpty(chunksection)) {
                    super.updateSectionStatus(Coords.sectionPosByIndex(cubePos, i), false);
                }
            }

            super.enableLightSources(cubePos, true);
            if (!p_215593_2_) {
                icube.getCubeLightSources().forEach((blockPos) -> {
                    super.onBlockEmissionIncrease(blockPos, icube.getLightValue(blockPos));
                });
            }

            ((IChunkManager)this.chunkManager).releaseLightTicket(cubePos);
        }, () -> {
            return "lightCube " + cubePos + " " + p_215593_2_;
        }));
        return CompletableFuture.supplyAsync(() -> {
            icube.setCubeLight(true);
            super.retainData(cubePos, false);
            return icube;
        }, (runnable) -> {
            this.schedulePhaseTask(cubePos.getX(), cubePos.getY(), cubePos.getZ(), ServerWorldLightManager.Phase.POST_UPDATE, runnable);
        });
    }

    /**
     * @author
     */
    @Overwrite
    public void updateSectionStatus(SectionPos pos, boolean isEmpty) {
        this.schedulePhaseTask(pos.getSectionX(), pos.getSectionY(), pos.getSectionZ(), () -> {
            return 0;
        }, ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            super.updateSectionStatus(pos, isEmpty);
        }, () -> {
            return "updateSectionStatus " + pos + " " + isEmpty;
        }));
    }

    /**
     * @author NotStirred
     * @reason Vanilla lighting is gone
     */
    @Overwrite
    public void setData(LightType type, SectionPos pos, @Nullable NibbleArray array) {
        this.schedulePhaseTask(pos.getSectionX(), pos.getSectionY(), pos.getSectionZ(), () -> {
            return 0;
        }, ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            super.setData(type, pos, array);
        }, () -> {
            return "queueData " + pos;
        }));
    }

    //retainData(ChunkPos, bool)
    @Override
    public void retainData(CubePos pos, boolean retain) {
        this.schedulePhaseTask(pos.getX(), pos.getY(), pos.getZ(), () -> 0, ServerWorldLightManager.Phase.PRE_UPDATE, Util.namedRunnable(() -> {
            super.retainData(pos, retain);
        }, () -> "retainData " + pos));
    }
}
