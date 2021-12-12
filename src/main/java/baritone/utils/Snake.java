package baritone.utils;

import baritone.api.BaritoneAPI;
import baritone.api.behavior.IPathingBehavior;
import baritone.api.pathing.goals.GoalXZ;
import baritone.api.pathing.path.IPathExecutor;
import baritone.api.process.PathingCommand;
import baritone.api.process.PathingCommandType;
import baritone.api.utils.BetterBlockPos;
import baritone.api.utils.IPlayerContext;
import baritone.api.utils.RandomSpotNearby;
import net.minecraft.world.level.ChunkPos;

import java.util.*;

@SuppressWarnings("ALL")
@Deprecated
/**
 * Not used because Trail.java is better
 */
public final class Snake {
    private final Map<BetterBlockPos, Pack> vectors;
    private final Queue<Pack> traversedChunks;
    private BetterBlockPos prev;
    private final BlockStateInterface bsi;
    private final IPlayerContext player;
    private final static int VECTOR_LIMIT = 400;
    private final static int CROSS_LIMIT = 20;
    private final static int CHUNK_TAIL_MAX = 4*4;

    public Snake() {
        this.vectors = new HashMap<>();
        this.player = BaritoneAPI.getProvider().getPrimaryBaritone().getPlayerContext();
        this.bsi = new BlockStateInterface(this.player);
        this.traversedChunks = new ArrayDeque<>();
    }

    private boolean equalPos(final BetterBlockPos pos1, final BetterBlockPos pos2) {
        if (isNull(pos1) || isNull(pos2)) return false;
        return pos1.getX() == pos2.getX() && pos1.getY() == pos2.getY() && pos1.getZ() == pos2.getZ();
    }


    private boolean contains(final BetterBlockPos pos) {
        return this.vectors.keySet().stream().anyMatch(e -> equalPos(e, pos));
    }

    public boolean passedLimits() {
        return /*this.vectors.size() > VECTOR_LIMIT || */vectors.entrySet().stream().anyMatch(e -> e.getValue().crossings > CROSS_LIMIT);
    }

    private BetterBlockPos getOrigRef(final BetterBlockPos pos) {
        final Optional<BetterBlockPos> opt = vectors.keySet().stream().filter(e -> equalPos(e, pos)).findFirst();
        if (opt.isPresent()) {
            return opt.get();
        }
        return null;
    }

    private boolean isSameChunk(final ChunkPos c1, final ChunkPos c2) {
        return c1.x == c2.x && c1.z == c2.z;
    }

    public void reset() {
        traversedChunks.clear();
        vectors.clear();
        prev = null;
        pathing = false;
        stuckPos = null;
        targetPos = null;
    }

    private boolean removeOutsiders() {
        return this.vectors.entrySet().removeIf(e -> !traversedChunks.stream().anyMatch(c -> isSameChunk(e.getValue().chunkPos, c.chunkPos)));
    }

    private boolean pathing = false;
    private BetterBlockPos stuckPos;
    private RandomSpotNearby randSpot = new RandomSpotNearby(16d);
    private BetterBlockPos targetPos;

    public boolean activateRunAway() {
        final boolean old = pathing;
        pathing = true;
        return old;
    }

    public boolean isRunAwayActive() {
        return pathing;
    }

    public PathingCommand getRunAwayCommand() {
        final BetterBlockPos curr = getCurrent();
        if (isNull(curr)) return null;

        if (!pathing) {
            stuckPos = getCurrent();
            targetPos = randSpot.next(stuckPos);
            pathing = true;
        }

        if (curr.closerThan(targetPos, 3d)) {
            reset();
            return null; //new PathingCommand(null, PathingCommandType.DEFER);
        }
        return new PathingCommand(new GoalXZ(targetPos), PathingCommandType.FORCE_REVALIDATE_GOAL_AND_PATH);
    }

    public void tick() {
        final BetterBlockPos curr = getCurrent();
        if (isNull(curr) || equalPos(prev, curr)) return;
        prev = curr;

        final ChunkPos chunkPos = this.player.world().getChunk(curr).getPos();
        if (!traversedChunks.stream().anyMatch(e -> isSameChunk(e.chunkPos, chunkPos))) {
            traversedChunks.add(new Pack(chunkPos));
            //TODO: save block pos index list in their chunk. if the chunk gets removed
            // just iterate over it and remove that element in vectors over that index.
            // Iteration steps can be cut down to vectors.size()/CHUNK_TAIL_MAX + CHUNK_TAIL_MAX
            while (traversedChunks.size() > CHUNK_TAIL_MAX) traversedChunks.poll();
        }

        if (contains(curr)) {
            final BetterBlockPos orig = getOrigRef(curr);
            final Pack pack = this.vectors.get(orig);
            pack.crossings++;
            this.vectors.put(orig, pack);
        } else {
            final Pack pack = new Pack(chunkPos);
            vectors.put(curr, pack);
        }

        if (this.vectors.size() > VECTOR_LIMIT) this.vectors.clear();
        removeOutsiders();
    }

    /**
     * Call only after tick.
     */
    public void printCurrent() {
        BetterBlockPos playerPos = getCurrent();
        if (isSet(playerPos) && this.vectors.size() > 0) {
            System.out.println(playerPos.getX() + ":" + playerPos.getY() + ":" + playerPos.getZ() + " Counts: "
                    + this.vectors.get(getOrigRef(playerPos)).crossings + " Size: " + this.vectors.size());
        }
    }

    private static boolean isSet(final Object o) {
        return o != null;
    }

    private static boolean isNull(final Object o) { return !isSet(o);}

    private static BetterBlockPos getCurrent() {
        final IPathingBehavior p = BaritoneAPI.getProvider().getPrimaryBaritone().getPathingBehavior();
        if (isNull(p)) return null;
        final IPathExecutor c = p.getCurrent();
        if (isNull(c)) return null;
        final List<BetterBlockPos> bbp = c.getPath().positions();
        if (isNull(bbp)) return null;
        final int posIndex = c.getPosition();
        if (c.getPath().positions().size() <= posIndex) return null;
        final BetterBlockPos pos = c.getPath().positions().get(posIndex);
        return pos;
    }

    private class Pack {
        public Pack(final ChunkPos chunkPos) {
            this.crossings = 1;
            this.chunkPos = chunkPos;
        }

        public ChunkPos chunkPos;
        public int crossings;
    }
}
