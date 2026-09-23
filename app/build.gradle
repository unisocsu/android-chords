plugins {
    id 'com.android.application'
}

android {
    namespace 'kernel.unisocsu.chords_app'
    compileSdk 34

    defaultConfig {
        applicationId 'kernel.unisocsu.chords_app'
        minSdk 19
        targetSdk 19
        versionCode 1
        versionName '0.1.0'

        ndk {
            abiFilters 'armeabi-v7a', 'x86'
        }

        externalNativeBuild {
            cmake {
                arguments '-DANDROID_PLATFORM=android-19'
            }
        }
    }

    buildTypes {
        debug {
            minifyEnabled false
        }
        release {
            minifyEnabled false
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }

    ndkVersion '25.2.9519653'

    externalNativeBuild {
        cmake {
            path file('src/main/cpp/CMakeList.txt')
        }
    }
}
