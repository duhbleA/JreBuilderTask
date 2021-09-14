# NativeLibraryLoaderChecker
A custom gradle task that takes a native library and determines if it can be loaded successfully

### Example
```
import me.duhblea.jrebuildertask.JreBuilderTask
task createJre(type: JreBuilderTask) {

    // Throw an exception if a DLL is missing, or log an error instead
    distFolderPath = "Path-to-final-jre-location".toString()
}
```