package com.TrucVanban.shared;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResponseData<T> {
    @Builder.Default
    private boolean success = true;
    private String message;
    private T data;
    
}
