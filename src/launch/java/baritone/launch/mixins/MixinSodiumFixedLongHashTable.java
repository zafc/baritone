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
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import net.minecraft.world.chunk.WorldChunk;
import org.spongepowered.asm.mixin.*;

import java.util.concurrent.atomic.AtomicReferenceArray;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.util.collections.FixedLongHashTable", remap = false)
public abstract class MixinSodiumFixedLongHashTable implements ISodiumChunkArray {
    
    @Shadow
    public abstract ObjectIterator<Long2ObjectMap.Entry<Object>> iterator();
    
    @Shadow
    public abstract Object put(final long k, final Object v);
    
    @Override
    public void copyFrom(IChunkArray other) {
        if (other instanceof ISodiumChunkArray) {
            ObjectIterator<Long2ObjectMap.Entry<Object>> it = ((ISodiumChunkArray) other).callIterator();
            while (it.hasNext()) {
                Long2ObjectMap.Entry<Object> entry = it.next();
                this.put(entry.getLongKey(), entry.getValue());
            }
        } else {
            throw new RuntimeException("Unimplemented");
        }
    }
    
    @Override
    public ObjectIterator<Long2ObjectMap.Entry<Object>> callIterator() {
        return iterator();
    }
    
    
    @Override
    public AtomicReferenceArray<WorldChunk> getChunks() {
        throw new RuntimeException("Unimplemented");
    }
    
    @Override
    public int centerX() {
        throw new RuntimeException("Unimplemented");
    }
    
    @Override
    public int centerZ() {
        throw new RuntimeException("Unimplemented");
    }
    
    @Override
    public int viewDistance() {
        throw new RuntimeException("Unimplemented");
    }
    
}
