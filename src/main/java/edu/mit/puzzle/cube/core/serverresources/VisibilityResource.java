package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Visibility;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class VisibilityResource extends AbstractCubeResource {

    private String getTeamId() {
        String idString = (String) getRequest().getAttributes().get("teamId");
        if (idString == null) {
            throw new IllegalArgumentException("teamId must be specified");
        }
        return idString;
    }

    private String getPuzzleId() {
        String idString = (String) getRequest().getAttributes().get("puzzleId");
        if (idString == null) {
            throw new IllegalArgumentException("puzzleId must be specified");
        }
        return idString;
    }

    @Get
    public Visibility handleGet() throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();
        return Visibility.builder()
                .setTeamId(teamId)
                .setPuzzleId(puzzleId)
                .setStatus(huntStatusStore.getVisibility(teamId, puzzleId))
                .build();
    }

    @Post
    public PostResult handlePost(Visibility visibility) throws JsonProcessingException {
        String teamId = getTeamId();
        String puzzleId = getPuzzleId();

        if (visibility.getStatus() == null
                || !huntStatusStore.getVisibilityStatusSet().isAllowedStatus(visibility.getStatus())) {
            return PostResult.builder().setUpdated(false).build();
        }

        boolean changed = huntStatusStore.setVisibility(teamId, puzzleId, visibility.getStatus(), true);
        return PostResult.builder().setUpdated(changed).build();
    }
}
