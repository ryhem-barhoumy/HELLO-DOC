plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "tn.rnu.isetr.hellodoc"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "tn.rnu.isetr.hellodoc"
        minSdk = 27
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"
        val apiUrl = providers.gradleProperty("HELLODOC_API_URL").orElse("http://10.0.2.2:8081/api/").get()
        require(apiUrl.endsWith("/api/") && !apiUrl.contains('"') && !apiUrl.contains('\n')) {
            "HELLODOC_API_URL doit se terminer par /api/."
        }
        buildConfigField("String", "API_BASE_URL", "\"$apiUrl\"")
        manifestPlaceholders["allowCleartext"] = "false"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    buildTypes {
        debug { manifestPlaceholders["allowCleartext"] = "true" }
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    androidTestImplementation("androidx.arch.core:core-testing:2.2.0")
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    // Navigation
    implementation(libs.androidx.navigation.fragment.ktx)

    // ViewModel et Lifecycle
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:3.14.9")
}


tasks.withType<Test>().configureEach {
    inputs.property("helloDocTestApi", providers.environmentVariable("HELLODOC_TEST_API_URL").orElse(""))
}
