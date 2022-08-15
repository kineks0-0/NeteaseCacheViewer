
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.kapt")
}

val composeVersion: String by project

val accompanistVersion: String by project

val pagingVersion: String by project

val appCenterSdkVersion: String by project



android {

    compileSdk = 32
    //compileSdkPreview = "android-Tiramisu"

    defaultConfig {
        applicationId = "io.github.kineks.neteaseviewer"
        minSdk = 21
        targetSdk = 32
        versionCode = 105
        versionName = "Alpha 3.2.1"
        resourceConfigurations += listOf("en", "zh", "zh-rCN")

        ndk {
            abiFilters += listOf("arm64-v8a", "x86_64")
        }

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // extra["enableCrashlytics"] = false
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

        }
    }

    // 输出类型
    android.applicationVariants.all {
        // 编译类型
        val buildType = this.buildType.name
        val appName = "NeteaseViewer"
        outputs.all {
            // 判断是否是输出 apk 类型
            if (this is com.android.build.gradle.internal.api.ApkVariantOutputImpl) {
                this.outputFileName =
                    "${appName} - ${defaultConfig.versionName} - $buildType.apk"
            }
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
        kotlinCompilerExtensionVersion = "1.3.0-beta01"
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
    // 谨慎升级 Androidx 的依赖，否则可能导致 Compose Preview 可能无法正常渲染
    implementation("androidx.core:core-ktx:1.8.0")
    implementation("androidx.activity:activity-compose:1.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")

    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-test:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")

    implementation("androidx.compose.material:material:$composeVersion")

    //implementation("androidx.compose.compiler:compiler:1.3.0-rc02")
    implementation("androidx.compose.runtime:runtime:$composeVersion")
    implementation("androidx.compose.foundation:foundation:$composeVersion")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.5.1")
    implementation("androidx.fragment:fragment-ktx:1.5.1")
    implementation("androidx.documentfile:documentfile:1.1.0-alpha01")
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // compose 的额外扩展库(图标库和livedata)
    implementation("androidx.compose.material:material-icons-extended:$composeVersion")
    //implementation("androidx.compose.runtime:runtime-livedata:$compose_version")

    // 适用 compose 的系统ui控制(代码设置沉浸状态栏和导航栏)
    implementation("com.google.accompanist:accompanist-systemuicontroller:$accompanistVersion")

    // 支持 compose 使用的图像加载库
    implementation("io.coil-kt:coil-compose:2.1.0")

    // 用于数据请求和处理
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.google.code.gson:gson:2.9.1")

    // 简化权限申请库
    implementation("com.guolindev.permissionx:permissionx:1.6.4")
    //implementation("com.google.accompanist:accompanist-permissions:$accompanist_version")

    // 播放控制
    implementation("com.github.EspoirX:StarrySky:v2.6.5")
    implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
    implementation("com.google.android.exoplayer:exoplayer-ui:2.18.1")
    //implementation ("com.google.android.exoplayer:exoplayer-ui:2.17.1")
    configurations.all {
        implementation("com.google.android.exoplayer:exoplayer-core:2.18.1")
    }

    // Compose 的下拉刷新
    implementation("com.google.accompanist:accompanist-swiperefresh:$accompanistVersion")

    // Compose 版的 ViewPager ,用来滑动页面
    implementation("com.google.accompanist:accompanist-pager:$accompanistVersion")

    // 音频文件标签处理
    implementation("com.ealva:ealvatag:0.4.6")
    implementation("com.squareup.okio:okio:3.2.0")

    // 异常上报
    implementation("com.microsoft.appcenter:appcenter-analytics:$appCenterSdkVersion")
    implementation("com.microsoft.appcenter:appcenter-crashes:${appCenterSdkVersion}")

    // 文件操作
    implementation("com.github.javakam:file.core:3.5.0@aar")


    // 单元测试
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.3")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.3.0")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVersion")
}