package com.paypulse.platform.infrastructure.soap.client;

import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.ws.client.core.WebServiceTemplate;
import org.springframework.ws.client.core.support.WebServiceGatewaySupport;

public abstract class AbstractSpringWsSoapClient<T, U> extends WebServiceGatewaySupport implements GenericSoapClient<T, U> {

    protected AbstractSpringWsSoapClient(Marshaller marshaller) {
        WebServiceTemplate webServiceTemplate = new WebServiceTemplate();
        webServiceTemplate.setMarshaller(marshaller);
        if (marshaller instanceof Unmarshaller unmarshaller) {
            webServiceTemplate.setUnmarshaller(unmarshaller);
        }
        setWebServiceTemplate(webServiceTemplate);
    }

    @Override
    public U send(String endpointUri, T request) {
        Object response = getWebServiceTemplate().marshalSendAndReceive(endpointUri, request);
        return mapResponse(response);
    }

    @Override
    public U send(T request) {
        Object response = getWebServiceTemplate().marshalSendAndReceive(request);
        return mapResponse(response);
    }

    protected abstract U mapResponse(Object response);
}

