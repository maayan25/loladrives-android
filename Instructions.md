## Initial environment setup
The project wa s developed using `Android 11`, Java `JDK 11`, and `Gradle 6.5`

###### For new RTLOLA engine (SDK build tool == 32.0):
Change the build files: 
```bash
cd ~/Android/Sdk/build-tools/32.0.0/
mv d8 dx
mv lib/d8.jar lib/dx.jar
```
Run the build



### Running the tests


### Running the OBD simulator
1. Connect simulator to power and to computer
2. Run OBD simulator in AndroidStudio with JDK 17
3. Run app in AndroidStudio


### Setting up PCDF-CORE
1. Extract the zip sent from Sebastian into OBD-2-Simulator/kotlin/src/main/kotlin/
2. Check and change the names of the imports where it gives errors (e.g. pcdfEvent to PCDFEvent)
