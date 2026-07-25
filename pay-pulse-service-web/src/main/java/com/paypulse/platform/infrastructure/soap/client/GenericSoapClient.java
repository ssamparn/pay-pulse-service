package com.paypulse.platform.infrastructure.soap.client;

public interface GenericSoapClient<T, U> {

    U send(T request);

    U send(String endpointUri, T request);
}

