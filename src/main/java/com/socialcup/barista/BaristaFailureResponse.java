package com.socialcup.barista;

public record BaristaFailureResponse(
        String result,
        BaristaFailureReason reason
) {

    public static BaristaFailureResponse of(BaristaFailureReason reason) {
        return new BaristaFailureResponse("FAILURE", reason);
    }
}
