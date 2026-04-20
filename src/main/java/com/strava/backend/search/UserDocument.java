package com.strava.backend.search;

import com.strava.backend.domain.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.DateFormat;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.InnerField;
import org.springframework.data.elasticsearch.annotations.MultiField;

import java.time.LocalDateTime;

/**
 * Elasticsearch projection of a {@link User} used purely for full-text search.
 * This is a read-only search view — the User JPA entity remains the source of truth.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(indexName = "users", createIndex = false)
public class UserDocument {

    @Id
    @Field(type = FieldType.Keyword)
    private Long id;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String firstName;

    @MultiField(
            mainField = @Field(type = FieldType.Text),
            otherFields = {@InnerField(suffix = "keyword", type = FieldType.Keyword)}
    )
    private String lastName;

    @Field(type = FieldType.Text)
    private String fullName;

    /**
     * Exact-match only — we deliberately do not expose email to free-text search for privacy.
     */
    @Field(type = FieldType.Keyword)
    private String email;

    @Field(type = FieldType.Text)
    private String bio;

    @Field(type = FieldType.Text)
    private String city;

    @Field(type = FieldType.Keyword)
    private String country;

    @Field(type = FieldType.Boolean)
    private Boolean hideFromSearch;

    @Field(type = FieldType.Date, format = DateFormat.date_hour_minute_second_millis)
    private LocalDateTime createdAt;

    /**
     * Map a JPA {@link User} to its Elasticsearch projection.
     * Computes {@code fullName} for boosted matching at query time.
     */
    public static UserDocument fromUser(User user) {
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        return UserDocument.builder()
                .id(user.getId())
                .firstName(firstName)
                .lastName(lastName)
                .fullName((firstName == null ? "" : firstName) + " " + (lastName == null ? "" : lastName))
                .email(user.getEmail())
                .bio(user.getBio())
                .city(user.getCity())
                .country(user.getCountry())
                .hideFromSearch(Boolean.TRUE.equals(user.getHideFromSearch()))
                .createdAt(user.getCreatedAt())
                .build();
    }
}
