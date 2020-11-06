package io.github.opencubicchunks.cubicchunks.mixin.core.common.chunk;

import io.github.opencubicchunks.cubicchunks.chunk.ICubeHolderListener;
import io.github.opencubicchunks.cubicchunks.chunk.util.CubePos;
import org.spongepowered.asm.mixin.Mixin;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import net.minecraft.server.level.ChunkHolder;

@Mixin(ChunkHolder.LevelChangeListener.class)
public interface MixinChunkHolderListener extends ICubeHolderListener {

    @Override default void onCubeLevelChange(CubePos pos, IntSupplier intSupplier, int p_219066_3_, IntConsumer p_219066_4_) {
    }
}