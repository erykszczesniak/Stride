package com.strava.backend.search;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * Spring Data Elasticsearch repository for {@link UserDocument}.
 *
 * <p>The custom {@code searchByQuery} uses a {@code bool} / {@code must} /
 * {@code must_not} combination so that users with {@code hideFromSearch = true}
 * never appear in results, regardless of the query. The inner {@code multi_match}
 * boosts {@code fullName} highest, then first/last name, then bio/city.
 */
@Repository
public interface UserSearchRepository extends ElasticsearchRepository<UserDocument, Long> {

    @Query("""
            {
              "bool": {
                "must": [
                  {
                    "multi_match": {
                      "query": "?0",
                      "fields": ["fullName^3", "firstName^2", "lastName^2", "bio", "city"],
                      "type": "best_fields",
                      "fuzziness": "AUTO"
                    }
                  }
                ],
                "must_not": [
                  { "term": { "hideFromSearch": true } }
                ]
              }
            }
            """)
    Page<UserDocument> searchByQuery(String query, Pageable pageable);
}
