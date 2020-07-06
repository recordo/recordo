package com.cariochi.recordo.given;

import uk.co.jemos.podam.api.AbstractRandomDataProviderStrategy;
import uk.co.jemos.podam.api.PodamFactory;
import uk.co.jemos.podam.api.PodamFactoryImpl;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

public class RandomDataGenerator {

    private final PodamFactory factory = new PodamFactoryImpl(new DataProviderStrategy());

    @SuppressWarnings("unchecked")
    public <T> T generateObject(Type type) {
        if (type instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) type;
            return (T) factory.manufacturePojo(
                    (Class<?>) parameterizedType.getRawType(),
                    parameterizedType.getActualTypeArguments()
            );
        } else if (type instanceof Class) {
            return (T) factory.manufacturePojo((Class<?>) type);
        } else {
            return null;
        }
    }

    public static class DataProviderStrategy extends AbstractRandomDataProviderStrategy {
        public DataProviderStrategy() {
            super(2);
            setMaxDepth(3);
        }
    }
}
