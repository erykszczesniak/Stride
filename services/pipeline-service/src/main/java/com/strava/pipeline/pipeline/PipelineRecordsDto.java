package com.strava.pipeline.pipeline;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * GET /pipeline/v1/records payload. {@link Value} is the contract's tagged
 * union: exactly one of {@code time}/{@code power} is present
 * ({@code {"time":{"seconds":1152}}} or {@code {"power":{"watts":286}}}).
 */
public record PipelineRecordsDto(List<RecordItem> records) {

    public record RecordItem(
            String id,
            String discipline,
            String eventLabel,
            Value value,
            OffsetDateTime date,
            boolean isNew,
            @JsonInclude(JsonInclude.Include.NON_NULL) Value previousBest,
            List<Double> progression
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Value(Time time, Power power) {

        public static Value ofSeconds(int seconds) {
            return new Value(new Time(seconds), null);
        }

        public static Value ofWatts(int watts) {
            return new Value(null, new Power(watts));
        }

        public record Time(int seconds) {
        }

        public record Power(int watts) {
        }
    }
}
