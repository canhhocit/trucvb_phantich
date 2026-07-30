package com.TrucVanban.registry.controller;

import com.TrucVanban.registry.dto.request.UpdateSlaConfigRequest;
import com.TrucVanban.registry.dto.response.UpdateSlaConfigResponse;
import com.TrucVanban.registry.service.RegistryService;
import com.TrucVanban.shared.ResponseData;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/registry/sla-configs")
@RequiredArgsConstructor
public class SlaConfigController {

    private final RegistryService registryService;

    @PutMapping("/{documentPriority}")
    public ResponseEntity<ResponseData<UpdateSlaConfigResponse>> updateSlaConfig(
            @PathVariable Integer documentPriority,
            @Valid @RequestBody UpdateSlaConfigRequest request) {

        UpdateSlaConfigResponse data = registryService.updateSlaConfig(documentPriority, request);

        ResponseData<UpdateSlaConfigResponse> response = ResponseData.<UpdateSlaConfigResponse>builder()
                .success(true)
                .message("Cập nhật SLA thành công")
                .data(data)
                .build();

        return ResponseEntity.ok(response);
    }
}
