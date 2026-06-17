#pragma once

#include <cstdint>
#include <vector>

namespace image_filter {

enum class FilterType : int {
  None = 0,
  Grayscale = 1,
  Blur = 2,
  Sharpen = 3,
  EdgeDetect = 4
};

/**
 * Stateless image filter applying 2D convolution kernels to ARGB_8888 pixels.
 * Thread-safe: contains no mutable state.
 */
class ImageFilter {
public:
  ImageFilter() = default;
  ~ImageFilter() = default;

  /**
   * Apply the requested filter to an ARGB_8888 pixel buffer.
   * @param pixels  Flat array of width*height int32 pixels (ARGB order).
   * @param width   Frame width in pixels.
   * @param height  Frame height in pixels.
   * @param type    Filter to apply.
   */
  void apply(int32_t *pixels, int32_t width, int32_t height,
             FilterType type) const;

private:
  void applyGrayscale(int32_t *pixels, int32_t width, int32_t height) const;
  void applyBlur(int32_t *pixels, int32_t width, int32_t height) const;
  void applySharpen(int32_t *pixels, int32_t width, int32_t height) const;
  void applyEdgeDetect(int32_t *pixels, int32_t width, int32_t height) const;

  void applyKernel(int32_t *pixels, int32_t width, int32_t height,
                   const float kernel[9], float scale, bool absValues) const;
};

} // namespace image_filter
