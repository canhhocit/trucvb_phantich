#!/bin/bash

# Định nghĩa đường dẫn tương đối từ gốc dự án đến package Java
BASE_PATH="src/main/java/com/lechicong/documenthub"

# Danh sách 3 module tuần này cần khởi tạo
MODULES=("registry" "exchange" "routing")

echo "🚀 Đang khởi tạo cấu trúc thư mục và file Java mẫu tại: $(pwd)"

# 1. Tạo thư mục và file dùng chung (Shared Base) cho Công làm
mkdir -p "$BASE_PATH/shared/exception"
mkdir -p "$BASE_PATH/shared/config"

cat << 'EOF' > "$BASE_PATH/shared/exception/GlobalExceptionHandler.java"
package com.lechicong.documenthub.shared.exception;

public class GlobalExceptionHandler {
    // Base Global Exception Handler
}
EOF

cat << 'EOF' > "$BASE_PATH/shared/config/SecurityConfig.java"
package com.lechicong.documenthub.shared.config;

public class SecurityConfig {
    // Base Security Configuration
}
EOF

# 2. Vòng lặp tự động tạo các thư mục con và file .java mẫu cho từng Module
for MODULE in "${MODULES[@]}"; do
    # Chuyển ký tự đầu của tên module thành chữ hoa để đặt tên Class cho chuẩn Java
    CLASS_PREFIX="$(tr '[:lower:]' '[:upper:]' <<< "${MODULE:0:1}")${MODULE:1}"
    
    echo "Creating files for module: $MODULE..."
    
    # Tạo các thư mục vật lý
    mkdir -p "$BASE_PATH/$MODULE/controller"
    mkdir -p "$BASE_PATH/$MODULE/dto/request"
    mkdir -p "$BASE_PATH/$MODULE/dto/response"
    mkdir -p "$BASE_PATH/$MODULE/entity"
    mkdir -p "$BASE_PATH/$MODULE/repository"
    mkdir -p "$BASE_PATH/$MODULE/service/impl"
    
    # Ghi nội dung code Java cơ bản vào từng file
    
    # Controller file
    cat << EOF > "$BASE_PATH/$MODULE/controller/${CLASS_PREFIX}Controller.java"
package com.lechicong.documenthub.$MODULE.controller;

public class ${CLASS_PREFIX}Controller {
}
EOF

    # Request DTO file
    cat << EOF > "$BASE_PATH/$MODULE/dto/request/${CLASS_PREFIX}Request.java"
package com.lechicong.documenthub.$MODULE.dto.request;

public class ${CLASS_PREFIX}Request {
}
EOF

    # Response DTO file
    cat << EOF > "$BASE_PATH/$MODULE/dto/response/${CLASS_PREFIX}Response.java"
package com.lechicong.documenthub.$MODULE.dto.response;

public class ${CLASS_PREFIX}Response {
}
EOF

    # Entity file
    cat << EOF > "$BASE_PATH/$MODULE/entity/${CLASS_PREFIX}.java"
package com.lechicong.documenthub.$MODULE.entity;

public class ${CLASS_PREFIX} {
}
EOF

    # Repository file
    cat << EOF > "$BASE_PATH/$MODULE/repository/${CLASS_PREFIX}Repository.java"
package com.lechicong.documenthub.$MODULE.repository;

public interface ${CLASS_PREFIX}Repository {
}
EOF

    # Service Interface file
    cat << EOF > "$BASE_PATH/$MODULE/service/${CLASS_PREFIX}Service.java"
package com.lechicong.documenthub.$MODULE.service;

public interface ${CLASS_PREFIX}Service {
}
EOF

    # Service Implementation file
    cat << EOF > "$BASE_PATH/$MODULE/service/impl/${CLASS_PREFIX}ServiceImpl.java"
package com.lechicong.documenthub.$MODULE.service.impl;

import com.lechicong.documenthub.$MODULE.service.${CLASS_PREFIX}Service;

public class ${CLASS_PREFIX}ServiceImpl implements ${CLASS_PREFIX}Service {
}
EOF

done

echo "🎉 [Thành công] Tất cả các folder đã được tạo kèm file .java mẫu chuẩn chỉnh!"
