package com.cariochi.recordo.httpmock.http;

import com.cariochi.recordo.httpmock.model.RecordoRequest;
import com.cariochi.recordo.httpmock.model.RecordoResponse;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.cariochi.recordo.httpmock.http.okhttp.OkHttpMapper.*;
import static com.cariochi.recordo.utils.Exceptions.trying;
import static com.cariochi.recordo.utils.Reflection.writeField;
import static java.util.stream.Collectors.toList;

public class OkHttpClientInterceptor implements Interceptor, HttpClientInterceptor {

    private Function<RecordoRequest, Optional<RecordoResponse>> onBeforeRequest;
    private BiFunction<RecordoRequest, RecordoResponse, RecordoResponse> onAfterRequest;

    public OkHttpClientInterceptor() {
    }

    public OkHttpClientInterceptor(OkHttpClient httpClient) {
        attachToHttpClient(httpClient);
    }

    @Override
    public void init(Function<RecordoRequest, Optional<RecordoResponse>> onBeforeRequest,
                     BiFunction<RecordoRequest, RecordoResponse, RecordoResponse> onAfterRequest) {
        this.onBeforeRequest = onBeforeRequest;
        this.onAfterRequest = onAfterRequest;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        final Request request = chain.request();
        final RecordoRequest recordoRequest = toRecordoRequest(request);
        final RecordoResponse recordoResponse = onBeforeRequest.apply(recordoRequest)
                .orElseGet(trying(
                        () -> onAfterRequest.apply(recordoRequest, toRecordoResponse(chain.proceed(request)))
                ));
        return fromRecordoResponse(request, recordoResponse);
    }

    private void attachToHttpClient(OkHttpClient httpClient) {
        final List<Interceptor> interceptors = httpClient.interceptors().stream()
                .filter(interceptor -> !(interceptor instanceof OkHttpClientInterceptor))
                .collect(toList());
        interceptors.add(this);
        writeField(httpClient, "interceptors", interceptors);
    }

}
