#!/bin/sh
# Tải sẵn mô hình DJL + native library vào cache (~/.djl.ai hoặc $DJL_CACHE_DIR).
# Chạy từ thư mục gốc sau khi build: ./mvnw -q -pl server package -DskipTests
exec java -cp server/target/server-1.0-SNAPSHOT-all.jar \
  com.nhom01.imagerec.server.detect.DjlObjectDetector \
  server/src/test/resources/djl/dog_bike_car.jpg
