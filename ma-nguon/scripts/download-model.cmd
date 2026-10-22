@echo off
rem Tai san mo hinh DJL + native library vao cache (%USERPROFILE%\.djl.ai hoac %DJL_CACHE_DIR%).
rem Chay tu thu muc goc sau khi da build: mvnw.cmd -q -pl server package -DskipTests
java -cp server\target\server-1.0-SNAPSHOT-all.jar com.nhom01.imagerec.server.detect.DjlObjectDetector server\src\test\resources\djl\dog_bike_car.jpg
