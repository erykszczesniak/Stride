package db.migration.common;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

import java.io.InputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

/**
 * Seeds {@code point_timestamps} for the sample activities (V3/V4).
 *
 * The seeded streams anchor to each activity's NOW()-relative
 * {@code start_time}: every element is {@code epoch(start_time) + offset}
 * with per-activity offsets generated deterministically alongside the V4
 * polylines/elevation profiles (ported from main's V19). PostgreSQL can
 * compute that in SQL ({@code string_agg} over {@code unnest ... WITH
 * ORDINALITY}) but H2 cannot, so this Java migration is the H2+PG-portable
 * form — it runs identically on both vendors.
 */
public class V5__Seed_sample_point_timestamps extends BaseJavaMigration {

    private static final String OFFSETS_RESOURCE = "db/seed/sample_point_timestamp_offsets.json";

    @Override
    public void migrate(Context context) throws Exception {
        Map<Long, List<Long>> offsetsByActivityId = loadOffsets();

        try (PreparedStatement select = context.getConnection()
                .prepareStatement("SELECT start_time FROM activities WHERE id = ?");
             PreparedStatement update = context.getConnection()
                     .prepareStatement("UPDATE activities SET point_timestamps = ? WHERE id = ?")) {

            for (Map.Entry<Long, List<Long>> entry : offsetsByActivityId.entrySet()) {
                Long activityId = entry.getKey();
                select.setLong(1, activityId);
                try (ResultSet rs = select.executeQuery()) {
                    if (!rs.next()) {
                        continue; // sample row not present — nothing to seed
                    }
                    Timestamp startTime = rs.getTimestamp(1);
                    long base = startTime.toLocalDateTime().toEpochSecond(ZoneOffset.UTC);
                    StringJoiner json = new StringJoiner(",", "[", "]");
                    for (Long offset : entry.getValue()) {
                        json.add(String.valueOf(base + offset));
                    }
                    update.setString(1, json.toString());
                    update.setLong(2, activityId);
                    update.executeUpdate();
                }
            }
        }
    }

    private Map<Long, List<Long>> loadOffsets() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(OFFSETS_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing classpath resource " + OFFSETS_RESOURCE);
            }
            return new ObjectMapper().readValue(in, new TypeReference<>() {});
        }
    }
}
