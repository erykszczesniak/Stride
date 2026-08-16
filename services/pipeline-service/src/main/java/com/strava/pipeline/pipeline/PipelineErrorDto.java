package com.strava.pipeline.pipeline;

/** Error body of the pipeline contract: {"code": string, "message": string}. */
public record PipelineErrorDto(String code, String message) {
}
