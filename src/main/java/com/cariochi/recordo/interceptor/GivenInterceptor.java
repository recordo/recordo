package com.cariochi.recordo.interceptor;

import com.cariochi.recordo.Given;
import com.cariochi.recordo.Givens;
import com.cariochi.recordo.RecordoException;
import com.cariochi.recordo.json.JsonConverter;
import com.cariochi.recordo.json.JsonPropertyFilter;
import com.cariochi.recordo.utils.ExceptionsSuppressor;
import com.cariochi.recordo.utils.Files;
import com.cariochi.recordo.utils.Properties;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Stream;

import static com.cariochi.recordo.utils.ReflectionUtils.*;
import static java.lang.String.format;
import static org.apache.commons.lang3.reflect.MethodUtils.getAnnotation;
import static org.slf4j.LoggerFactory.getLogger;

public class GivenInterceptor implements BeforeTestInterceptor {

    private static final Logger log = getLogger(GivenInterceptor.class);

    private final PodamFactory podamFactory = new PodamFactoryImpl(new RandomDataProviderStrategy());
    private final JsonConverter jsonConverter;

    public GivenInterceptor(JsonConverter jsonConverter) {
        this.jsonConverter = jsonConverter;
    }

    @Override
    public void beforeTest(Object testInstance, Method method) {
        ExceptionsSuppressor.of(RecordoException.class).executeAll(
                findGivenAnnotations(method).map(given -> () -> writeFieldValue(testInstance, given, method))
        );
    }

    private void writeFieldValue(Object testInstance, Given given, Method method) {
        final Type fieldType = fieldType(testInstance, given);
        final String fileName = fileName(given, method);
        try {
            final String json = Files.readFromFile(fileName);
            final Object givenObject = jsonConverter.fromJson(json, fieldType);
            writeField(testInstance, given.value(), givenObject);
            log.info("`{}` value was read from `{}`", given.value(), fileName);
        } catch (IOException e) {
            final String message = generateFile(fieldType, fileName)
                    .map(file -> format("\nRandom '%s' value file was generated.", given.value()))
                    .orElse("");
            throw new RecordoException(e.getMessage() + message);
        }
    }

    private Type fieldType(Object testInstance, Given given) {
        return getFieldAndTargetObject(testInstance, given.value())
                .map(Pair::getLeft)
                .map(Field::getGenericType)
                .orElseThrow(() -> new IllegalArgumentException(format("Test field %s not found", given.value())));
    }

    private String fileName(Given given, Method method) {
        final String fileNamePattern = Optional.of(given.file())
                .filter(StringUtils::isNotBlank)
                .orElseGet(Properties::givenFileNamePattern);
        return Files.fileName(fileNamePattern, method, given.value());
    }

    private Optional<File> generateFile(Type fieldType, String fileName) {
        return Optional.ofNullable(generateValue(fieldType))
                .flatMap(o -> Files.writeToFile(jsonConverter.toJson(o, new JsonPropertyFilter()), fileName));
    }

    private Object generateValue(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return podamFactory.manufacturePojo(
                    (Class<?>) parameterizedType.getRawType(),
                    parameterizedType.getActualTypeArguments()
            );
        } else if (type instanceof Class) {
            return podamFactory.manufacturePojo((Class<?>) type);
        } else {
            return null;
        }
    }

    private Stream<Given> findGivenAnnotations(Method method) {
        return Optional.ofNullable(getAnnotation(method, Givens.class, true, true))
                .map(Givens::value)
                .map(Arrays::stream)
                .orElseGet(() -> findAnnotation(method, Given.class));
    }

}
