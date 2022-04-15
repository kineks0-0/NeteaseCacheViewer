plugins {
    id ("com.android.application")
    id ("org.jetbrains.kotlin.android")
    id ("org.jetbrains.kotlin.kapt") version "1.6.10"
}

@Suppress("PropertyName")
val compose_version: String by project

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "io.github.kineks.neteaseviewer"
        minSdk = 21
        targetSdk = 31
        versionCode = 36
        versionName = "Alpha 2.8.6"
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
    buildToolsVersion = "32.0.0"
}

dependencies {

    // 默认库
    implementation ("androidx.core:core-ktx:1.7.0")
    implementation ("androidx.compose.ui:ui:1.2.0-alpha07")
    implementation ("androidx.compose.compiler:compiler:$compose_version")
    implementation ("androidx.compose.runtime:runtime:1.2.0-alpha07")
    implementation ("androidx.compose.material:material:$compose_version")
    implementation ("androidx.compose.ui:ui-tooling-preview:$compose_version")
    implementation ("androidx.lifecycle:lifecycle-runtime-ktx:2.4.1")
    implementation ("androidx.lifecycle:lifecycle-viewmodel-compose:2.4.1")
    implementation ("androidx.activity:activity-compose:1.4.0")
    implementation ("androidx.activity:activity:1.4.0")
    implementation ("androidx.fragment:fragment:1.4.1")
    implementation ("androidx.documentfile:documentfile:1.0.1")
    implementation ("androidx.datastore:datastore-preferences:1.0.0")

    // compose 的额外扩展库(图标库和livedata)
    implementation ("androidx.compose.material:material-icons-extended:$compose_version")
    implementation ("androidx.compose.runtime:runtime-livedata:$compose_version")

    // todo: 等我折腾明白注入怎么用再加回来
    //implementation 'androidx.hilt:hilt-lifecycle-viewmodel:1.0.0-alpha03'
    // When using Kotlin.
    //kapt 'androidx.hilt:hilt-compiler:1.0.0'

    // 用于 compose 的系统ui控制(代码设置沉浸状态栏和导航栏)
    implementation ("com.google.accompanist:accompanist-systemuicontroller:0.24.6-alpha")

    // 支持 compose 使用的图像加载库
    implementation ("io.coil-kt:coil-compose:1.4.0")

    // 用于数据请求和处理
    implementation ("com.squareup.retrofit2:retrofit:2.9.0")
    implementation ("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation ("com.google.code.gson:gson:2.9.0")

    // 简化权限申请库
    implementation ("com.guolindev.permissionx:permissionx:1.6.1")

    // 播放控制
    implementation ("com.github.EspoirX:StarrySky:v2.6.5")
    implementation ("com.google.android.exoplayer:exoplayer-core:2.17.1")
    implementation ("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    configurations.all {
        implementation ("com.google.android.exoplayer:exoplayer-core:2.16.1")
    }

    // Compose 的下拉刷新
    implementation ("com.google.accompanist:accompanist-swiperefresh:0.18.0")

    // Compose 版的 ViewPager ,用来滑动页面
    implementation ("com.google.accompanist:accompanist-pager:0.24.6-alpha")

    // 音频文件标签处理
    implementation ("com.ealva:ealvatag:0.4.6")
    implementation ("com.squareup.okio:okio:3.0.0")

    // 异常上报
    implementation ("com.tencent.bugly:crashreport:4.0.4")

    // 文件操作
    implementation ("com.github.javakam:file.core:3.5.0@aar")
    implementation("androidx.test.ext:junit-ktx:1.1.3")


    // 单元测试
    testImplementation ("junit:junit:4.13.2")
    androidTestImplementation ("androidx.test.ext:junit:1.1.3")
    androidTestImplementation ("androidx.test.espresso:espresso-core:3.4.0")
    androidTestImplementation ("androidx.compose.ui:ui-test-junit4:$compose_version")
    debugImplementation ("androidx.compose.ui:ui-tooling:$compose_version")
}