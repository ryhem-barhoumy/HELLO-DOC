plugins {
    // Plugin Android déjà déclaré dans le projet racine.
    alias(libs.plugins.android.application)
}

android {
    // Le package demandé reste identique dans tous les fichiers Kotlin et XML.
    namespace = "com.example.hellodoc"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.hellodoc"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.1"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        debug { isDebuggable = true }
        release { isMinifyEnabled = false }
    }
}

dependencies {
    // TP2/TP3 : XML, Activities, Material Design, ViewBinding.
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.activity:activity-ktx:1.11.0")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // TP4 : RecyclerView.
    implementation("androidx.recyclerview:recyclerview:1.4.0")

    // TP5 : DrawerLayout, NavigationView, Fragments et Navigation Components.
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")
    implementation("androidx.navigation:navigation-fragment-ktx:2.9.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.6")

    // TP6 : MVVM avec ViewModel, LiveData et Coroutines.
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")

    // TP7 : client HTTP et conversion JSON.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
