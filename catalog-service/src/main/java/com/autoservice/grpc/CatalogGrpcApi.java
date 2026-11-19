package com.autoservice.grpc;

import com.autoservice.contract.CatalogRpcGrpc;
import com.autoservice.contract.ServiceData;
import com.autoservice.contract.ServiceId;
import com.autoservice.service.CatalogService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class CatalogGrpcApi extends CatalogRpcGrpc.CatalogRpcImplBase {
    private final CatalogService catalog;

    public CatalogGrpcApi(CatalogService catalog) {
        this.catalog = catalog;
    }

    @Override
    public void getService(ServiceId request, StreamObserver<ServiceData> response) {
        try {
            var item = catalog.findById(request.getId());
            response.onNext(ServiceData.newBuilder().setId(item.id()).setName(item.name())
                    .setDescription(item.description() == null ? "" : item.description())
                    .setPrice(item.price().toPlainString()).setDurationMinutes(item.durationMinutes()).build());
            response.onCompleted();
        } catch (ResponseStatusException error) {
            var status = error.getStatusCode().value() == 404 ? Status.NOT_FOUND : Status.INTERNAL;
            response.onError(status.withDescription("Послуга недоступна").asRuntimeException());
        } catch (RuntimeException error) {
            response.onError(Status.INTERNAL.withDescription("Помилка каталогу").asRuntimeException());
        }
    }
}
