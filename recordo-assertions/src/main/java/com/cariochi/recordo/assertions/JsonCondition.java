package com.cariochi.recordo.assertions;

import com.cariochi.recordo.core.json.JsonConverter;
import com.cariochi.recordo.core.json.JsonPropertyFilter;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.assertj.core.api.Condition;
import org.skyscreamer.jsonassert.JSONCompareMode;

import static com.cariochi.recordo.core.json.JsonUtils.compareMode;
import static java.util.Arrays.asList;

public class JsonCondition<T> extends Condition<T> {

    private final RecordoPredicate<T> predicate;

    public static <T> JsonCondition<T> equalAsJsonTo(String fileName) {
        return new JsonCondition<>(new RecordoPredicate<>(fileName));
    }

    private JsonCondition(RecordoPredicate<T> predicate) {
        super(predicate, "");
        this.predicate = predicate;
    }

    public JsonCondition<T> using(ObjectMapper mapper) {
        predicate.using(mapper);
        return this;
    }

    public JsonCondition<T> including(String... fields) {
        predicate.including(asList(fields));
        return this;
    }

    public JsonCondition<T> excluding(String... fields) {
        predicate.excluding(asList(fields));
        return this;
    }

    public JsonCondition<T> extensible(boolean extensible) {
        predicate.extensible(extensible);
        return this;
    }

    public JsonCondition<T> withStrictOrder(boolean strictOrder) {
        predicate.strictOrder(strictOrder);
        return this;
    }

    @RequiredArgsConstructor
    @Setter
    @Accessors(fluent = true)
    private static class RecordoPredicate<T> implements Predicate<T> {

        private final JsonComparator jsonComparator = new JsonComparator();
        private final String fileName;

        private List<String> including = new ArrayList<>();
        private List<String> excluding = new ArrayList<>();
        private boolean extensible = false;
        private boolean strictOrder = true;

        @Override
        public boolean test(T actual) {
            final JsonPropertyFilter jsonFilter = new JsonPropertyFilter(including, excluding);
            final JSONCompareMode compareMode = compareMode(extensible, strictOrder);
            return jsonComparator.compareAsJson(actual, fileName, jsonFilter, compareMode).passed();
        }

        public void using(ObjectMapper objectMapper) {
            jsonComparator.setJsonConverter(new JsonConverter(objectMapper));
        }

    }

}
