#ifndef WHISPER_ANDROID_CHORD_INTERNAL_H
#define WHISPER_ANDROID_CHORD_INTERNAL_H

// Shared between chord.c (feature extraction + the simple live classifier)
// and chord_hmm.c (statistical sequence decoder) - both need the same
// chroma features, just do different things with them afterward.

#define FRAME_SECONDS      0.25
#define HOP_FRACTION       0.5

#define N_PITCH_CLASSES     12

extern const char * CHORD_NOTE_NAMES[N_PITCH_CLASSES];

// Fills a 12-bin chroma vector for one already-windowed frame (see chord.c
// for how the window itself is built/cached).
void chord_compute_chroma(const float * windowed_frame, int frame_len, double sample_rate, double chroma[N_PITCH_CLASSES]);

// Returns a cached Hann window of the given length, (re)computing it only
// when the requested length changes. NOT thread-safe for concurrent callers
// - fine here since chord analysis only ever runs on one thread at a time
// (either the batch caller or the live recording thread, never both).
const float * chord_get_window(int frame_len);

#endif //WHISPER_ANDROID_CHORD_INTERNAL_H
