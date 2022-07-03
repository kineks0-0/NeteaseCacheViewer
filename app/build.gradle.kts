plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt") //version "1.6.21"
}

@Suppress("PropertyName")
val compose_version: String by project

@Suppress("PropertyName")
val accompanist_version: String by project

android {

    compileSdk = 32
    //compileSdkPreview = "android-Tiramisu"

    defaultConfig {
        applicationId = "io.github.kineks.neteaseviewer"
        minSdk = 21
        targetSdk = 30
        versionCode = 44
        versionName = "Alpha 2.9.0"
        resourceConfigurations += listOf("en", "zh", "zh-rCN")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {

            extra["enableCrashlytics"] = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = compose_version
    }
    packagingOptions {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    namespace = "io.github.kineks.neteaseviewer"
    //buildToolsVersion = "33.0.0 rc2"
}

dependencies {

    // 默认库
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity-compose:1.5.0-rc01")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.0-rc02")

    implementation("androidx.compose.ui:ui:$compose_version")
    implementation("androidx.compose.ui:ui-test:$compose_version")
    implementation("androidx.compose.ui:ui-tooling:$compose_version")
    implementation("androidx.compose.ui:ui-tooling-preview:$compose_version")

    implementation("androidx.compose.material:material:$compose_version")

    implementation("androidx.compose.compiler:compiler:$compose_version")
    implementation("androidx.compose.runtime:runtime:$compose_version")
    implementation("androidx.compose.foundation:foundation:$compose_version")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.0-rc02")
    implementation("androidx.fragment:fragment-ktx:1.4.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // compose 的额外扩展库(图标库和livedata)
    implementation("androidx.compose.material:material-icons-extended:$compose_version")
    //implementation("androidx.compose.runtime:runtime-livedata:$compose_version")

    // todo: 等我折腾明白注入怎么用再加回来
    //implementation 'androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03'
    // When using Kotlin.
    //kapt 'androidx.hilt:hilt-compiler:1.0.0'

    // 用于 compose 的系统ui控制(代码设置沉浸状态栏和导航栏)
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanist_version")

    // 支持 compose 使用的图像加载库
    implementation("io.coil-kt:coil-compose:2.1.0")

    // 用于数据请求和处理
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.9.0")

    // 简化权限申请库
    implementation("com.guolindev.permissionx:permissionx:1.6.4")
    //implementation("com.google.accompanist:accompanist-permissions:$accompanist_version")

    // 播放控制
    implementation("com.github.EspoirX:StarrySky:v2.6.5")
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.0")
    //implementation ("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    configurations.all {
        implementation("com.google.android.exoplayer:exoplayer-core:2.18.0")
    }

    // Compose 的下拉刷新
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanist_version")

    // Compose 版的 ViewPager ,用来滑动页面
    implementation("com.google.accompanist:accompanist-pager:$accompanist_version")

    // 音频文件标签处理
    implementation("com.ealva:ealvatag:0.4.6")
    implementation("com.squareup.okio:okio:3.2.0")

    // 异常上报
    implementation ("com.tencent.bugly:crashreport:4.0.4")

    // 文件操作
    implementation ("com.github.javakam:file.core:3.5.0@aar")

    // 单元测试
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation("androidx.compose.ui:ui-tooling:$compose_version")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$compose_version")
}