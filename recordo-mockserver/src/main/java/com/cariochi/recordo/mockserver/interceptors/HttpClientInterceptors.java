package com.cariochi.recordo.mockserver.interceptors;

import com.cariochi.recordo.core.utils.Beans;
import com.cariochi.recordo.core.utils.Beans.OptionalBean;
import com.cariochi.recordo.mockserver.interceptors.apache.ApacheMockServerInterceptor;
import com.cariochi.recordo.mockserver.interceptors.okhttp.OkMockServerInterceptor;
import com.cariochi.recordo.mockserver.interceptors.resttemplate.RestTemplateInterceptor;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import org.apache.http.client.HttpClient;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.web.client.RestTemplate;

@Slf4j
@UtilityClass
public class HttpClientInterceptors {

    public OptionalBean<MockServerInterceptor> findInterceptor(String beanName, ExtensionContext context) {
        final Beans beans = Beans.of(context);
        return OptionalBean.<MockServerInterceptor>empty()
                .or(() -> interceptor(beanName, beans))
                .or(() -> restTemplate(beanName, beans))
                .or(() -> okHttpClient(beanName, beans))
                .or(() -> apacheHttpClient(beanName, beans));
    }

    private OptionalBean<MockServerInterceptor> interceptor(String beanName, Beans beans) {
        try {
            return beans.find(beanName, MockServerInterceptor.class);
        } catch (Exception | NoClassDefFoundError e) {
            return OptionalBean.empty();
        }
    }

    private OptionalBean<MockServerInterceptor> restTemplate(String beanName, Beans beans) {
        try {
            return beans.find(beanName, RestTemplate.class)
                    .map(RestTemplateInterceptor::attachTo)
                    .map(MockServerInterceptor.class::cast);
        } catch (Exception | NoClassDefFoundError e) {
            return OptionalBean.empty();
        }
    }

    private OptionalBean<MockServerInterceptor> okHttpClient(String beanName, Beans beans) {
        try {
            return beans.find(beanName, OkHttpClient.class)
                    .map(OkMockServerInterceptor::attachTo)
                    .map(MockServerInterceptor.class::cast);
        } catch (Exception | NoClassDefFoundError e) {
            return OptionalBean.empty();
        }
    }

    private OptionalBean<MockServerInterceptor> apacheHttpClient(String beanName, Beans beans) {
        try {
            return beans.find(beanName, HttpClient.class)
                    .map(ApacheMockServerInterceptor::new)
                    .map(MockServerInterceptor.class::cast);
        } catch (Exception | NoClassDefFoundError e) {
            return OptionalBean.empty();
        }
    }

}
