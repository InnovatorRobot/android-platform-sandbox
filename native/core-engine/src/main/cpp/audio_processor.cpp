#include "audio_processor.h"

#include <algorithm>
#include <cmath>
#include <numeric>

namespace audio_proc {

static constexpr float kPi = 3.14159265358979323846F;
static constexpr float kInt16Max = 32768.0F;
static constexpr float kSilenceDb = -100.0F;

// ── dB
// ────────────────────────────────────────────────────────────────────────

float AudioProcessor::computeDb(const int16_t *samples, int32_t count) const {
  if (samples == nullptr || count <= 0)
    return kSilenceDb;

  double sumSq = 0.0;
  for (int32_t i = 0; i < count; ++i) {
    const float s = static_cast<float>(samples[i]) / kInt16Max;
    sumSq += static_cast<double>(s * s);
  }
  const float rms =
      static_cast<float>(std::sqrt(sumSq / static_cast<double>(count)));
  if (rms < 1e-9F)
    return kSilenceDb;
  return 20.0F * std::log10(rms);
}

// ── Filter mapping
// ────────────────────────────────────────────────────────────

int32_t AudioProcessor::filterForDb(float db) const {
  if (db < -50.0F)
    return 0; // None
  if (db < -35.0F)
    return 2; // Blur
  if (db < -20.0F)
    return 1; // Grayscale
  if (db < -10.0F)
    return 3; // Sharpen
  return 4;   // EdgeDetect
}

// ── FFT (iterative Cooley-Tukey radix-2) ─────────────────────────────────────

void AudioProcessor::fft(std::vector<std::complex<float>> &x) const {
  const std::size_t n = x.size();
  if (n <= 1)
    return;

  // Bit-reversal permutation
  for (std::size_t i = 1, j = 0; i < n; ++i) {
    std::size_t bit = n >> 1;
    for (; j & bit; bit >>= 1)
      j ^= bit;
    j ^= bit;
    if (i < j)
      std::swap(x[i], x[j]);
  }

  // Butterfly passes
  for (std::size_t len = 2; len <= n; len <<= 1) {
    const float ang = -2.0F * kPi / static_cast<float>(len);
    const std::complex<float> wlen{std::cos(ang), std::sin(ang)};
    for (std::size_t i = 0; i < n; i += len) {
      std::complex<float> w{1.0F, 0.0F};
      for (std::size_t k = 0; k < len / 2; ++k) {
        const std::complex<float> u = x[i + k];
        const std::complex<float> v = x[i + k + len / 2] * w;
        x[i + k] = u + v;
        x[i + k + len / 2] = u - v;
        w *= wlen;
      }
    }
  }
}

// ── Magnitude bands
// ───────────────────────────────────────────────────────────

std::vector<float> AudioProcessor::computeBands(const int16_t *samples,
                                                int32_t count,
                                                int32_t bandCount) const {
  std::vector<float> bands(static_cast<std::size_t>(bandCount), 0.0F);
  if (samples == nullptr || count <= 0 || bandCount <= 0)
    return bands;

  // Round down to the largest power-of-2 ≤ count (cap at 4096)
  int32_t fftSize = 1;
  while (fftSize * 2 <= count && fftSize < 4096)
    fftSize <<= 1;

  std::vector<std::complex<float>> cx(static_cast<std::size_t>(fftSize));
  for (int32_t i = 0; i < fftSize; ++i) {
    // Hann window to reduce spectral leakage
    const float w = 0.5F * (1.0F - std::cos(2.0F * kPi * static_cast<float>(i) /
                                            static_cast<float>(fftSize - 1)));
    cx[static_cast<std::size_t>(i)] = {
        static_cast<float>(samples[i]) / kInt16Max * w, 0.0F};
  }

  fft(cx);

  const int32_t halfSize = fftSize / 2;
  // Map FFT bins → logarithmically-spaced bands (bins 1 .. halfSize-1)
  const float logMin = std::log2(2.0F);
  const float logMax = std::log2(static_cast<float>(halfSize));
  const float logRange = logMax - logMin;

  for (int32_t b = 0; b < bandCount; ++b) {
    const float t0 = static_cast<float>(b) / static_cast<float>(bandCount);
    const float t1 = static_cast<float>(b + 1) / static_cast<float>(bandCount);
    const auto binLo =
        static_cast<int32_t>(std::pow(2.0F, logMin + t0 * logRange));
    const auto binHi =
        static_cast<int32_t>(std::pow(2.0F, logMin + t1 * logRange));

    const int32_t lo = std::max(1, std::min(binLo, halfSize - 1));
    const int32_t hi = std::max(lo + 1, std::min(binHi + 1, halfSize));

    float maxMag = 0.0F;
    for (int32_t k = lo; k < hi; ++k) {
      maxMag = std::max(maxMag, std::abs(cx[static_cast<std::size_t>(k)]));
    }
    bands[static_cast<std::size_t>(b)] = maxMag;
  }

  // Normalise to [0, 1]
  const float peak = *std::max_element(bands.begin(), bands.end());
  if (peak > 1e-6F) {
    for (float &v : bands)
      v /= peak;
  }

  return bands;
}

} // namespace audio_proc
