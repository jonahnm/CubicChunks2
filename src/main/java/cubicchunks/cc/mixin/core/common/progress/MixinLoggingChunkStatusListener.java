package cubicchunks.cc.mixin.core.common.progress;

import cubicchunks.cc.chunk.ISectionStatusListener;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.SectionPos;
import net.minecraft.world.chunk.ChunkStatus;
import net.minecraft.world.chunk.listener.LoggingChunkStatusListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import javax.annotation.Nullable;

@Mixin(LoggingChunkStatusListener.class)
public abstract class MixinLoggingChunkStatusListener implements ISectionStatusListener {

    @Shadow public abstract void statusChanged(ChunkPos chunkPosition, @Nullable ChunkStatus newStatus);

    @Override public void sectionStatusChanged(SectionPos chunkPosition, @Nullable ChunkStatus newStatus) {
        //this.statusChanged(chunkPosition.asChunkPos(), newStatus);
    }
}
