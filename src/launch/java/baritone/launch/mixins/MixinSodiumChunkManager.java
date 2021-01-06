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
import baritone.utils.accessor.IClientChunkProvider;
import baritone.utils.accessor.ISodiumChunkArray;
import net.minecraft.client.world.ClientChunkManager;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.locks.StampedLock;

@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.world.SodiumChunkManager")
public abstract class MixinSodiumChunkManager extends ClientChunkManager implements IClientChunkProvider {
    
    
    @Shadow(remap = false)
    @Final
    private StampedLock lock;
    
    @Shadow(remap = false)
    private int centerX;
    
    @Shadow(remap = false)
    private int centerZ;
    
    @Shadow(remap = false)
    private int radius;
    
    @Shadow(remap = false)
    @Final
    private ClientWorld world;
    
    @Override
    public ClientChunkManager createThreadSafeCopy() {
    
        //we're just gonna aquire a read lock for this whole operation
        long stamp = this.lock.readLock();
    
        try {
            ISodiumChunkArray refArr = extractReferenceArray();
            refArr.putCenterX(centerX);
            refArr.putCenterZ(centerZ);
            refArr.putViewDistance(radius);
            ClientChunkManager result = new ClientChunkManager(world, radius - 3); // -3 because its adds 3 for no reason lmao
            IChunkArray copyArr = ((IClientChunkProvider) result).extractReferenceArray();
            copyArr.copyFrom(refArr);
            if (copyArr.viewDistance() != refArr.viewDistance()) {
                throw new IllegalStateException(copyArr.viewDistance() + " " + refArr.viewDistance());
            }
            return result;
        } finally {
            // put this in finally so we can't break anything.
            this.lock.unlockRead(stamp);
        }
    
    }
    @Override
    public ISodiumChunkArray extractReferenceArray() {
        try {
            for (Field f : Class.forName("me.jellysquid.mods.sodium.client.world.SodiumChunkManager").getDeclaredFields()) {
                if (ISodiumChunkArray.class.isAssignableFrom(f.getType())) {
                    try {
                        return (ISodiumChunkArray) f.get(this);
                    } catch (IllegalAccessException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            throw new RuntimeException(Arrays.toString(Class.forName("me.jellysquid.mods.sodium.client.world.SodiumChunkManager").getDeclaredFields()));
        
        //since it's the class we're mixin'ing to this should never be raised and shouldn't become a problem
        } catch (ClassNotFoundException ignored) {}
        return null;
    }
    
    
    public MixinSodiumChunkManager(ClientWorld world, int loadDistance) {
        super(world, loadDistance);
    }
    
    
}
