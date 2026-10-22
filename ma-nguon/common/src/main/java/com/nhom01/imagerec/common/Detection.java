package com.nhom01.imagerec.common;

/**
 * Một đối tượng được phát hiện trong ảnh.
 *
 * <p>Tọa độ bounding box là tỉ lệ 0..1 so với kích thước ảnh gốc: (x, y) là góc trên-trái,
 * width/height là bề rộng/chiều cao. Nhờ vậy Client vẽ đúng dù hiển thị ảnh ở kích thước nào.
 *
 * @param label      tên lớp đối tượng, ví dụ "person"
 * @param confidence độ tin cậy 0..1
 */
public record Detection(String label, double confidence, double x, double y, double width, double height) {
}
