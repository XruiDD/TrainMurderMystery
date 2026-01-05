package dev.doctor4t.wathe.compat.sodium;

import net.caffeinemc.mods.sodium.client.render.chunk.RenderSection;
import net.caffeinemc.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.HashSet;
import java.util.Set;

public class SceneryOcclusionVisitor implements OcclusionCuller.Visitor {

    public final OcclusionCuller.Visitor visitor;

    public Set<Long> visitedChunk = new HashSet<>();

    public SceneryOcclusionVisitor(OcclusionCuller.Visitor originVisitor)
    {
        visitor = originVisitor;
    }

    @Override
    public void visit(RenderSection renderSection) {
        var key = ChunkSectionPos.asLong(renderSection.getChunkX(),renderSection.getChunkY(),renderSection.getChunkZ());
        if(!visitedChunk.contains(key)){
            visitedChunk.add(key);
            visitor.visit(renderSection);
        }
    }
}
