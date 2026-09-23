#include <jni.h>
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>
#include <android/log.h>
#include <stdlib.h>
#include <string.h>
#include "whisper.h"

#define TAG "ChordsWhisper"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)

static struct whisper_context_params low_resource_context_params(void) {
    struct whisper_context_params p = whisper_context_default_params();
    p.use_gpu = false;
    p.flash_attn = false;
    return p;
}

struct input_stream_context { JNIEnv *env; jobject stream; jmethodID available; jmethodID read; };
static size_t stream_read(void *ctx, void *out, size_t n) {
    struct input_stream_context *c = (struct input_stream_context*)ctx;
    jint avail = (*c->env)->CallIntMethod(c->env, c->stream, c->available);
    jint want = n < (size_t)avail ? (jint)n : avail;
    if (want <= 0) return 0;
    jbyteArray arr = (*c->env)->NewByteArray(c->env, want);
    jint got = (*c->env)->CallIntMethod(c->env, c->stream, c->read, arr, 0, want);
    if (got > 0) {
        jbyte *p = (*c->env)->GetByteArrayElements(c->env, arr, NULL);
        memcpy(out, p, got);
        (*c->env)->ReleaseByteArrayElements(c->env, arr, p, JNI_ABORT);
    }
    (*c->env)->DeleteLocalRef(c->env, arr);
    return got > 0 ? (size_t)got : 0;
}
static bool stream_eof(void *ctx) {
    struct input_stream_context *c = (struct input_stream_context*)ctx;
    return (*c->env)->CallIntMethod(c->env, c->stream, c->available) <= 0;
}
static void stream_close(void *ctx) { (void)ctx; }
static size_t asset_loader_read(void *ctx, void *out, size_t n) { return AAsset_read((AAsset*)ctx, out, n); }
static bool asset_loader_eof(void *ctx) { return AAsset_getRemainingLength64((AAsset*)ctx) <= 0; }
static void asset_loader_close(void *ctx) { AAsset_close((AAsset*)ctx); }

JNIEXPORT jlong JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_initContext
  (JNIEnv *env, jclass cls, jstring path) {
    (void)cls;
    const char *p = (*env)->GetStringUTFChars(env, path, NULL);
    struct whisper_context *ctx = whisper_init_from_file_with_params(p, low_resource_context_params());
    (*env)->ReleaseStringUTFChars(env, path, p);
    return (jlong)ctx;
}

JNIEXPORT jlong JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_initContextFromAsset
  (JNIEnv *env, jclass cls, jobject manager, jstring assetPath) {
    (void)cls;
    const char *name = (*env)->GetStringUTFChars(env, assetPath, NULL);
    AAssetManager *am = AAssetManager_fromJava(env, manager);
    AAsset *asset = AAssetManager_open(am, name, AASSET_MODE_STREAMING);
    (*env)->ReleaseStringUTFChars(env, assetPath, name);
    if (!asset) return 0;
    struct whisper_model_loader loader = {
        .context = asset,
        .read = asset_loader_read,
        .eof = asset_loader_eof,
        .close = asset_loader_close
    };
    return (jlong)whisper_init_with_params(&loader, low_resource_context_params());
}

JNIEXPORT jlong JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_initContextFromInputStream
  (JNIEnv *env, jclass cls, jobject stream) {
    (void)cls;
    struct input_stream_context *ctx = (struct input_stream_context*)calloc(1, sizeof(*ctx));
    if (!ctx) return 0;
    ctx->env = env;
    ctx->stream = stream;
    jclass c = (*env)->GetObjectClass(env, stream);
    ctx->available = (*env)->GetMethodID(env, c, "available", "()I");
    ctx->read = (*env)->GetMethodID(env, c, "read", "([BII)I");
    struct whisper_model_loader loader = { .context = ctx, .read = stream_read, .eof = stream_eof, .close = stream_close };
    struct whisper_context *result = whisper_init_with_params(&loader, low_resource_context_params());
    free(ctx);
    return (jlong)result;
}

JNIEXPORT void JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_freeContext
  (JNIEnv *env, jclass cls, jlong ptr) { (void)env; (void)cls; whisper_free((struct whisper_context*)ptr); }

JNIEXPORT void JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_fullTranscribe
  (JNIEnv *env, jclass cls, jlong ptr, jint threads, jfloatArray audio) {
    (void)cls;
    jfloat *data = (*env)->GetFloatArrayElements(env, audio, NULL);
    jsize n = (*env)->GetArrayLength(env, audio);
    struct whisper_full_params p = whisper_full_default_params(WHISPER_SAMPLING_GREEDY);
    p.print_realtime = false;
    p.print_progress = false;
    p.print_timestamps = true;
    p.print_special = false;
    p.translate = false;
    p.language = "he";
    p.n_threads = threads;
    p.no_context = true;
    p.single_segment = false;
    int rc = whisper_full((struct whisper_context*)ptr, p, data, n);
    LOGI("whisper_full returned %d", rc);
    (*env)->ReleaseFloatArrayElements(env, audio, data, JNI_ABORT);
}

JNIEXPORT jint JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_segmentCount
  (JNIEnv *env, jclass cls, jlong ptr) { (void)env; (void)cls; return whisper_full_n_segments((struct whisper_context*)ptr); }

JNIEXPORT jstring JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_segmentText
  (JNIEnv *env, jclass cls, jlong ptr, jint i) { (void)cls; return (*env)->NewStringUTF(env, whisper_full_get_segment_text((struct whisper_context*)ptr, i)); }

JNIEXPORT jlong JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_segmentStart
  (JNIEnv *env, jclass cls, jlong ptr, jint i) { (void)env; (void)cls; return whisper_full_get_segment_t0((struct whisper_context*)ptr, i); }

JNIEXPORT jlong JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_segmentEnd
  (JNIEnv *env, jclass cls, jlong ptr, jint i) { (void)env; (void)cls; return whisper_full_get_segment_t1((struct whisper_context*)ptr, i); }

JNIEXPORT jstring JNICALL Java_kernel_unisocsu_chords_1app_nativebridge_NativeWhisper_systemInfo
  (JNIEnv *env, jclass cls) { (void)cls; return (*env)->NewStringUTF(env, whisper_print_system_info()); }
