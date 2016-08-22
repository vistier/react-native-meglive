

# Linking Android with Gradle
 * Add following lines into android/settings.gradle
 ```
  include ':react-native-meglive'
  project(':react-native-meglive').projectDir = new File(rootProject.projectDir, '../node_modules/react-native-meglive/android')
 ```

 * Add following lines into your android/app/build.gradle in section dependencies
 ```
 dependencies {
   compile project(':react-native-meglive')  // Add this line only.
 }
 ```

 * Add following meglive lib *.so android/lib

 * Add following meglive lib model android/src/main/res/raw

 * Add following lines into MainApplication.java
 ```
 import com.pansky.megvill.MegliveReactPackage;
  ...
 @Override
    protected List<ReactPackage> getPackages() {
      return Arrays.<ReactPackage>asList(
          new MainReactPackage(),
          new MegliveReactPackage(R.raw.model;) // Add this line
      );
    }
 ```

