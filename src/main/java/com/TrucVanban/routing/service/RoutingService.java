package com.TrucVanban.routing.service;

import com.TrucVanban.routing.dto.request.RoutingRequest;
import com.TrucVanban.routing.dto.response.RoutingResponse;

public interface RoutingService {
    RoutingResponse dispatch(RoutingRequest request);
}
