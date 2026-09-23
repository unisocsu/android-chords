#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include "chord.h"

JNIEXPORT jobjectArray JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeChords_detect
  (JNIEnv *env, jclass clazz, jfloatArray audio_data, jint sample_rate) {
    (void)clazz;
    jfloat *audio = (*env)->GetFloatArrayElements(env, audio_data, NULL);
    jsize n_samples = (*env)->GetArrayLength(env, audio_data);
    chord_segment_t *segments = NULL;
    int n_segments = chord_detect(audio, (int)n_samples, (int)sample_rate, &segments);
    (*env)->ReleaseFloatArrayElements(env, audio_data, audio, JNI_ABORT);
    jclass string_class = (*env)->FindClass(env, "java/lang/String");
    if (n_segments <= 0) {
        free(segments);
        return (*env)->NewObjectArray(env, 0, string_class, NULL);
    }
    jobjectArray result = (*env)->NewObjectArray(env, n_segments, string_class, NULL);
    for (int i = 0; i < n_segments; i++) {
        char buf[64];
        snprintf(buf, sizeof(buf), "%.3f|%.3f|%s", segments[i].start_sec, segments[i].end_sec, segments[i].label);
        jstring s = (*env)->NewStringUTF(env, buf);
        (*env)->SetObjectArrayElement(env, result, i, s);
        (*env)->DeleteLocalRef(env, s);
    }
    free(segments);
    return result;
}

JNIEXPORT jstring JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeChords_classifyFrame
  (JNIEnv *env, jclass clazz, jfloatArray frame, jint sample_rate) {
    (void)clazz;
    jfloat *samples = (*env)->GetFloatArrayElements(env, frame, NULL);
    jsize n_samples = (*env)->GetArrayLength(env, frame);
    char label[8];
    chord_classify_frame(samples, (int)n_samples, (int)sample_rate, label);
    (*env)->ReleaseFloatArrayElements(env, frame, samples, JNI_ABORT);
    return (*env)->NewStringUTF(env, label);
}

JNIEXPORT jint JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeChords_frameLengthSamples
  (JNIEnv *env, jclass clazz, jint sample_rate) {
    (void)env;
    (void)clazz;
    return chord_frame_length_samples((int)sample_rate);
}
