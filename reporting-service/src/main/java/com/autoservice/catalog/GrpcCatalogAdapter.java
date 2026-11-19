package com.autoservice.catalog;

import com.autoservice.contract.CatalogRpcGrpc;
import com.autoservice.contract.ServiceId;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;

public class GrpcCatalogAdapter extends CatalogLookupAdapter {
    private final CatalogRpcGrpc.CatalogRpcBlockingStub stub;
    private final int deadlineMillis;

    public GrpcCatalogAdapter(CatalogRpcGrpc.CatalogRpcBlockingStub stub, int deadlineMillis) {
        this.stub = stub;
        this.deadlineMillis = deadlineMillis;
    }

    @Override
    protected ServiceCard find(long id) {
        try {
            var item = stub.withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
                    .getService(ServiceId.newBuilder().setId(id).build());
            return new ServiceCard(item.getId(), item.getName(), item.getDescription(),
                    new BigDecimal(item.getPrice()), item.getDurationMinutes());
        } catch (StatusRuntimeException error) {
            if (error.getStatus().getCode() == Status.Code.NOT_FOUND) {
                throw new CatalogItemNotFound(id);
            }
            throw new CatalogUnavailable(error);
        } catch (NumberFormatException error) {
            throw new CatalogUnavailable(error);
        }
    }
}
