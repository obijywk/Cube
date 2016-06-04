package edu.mit.puzzle.cube.core.serverresources;

import edu.mit.puzzle.cube.core.model.VisibilityChanges;

import org.restlet.resource.Get;

public class VisibilityChangesResource extends AbstractCubeResource {

    @Get
    public VisibilityChanges handleGet() {
        return VisibilityChanges.builder()
                .setVisibilityChanges(huntStatusStore.getVisibilityChanges())
                .build();
    }
}
