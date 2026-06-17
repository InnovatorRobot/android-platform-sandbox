#pragma once

#include <complex>
#include <cstdint>
#include <vector>

namespace audio_proc {

/**
 * Processes raw 16-bit PCM audio into dB levels, filter suggestions,
 * and logarithmically-spaced FFT magnitude bands for visualisation.
 * All operations are stateless and thread-safe.
 */
class AudioProcessor {
public:
  AudioProcessor() = default;
  ~AudioProcessor() = default;

  /**
   * Compute RMS power level in dBFS.
   * Returns values in the range [-100, 0]. Returns -100 for silence.
   */
  float computeDb(const int16_t *samples, int32_t count) const;

  /**
   * Map a dBFS level to an image_filter::FilterType int (0-4).
   *   < -50 dBFS  →  0  None       (silent — show original)
   *  -50..-35     →  2  Blur        (quiet — soft)
   *  -35..-20     →  1  Grayscale   (medium — calm)
   *  -20..-10     →  3  Sharpen     (loud — energetic)
   *   > -10       →  4  EdgeDetect  (very loud — intense)
   */
  int32_t filterForDb(float db) const;

  /**
   * Compute magnitude spectrum bands from PCM samples.
   * Applies Hann window, runs a radix-2 Cooley-Tukey FFT, then groups
   * frequency bins logarithmically into bandCount output bands.
   * Each value is normalised to [0, 1].
   */
  std::vector<float> computeBands(const int16_t *samples, int32_t count,
                                  int32_t bandCount) const;

private:
  void fft(std::vector<std::complex<float>> &x) const;
};

} // namespace audio_proc
