#!/usr/bin/env node

const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

console.log('🚀 Setting up HybridMesh React Native App...\n');

// Check if Node.js is installed
try {
  const nodeVersion = execSync('node --version', { encoding: 'utf8' }).trim();
  console.log(`✅ Node.js version: ${nodeVersion}`);
} catch (error) {
  console.error('❌ Node.js is not installed. Please install Node.js 16+ first.');
  process.exit(1);
}

// Check if npm is available
try {
  const npmVersion = execSync('npm --version', { encoding: 'utf8' }).trim();
  console.log(`✅ npm version: ${npmVersion}`);
} catch (error) {
  console.error('❌ npm is not available. Please install npm.');
  process.exit(1);
}

// Install dependencies
console.log('\n📦 Installing dependencies...');
try {
  execSync('npm install', { stdio: 'inherit' });
  console.log('✅ Dependencies installed successfully');
} catch (error) {
  console.error('❌ Failed to install dependencies:', error.message);
  process.exit(1);
}

// Check if React Native CLI is installed
try {
  execSync('npx react-native --version', { stdio: 'pipe' });
  console.log('✅ React Native CLI is available');
} catch (error) {
  console.log('⚠️  React Native CLI not found, installing globally...');
  try {
    execSync('npm install -g @react-native-community/cli', { stdio: 'inherit' });
    console.log('✅ React Native CLI installed');
  } catch (installError) {
    console.error('❌ Failed to install React Native CLI:', installError.message);
  }
}

// Create necessary directories
console.log('\n📁 Creating necessary directories...');
const directories = [
  'android/app/src/main/java/com/hybridmesh/chat/rn',
  'android/app/src/main/res/values',
  'ios',
];

directories.forEach(dir => {
  const fullPath = path.join(process.cwd(), dir);
  if (!fs.existsSync(fullPath)) {
    fs.mkdirSync(fullPath, { recursive: true });
    console.log(`✅ Created directory: ${dir}`);
  }
});

// Create gradle.properties if it doesn't exist
const gradlePropertiesPath = path.join(process.cwd(), 'android/gradle.properties');
if (!fs.existsSync(gradlePropertiesPath)) {
  const gradleProperties = `# Project-wide Gradle settings.

# IDE (e.g. Android Studio) users:
# Gradle settings configured through the IDE *will override*
# any settings specified in this file.

# For more details on how to configure your build environment visit
# http://www.gradle.org/docs/current/userguide/build_environment.html

# Specifies the JVM arguments used for the daemon process.
# The setting is particularly useful for tweaking memory settings.
# Default value: -Xmx1024m -XX:MaxPermSize=256m
org.gradle.jvmargs=-Xmx2048m -XX:MaxPermSize=512m -XX:+HeapDumpOnOutOfMemoryError -Dfile.encoding=UTF-8

# When configured, Gradle will run in incubating parallel mode.
# This option should only be used with decoupled projects. More details, visit
# http://www.gradle.org/docs/current/userguide/multi_project_builds.html#sec:decoupled_projects
# org.gradle.parallel=true

# AndroidX package structure to make it clearer which packages are bundled with the
# Android operating system, and which are packaged with your app's APK
# https://developer.android.com/topic/libraries/support-library/androidx-rn
android.useAndroidX=true

# Automatically convert third-party libraries to use AndroidX
android.enableJetifier=true

# Version of flipper SDK to use with React Native
FLIPPER_VERSION=0.125.0

# Use this property to specify which architecture you want to build.
# You can also override it from the CLI using
# ./gradlew <task> -PreactNativeArchitectures=x86_64
reactNativeArchitectures=armeabi-v7a,arm64-v8a,x86,x86_64

# Use this property to enable support to the new architecture.
# This will allow you to use TurboModules and the Fabric render in
# your application. You should enable this flag either if you want
# to write custom TurboModules/Fabric components OR use libraries that
# are providing them.
newArchEnabled=false

# Use this property to enable or disable the Hermes JS engine.
# If set to false, you will be using JSC instead.
hermesEnabled=true
`;
  
  fs.writeFileSync(gradlePropertiesPath, gradleProperties);
  console.log('✅ Created gradle.properties');
}

console.log('\n🎉 Setup completed successfully!');
console.log('\n📋 Next steps:');
console.log('1. For Android development:');
console.log('   - Install Android Studio');
console.log('   - Set up Android SDK and emulator');
console.log('   - Run: npx react-native run-android');
console.log('\n2. For iOS development:');
console.log('   - Install Xcode');
console.log('   - Run: cd ios && pod install && cd ..');
console.log('   - Run: npx react-native run-ios');
console.log('\n3. Start the Metro bundler:');
console.log('   - Run: npx react-native start');
console.log('\n🔧 Note: Bluetooth and Wi-Fi Direct features require a physical device to test.');

