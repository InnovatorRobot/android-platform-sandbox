Option 1 — Real-time Audio Visualizer
Mic input → C++ computes FFT (frequency spectrum) every 30ms → Kotlin draws animated bar chart

C++ is genuinely needed: FFT on every audio frame in real time is too slow in JVM
Visible result: animated equalizer bars reacting to sound/music
Good for: musicians, DJs, developers showing off

Option 2 — Noise Level Meter (dB Meter)
Mic input → C++ calculates RMS/peak dB → Kotlin shows big live gauge + history graph

Simpler than option 1, builds faster
Genuinely useful: construction workers, parents checking baby room noise, musicians
C++ justified: audio buffer math runs at hardware callback rate

Option 3 — Image Filter Camera
Camera frame → C++ applies convolution filters (blur, sharpen, edge detect) → Kotlin shows preview + save

C++ justified: per-pixel math on 4K frames can't run in Kotlin at 30fps
Tangible product: people take filtered photos
Uses OpenCV or raw C++ pixel math
