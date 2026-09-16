package ao.skool.fees.internal.service.adapter;

import ao.skool.fees.api.PaymentMethod;
import ao.skool.common.web.error.ApplicationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class PaymentAdapterRegistry {

    private final Map<PaymentMethod, PaymentAdapter> byMethod = new EnumMap<>(PaymentMethod.class);

    public PaymentAdapterRegistry(List<PaymentAdapter> adapters) {
        for (PaymentAdapter a : adapters) {
            byMethod.put(a.method(), a);
        }
    }

    public PaymentAdapter forMethod(PaymentMethod method) {
        PaymentAdapter a = byMethod.get(method);
        if (a == null) {
            throw new ApplicationException(HttpStatus.BAD_REQUEST, "error.validation");
        }
        return a;
    }
}
