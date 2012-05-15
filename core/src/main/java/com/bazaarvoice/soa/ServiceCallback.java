package com.bazaarvoice.soa;

public interface ServiceCallback<S extends Service, RETURN> {
    RETURN call(S service) throws ServiceException;
}