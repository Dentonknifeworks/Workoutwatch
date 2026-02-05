// plugins/withWearConnectivity.js
// Expo config plugin to set up Wear OS connectivity

const { withAndroidManifest, withAppBuildGradle } = require('expo/config-plugins');

function withWearConnectivity(config) {
  // Add the wearable dependency to build.gradle
  config = withAppBuildGradle(config, (config) => {
    if (!config.modResults.contents.includes('play-services-wearable')) {
      config.modResults.contents = config.modResults.contents.replace(
        /dependencies\s*{/,
        `dependencies {
    implementation 'com.google.android.gms:play-services-wearable:18.1.0'`
      );
    }
    return config;
  });

  // Add permissions to AndroidManifest.xml
  config = withAndroidManifest(config, (config) => {
    const mainApplication = config.modResults.manifest.application[0];
    
    // Ensure meta-data exists for wearable
    if (!mainApplication['meta-data']) {
      mainApplication['meta-data'] = [];
    }
    
    // Add capability to communicate with wearable
    const hasWearableCapability = mainApplication['meta-data'].some(
      (item) => item.$['android:name'] === 'com.google.android.wearable.standalone'
    );
    
    if (!hasWearableCapability) {
      mainApplication['meta-data'].push({
        $: {
          'android:name': 'com.google.android.wearable.standalone',
          'android:value': 'false',
        },
      });
    }
    
    return config;
  });

  return config;
}

module.exports = withWearConnectivity;
