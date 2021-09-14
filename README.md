# NativeLibraryLoaderChecker
A custom gradle task that takes a native library and determines if it can be loaded successfully

### Example
```
import me.duhblea.jrebuildertask.JreBuilderTask
task createJre(type: JreBuilderTask) {

    // The final folder path the JRE should live in
    distFolderPath = "Path-to-final-jre-location".toString()
}
```