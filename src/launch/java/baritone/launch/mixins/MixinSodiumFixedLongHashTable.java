/*
 * This file is part of Baritone.
 *
 * Baritone is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Baritone is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Baritone.  If not, see <https://www.gnu.org/licenses/>.
 */

package baritone.launch.mixins;

import baritone.utils.accessor.IChunkArray;
import baritone.utils.accessor.ISodiumChunkArray;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.util.collections.FixedLongHashTable", remap = false)
public class MixinSodiumFixedLongHashTable implements ISodiumChunkArray {
    
    @Unique
    private int centerX = 0;
    
    @Unique
    private int centerZ = 0;
    
    @Unique
    private int dist = 0;
    
    @Shadow
    @Final
    protected Object[] value;
    
    // this will be un-used because we're copying to a vanilla chunk array.
    @Override
    public void copyFrom(IChunkArray other) {
        throw new RuntimeException("Unimplemented");
    }
    
    @Override
    public AtomicReferenceArray<WorldChunk> getChunks() {
        //either this or find a way to cast an [Ljava.lang.Object;
        WorldChunk[] chunks = new WorldChunk[value.length];
        for (int i = 0; i < value.length; ++i) {
            chunks[i] = (WorldChunk) value[i];
        }
        return new AtomicReferenceArray<>(chunks);
    }
    
    @Override
    public int centerX() {
        return centerX;
    }
    
    @Override
    public int centerZ() {
        return centerZ;
    }
    
    @Override
    public int viewDistance() {
        return dist;
    }
    
    @Override
    public void putCenterX(int x) {
        this.centerX = x;
    }
    
    @Override
    public void putCenterZ(int z) {
        this.centerZ = z;
    }
    
    @Override
    public void putViewDistance(int dist) {
        this.dist = dist;
    }
    
    
}
