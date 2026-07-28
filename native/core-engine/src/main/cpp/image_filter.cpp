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
  // Work on a copy so we don't read partially-written output.
  std::vector<int32_t> src(pixels, pixels + width * height);
  const int32_t *s = src.data();

  // Precompute integer kernel weights (fixed-point) to avoid float math in the
  // hot loop. Weights are small integers (e.g. ±1, 5, 8); scale is applied
  // once.
  int32_t ik[9];
  for (int32_t i = 0; i < 9; ++i) {
    ik[i] = static_cast<int32_t>(kernel[i]);
  }

  for (int32_t y = 1; y < height - 1; ++y) {
    const int32_t *row0 = s + (y - 1) * width;
    const int32_t *row1 = s + y * width;
    const int32_t *row2 = s + (y + 1) * width;
    int32_t *out = pixels + y * width;

    for (int32_t x = 1; x < width - 1; ++x) {
      int32_t r = 0, g = 0, b = 0;

      // Unrolled 3×3 convolution over the three rows.
      const int32_t p00 = row0[x - 1], p01 = row0[x], p02 = row0[x + 1];
      const int32_t p10 = row1[x - 1], p11 = row1[x], p12 = row1[x + 1];
      const int32_t p20 = row2[x - 1], p21 = row2[x], p22 = row2[x + 1];

      r = ((p00 >> 16) & 0xFF) * ik[0] + ((p01 >> 16) & 0xFF) * ik[1] +
          ((p02 >> 16) & 0xFF) * ik[2] + ((p10 >> 16) & 0xFF) * ik[3] +
          ((p11 >> 16) & 0xFF) * ik[4] + ((p12 >> 16) & 0xFF) * ik[5] +
          ((p20 >> 16) & 0xFF) * ik[6] + ((p21 >> 16) & 0xFF) * ik[7] +
          ((p22 >> 16) & 0xFF) * ik[8];
      g = ((p00 >> 8) & 0xFF) * ik[0] + ((p01 >> 8) & 0xFF) * ik[1] +
          ((p02 >> 8) & 0xFF) * ik[2] + ((p10 >> 8) & 0xFF) * ik[3] +
          ((p11 >> 8) & 0xFF) * ik[4] + ((p12 >> 8) & 0xFF) * ik[5] +
          ((p20 >> 8) & 0xFF) * ik[6] + ((p21 >> 8) & 0xFF) * ik[7] +
          ((p22 >> 8) & 0xFF) * ik[8];
      b = (p00 & 0xFF) * ik[0] + (p01 & 0xFF) * ik[1] + (p02 & 0xFF) * ik[2] +
          (p10 & 0xFF) * ik[3] + (p11 & 0xFF) * ik[4] + (p12 & 0xFF) * ik[5] +
          (p20 & 0xFF) * ik[6] + (p21 & 0xFF) * ik[7] + (p22 & 0xFF) * ik[8];

      if (scale != 1.0F) {
        r = static_cast<int32_t>(static_cast<float>(r) * scale);
        g = static_cast<int32_t>(static_cast<float>(g) * scale);
        b = static_cast<int32_t>(static_cast<float>(b) * scale);
      }
      if (absValues) {
        r = r < 0 ? -r : r;
        g = g < 0 ? -g : g;
        b = b < 0 ? -b : b;
      }

      out[x] = argbPack(alphaOf(p11), clamp(r), clamp(g), clamp(b));
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
