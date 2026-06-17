#include "image_filter.h"

#include <algorithm>
#include <cmath>
#include <cstring>
#include <vector>

namespace image_filter {

// ── Pixel component helpers
// ───────────────────────────────────────────────────

static inline int32_t argbPack(int32_t a, int32_t r, int32_t g, int32_t b) {
  return ((a & 0xFF) << 24) | ((r & 0xFF) << 16) | ((g & 0xFF) << 8) |
         (b & 0xFF);
}
static inline int32_t alphaOf(int32_t p) { return (p >> 24) & 0xFF; }
static inline int32_t redOf(int32_t p) { return (p >> 16) & 0xFF; }
static inline int32_t greenOf(int32_t p) { return (p >> 8) & 0xFF; }
static inline int32_t blueOf(int32_t p) { return p & 0xFF; }
static inline int32_t clamp(int32_t v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

// ── Public dispatch
// ───────────────────────────────────────────────────────────

void ImageFilter::apply(int32_t *pixels, int32_t width, int32_t height,
                        FilterType type) const {
  switch (type) {
  case FilterType::Grayscale:
    applyGrayscale(pixels, width, height);
    break;
  case FilterType::Blur:
    applyBlur(pixels, width, height);
    break;
  case FilterType::Sharpen:
    applySharpen(pixels, width, height);
    break;
  case FilterType::EdgeDetect:
    applyEdgeDetect(pixels, width, height);
    break;
  default:
    break; // FilterType::None — passthrough
  }
}

// ── Grayscale
// ─────────────────────────────────────────────────────────────────

void ImageFilter::applyGrayscale(int32_t *pixels, int32_t width,
                                 int32_t height) const {
  const int32_t n = width * height;
  for (int32_t i = 0; i < n; ++i) {
    const int32_t p = pixels[i];
    // BT.601 luminance weights
    const int32_t gray =
        (redOf(p) * 299 + greenOf(p) * 587 + blueOf(p) * 114) / 1000;
    pixels[i] = argbPack(alphaOf(p), gray, gray, gray);
  }
}

// ── 3×3 kernel helper
// ─────────────────────────────────────────────────────────

void ImageFilter::applyKernel(int32_t *pixels, int32_t width, int32_t height,
                              const float kernel[9], float scale,
                              bool absValues) const {
  // Work on a copy so we don't read partially-written output
  std::vector<int32_t> src(pixels, pixels + width * height);

  for (int32_t y = 1; y < height - 1; ++y) {
    for (int32_t x = 1; x < width - 1; ++x) {
      float r = 0.0F, g = 0.0F, b = 0.0F;
      int32_t k = 0;
      for (int32_t dy = -1; dy <= 1; ++dy) {
        for (int32_t dx = -1; dx <= 1; ++dx) {
          const int32_t p = src[(y + dy) * width + (x + dx)];
          r += static_cast<float>(redOf(p)) * kernel[k];
          g += static_cast<float>(greenOf(p)) * kernel[k];
          b += static_cast<float>(blueOf(p)) * kernel[k];
          ++k;
        }
      }
      r *= scale;
      g *= scale;
      b *= scale;
      if (absValues) {
        r = std::abs(r);
        g = std::abs(g);
        b = std::abs(b);
      }

      const int32_t orig = src[y * width + x];
      pixels[y * width + x] = argbPack(
          alphaOf(orig), clamp(static_cast<int32_t>(r)),
          clamp(static_cast<int32_t>(g)), clamp(static_cast<int32_t>(b)));
    }
  }
}

// ── Blur
// ──────────────────────────────────────────────────────────────────────

void ImageFilter::applyBlur(int32_t *pixels, int32_t width,
                            int32_t height) const {
  // 3×3 box blur — equal weights
  const float k[9] = {1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F, 1.0F};
  applyKernel(pixels, width, height, k, 1.0F / 9.0F, false);
}

// ── Sharpen
// ───────────────────────────────────────────────────────────────────

void ImageFilter::applySharpen(int32_t *pixels, int32_t width,
                               int32_t height) const {
  const float k[9] = {0.0F, -1.0F, 0.0F, -1.0F, 5.0F, -1.0F, 0.0F, -1.0F, 0.0F};
  applyKernel(pixels, width, height, k, 1.0F, false);
}

// ── Edge detect (Laplacian)
// ───────────────────────────────────────────────────

void ImageFilter::applyEdgeDetect(int32_t *pixels, int32_t width,
                                  int32_t height) const {
  // Convert to grayscale first for a cleaner edge map
  applyGrayscale(pixels, width, height);
  const float k[9] = {-1.0F, -1.0F, -1.0F, -1.0F, 8.0F,
                      -1.0F, -1.0F, -1.0F, -1.0F};
  applyKernel(pixels, width, height, k, 1.0F, true);
}

} // namespace image_filter
