package edu.mit.puzzle.cube.core.serverresources;

import com.fasterxml.jackson.core.JsonProcessingException;

import edu.mit.puzzle.cube.core.model.PostResult;
import edu.mit.puzzle.cube.core.model.Submission;
import edu.mit.puzzle.cube.core.model.Submissions;

import org.restlet.resource.Get;
import org.restlet.resource.Post;

public class SubmissionsResource extends AbstractCubeResource {

    @Get
    public Submissions handleGet() {
        return Submissions.builder()
                .setSubmissions(submissionStore.getAllSubmissions())
                .build();
    }

    @Post
    public PostResult handlePost(Submission submission) throws JsonProcessingException {
        String visibilityStatus = huntStatusStore.getVisibility(
                submission.getTeamId(),
                submission.getPuzzleId());
        if (!huntStatusStore.getVisibilityStatusSet().allowsSubmissions(visibilityStatus)) {
            return PostResult.builder().setCreated(false).build();
        }

        boolean success = submissionStore.addSubmission(submission);
        return PostResult.builder().setCreated(success).build();
    }
}
