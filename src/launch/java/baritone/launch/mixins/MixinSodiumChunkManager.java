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
import org.spongepowered.asm.mixin.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
    
    @Unique
    private static Constructor<?> sodiumChunkManagerConstructor = null;
    
    @Unique
    private static Field sodiumChunkArrayField = null;
    
    @Override
    public ClientChunkManager createThreadSafeCopy() {
    
        // we're just gonna acquire a write lock for this whole operation so it can't write any chunks.
        // see https://github.com/jellysquid3/sodium-fabric/blob/d3528521d48a130322c910c6f0725cf365ebae6f/src/main/java/me/jellysquid/mods/sodium/client/world/SodiumChunkManager.java#L139
        long stamp = this.lock.writeLock();
    
        try {
            ISodiumChunkArray refArr = extractReferenceArray();
            if (sodiumChunkManagerConstructor == null) {
                sodiumChunkManagerConstructor = this.getClass().getConstructor(ClientWorld.class, int.class);
            }
            ClientChunkManager result = (ClientChunkManager) sodiumChunkManagerConstructor.newInstance(world, radius - 3); // -3 because it adds 3 for no reason lmao
            IChunkArray copyArr = ((IClientChunkProvider) result).extractReferenceArray();
            copyArr.copyFrom(refArr);
            return result;
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException("Sodium chunk manager initialization for baritone failed", e);
        } finally {
            // put this in finally so we can't break anything.
            this.lock.unlockWrite(stamp);
        }
    }
    @Override
    public ISodiumChunkArray extractReferenceArray() {
        if (sodiumChunkArrayField == null) {
            boolean flag = true;
            for (Field f : this.getClass().getDeclaredFields()) {
                if (ISodiumChunkArray.class.isAssignableFrom(f.getType())) {
                    sodiumChunkArrayField = f;
                    flag = false;
                    break;
                }
            } //else
            if (flag) {
                throw new RuntimeException(Arrays.toString(this.getClass().getDeclaredFields()));
            }
        }
        try {
            return (ISodiumChunkArray) sodiumChunkArrayField.get(this);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
    
    
    public MixinSodiumChunkManager(ClientWorld world, int loadDistance) {
        super(world, loadDistance);
    }
    
    
}
