package com.shreyansh.regressionguard.api;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.shreyansh.regressionguard.domain.MaxWords;
import com.shreyansh.regressionguard.domain.PropertyRule;
import com.shreyansh.regressionguard.domain.RequiredFields;
import com.shreyansh.regressionguard.domain.ValidJson;
import java.util.List;

/**
 * A property rule as it appears in a request body, for example {"type": "maxWords", "limit": 200}.
 * Lives in the API layer so the domain records carry no JSON annotations.
 * An unknown "type" is rejected by Jackson and becomes 400 Bad Request.
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = RuleRequest.ValidJsonRequest.class, name = "validJson"),
        @JsonSubTypes.Type(value = RuleRequest.RequiredFieldsRequest.class, name = "requiredFields"),
        @JsonSubTypes.Type(value = RuleRequest.MaxWordsRequest.class, name = "maxWords")
})
public sealed interface RuleRequest {
    PropertyRule toRule();


    record ValidJsonRequest() implements RuleRequest {
        @Override
        public PropertyRule toRule() {
            return new ValidJson();
        }
    }

    record RequiredFieldsRequest(List<String> fields) implements RuleRequest {
        @Override
        public PropertyRule toRule() {
            return new RequiredFields(fields);
        }
    }

    record MaxWordsRequest(int limit) implements RuleRequest {
        @Override
        public PropertyRule toRule() {
            return new MaxWords(limit);
        }
    }
}
